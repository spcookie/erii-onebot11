package uesugi.onebot.lib.transport.http

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.ActionRequest
import uesugi.onebot.lib.model.ActionResponse
import uesugi.onebot.lib.transport.ActionChannel
import uesugi.onebot.lib.transport.TransportConfig

/**
 * HTTP 动作服务端：OneBot 作为 HTTP 服务器。
 * 监听 /:action 路由，接收 API 调用并返回响应。
 */
class HttpActionServer(
    private val config: TransportConfig,
) : ActionChannel {

    private val logger = LoggerFactory.getLogger(HttpActionServer::class.java)
    private var server: Any? = null
    private var actionHandler: (suspend (ActionRequest) -> ActionResponse)? = null
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    override fun onRequest(handler: suspend (ActionRequest) -> ActionResponse) {
        actionHandler = handler
    }

    override suspend fun start() {
        val handler = actionHandler ?: throw IllegalStateException("onRequest() must be called before start()")

        server = embeddedServer(Netty, host = config.host, port = config.port) {
            install(ContentNegotiation) {
                json(this@HttpActionServer.json)
            }
            routing {
                route("/{action}") {
                    handle(handler)
                }
            }
        }.start(wait = false)

        logger.info("HTTP action server started at http://{}:{}", config.host, config.port)
    }

    override suspend fun stop() {
        (server as? AutoCloseable)?.close()
        @Suppress("DEPRECATION")
        (server as? io.ktor.server.engine.ApplicationEngine)?.stop(1000, 2000)
        logger.info("HTTP action server stopped")
    }

    override suspend fun send(request: ActionRequest): ActionResponse {
        throw UnsupportedOperationException("HttpActionServer is server-side, use onRequest() instead")
    }

    /**
     * 处理 /:action 路由
     */
    private fun Route.handle(actionHandler: suspend (ActionRequest) -> ActionResponse) {
        get {
            processRequest(call, actionHandler)
        }
        post {
            processRequest(call, actionHandler)
        }
    }

    private suspend fun processRequest(call: ApplicationCall, actionHandler: suspend (ActionRequest) -> ActionResponse) {
        val actionName = call.parameters["action"] ?: run {
            call.respond(HttpStatusCode.NotFound, """{"status":"failed","retcode":1404}""")
            return
        }

        // 认证检查
        if (config.accessToken != null) {
            val authHeader = call.request.headers["Authorization"]
            val queryToken = call.request.queryParameters["access_token"]
            val token = authHeader?.removePrefix("Bearer ") ?: queryToken

            when {
                token == null -> {
                    call.respond(HttpStatusCode.Unauthorized, """{"status":"failed","retcode":1401}""")
                    return
                }
                token != config.accessToken -> {
                    call.respond(HttpStatusCode.Forbidden, """{"status":"failed","retcode":1403}""")
                    return
                }
            }
        }

        // Content-Type 检查（仅 POST）
        if (call.request.httpMethod == HttpMethod.Post) {
            val contentType = call.request.contentType()
            if (contentType != ContentType.Application.Json &&
                contentType != ContentType.Application.FormUrlEncoded &&
                contentType.toString() != "application/x-www-form-urlencoded") {
                // 如果请求有 body 但不是 JSON，返回 406
                val body = try { call.receiveText() } catch (e: Exception) { "" }
                if (body.isNotBlank() && !contentType.toString().startsWith("application/json")) {
                    if (!contentType.toString().startsWith("application/x-www-form-urlencoded")) {
                        call.respond(HttpStatusCode.NotAcceptable, """{"status":"failed","retcode":1406}""")
                        return
                    }
                }
            }
        }

        // 解析参数
        val params = try {
            parseParams(call)
        } catch (e: Exception) {
            logger.debug("Failed to parse params: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, """{"status":"failed","retcode":1400}""")
            return
        }

        val echo = call.request.queryParameters["echo"] ?: params["echo"]?.jsonPrimitive?.contentOrNull

        val request = ActionRequest(
            action = actionName,
            params = params,
            echo = echo,
        )

        try {
            val response = actionHandler(request)
            call.respond(HttpStatusCode.OK, response.toJsonObject().toString())
        } catch (e: Exception) {
            logger.error("Error processing action: {}", actionName, e)
            call.respond(HttpStatusCode.OK, """{"status":"failed","retcode":1}""")
        }
    }

    private suspend fun parseParams(call: ApplicationCall): JsonObject {
        // 优先从 JSON body 解析
        if (call.request.httpMethod == HttpMethod.Post) {
            val contentType = call.request.contentType()
            if (contentType == ContentType.Application.Json ||
                contentType.toString().startsWith("application/json")) {
                try {
                    val body = call.receiveText()
                    if (body.isNotBlank()) {
                        val element = json.parseToJsonElement(body)
                        if (element is JsonObject) return element
                    }
                } catch (e: Exception) {
                    logger.debug("Failed to parse JSON body: {}", e.message)
                }
            }
        }

        // 从 query 参数构建
        val collectedKeys = mutableSetOf<String>()
        val params = buildJsonObject {
            for ((key, values) in call.request.queryParameters.entries()) {
                if (key != "access_token" && key != "echo") {
                    put(key, values.firstOrNull() ?: "")
                    collectedKeys.add(key)
                }
            }
            // POST form 参数（不覆盖已有的 query 参数）
            if (call.request.httpMethod == HttpMethod.Post) {
                try {
                    val formParams = call.receiveParameters()
                    for ((key, values) in formParams.entries()) {
                        if (key !in collectedKeys) {
                            put(key, values.firstOrNull() ?: "")
                            collectedKeys.add(key)
                        }
                    }
                } catch (e: Exception) { logger.debug("Failed to parse form params: {}", e.message) }
            }
        }

        return params
    }
}
