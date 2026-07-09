package uesugi.onebot.lib.transport.impl.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.ActionNotFoundException
import uesugi.onebot.core.dispatch.DispatchException
import uesugi.onebot.core.model.ActionResponse
import uesugi.onebot.core.model.AsyncActionResult
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.transport.ActionServerChannel
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.core.transport.STOP_GRACE_PERIOD
import uesugi.onebot.core.transport.STOP_TIMEOUT

/**
 * HTTP Action 服务端。
 *
 * 用于 OneBot 实现侧：启动 HTTP 服务器，接收 API 调用。
 * 支持三种参数传递方式（按优先级）：
 * 1. JSON Body（Content-Type: application/json）
 * 2. URL-encoded Form（Content-Type: application/x-www-form-urlencoded）
 * 3. Query Parameters（GET 请求）
 */
class HttpActionServer(
    private val config: OneBotConfig,
    override val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult
) : ActionServerChannel {
    private val logger = LoggerFactory.getLogger(HttpActionServer::class.java)

    private val json = JsonFactory.lenient
    private var server: EmbeddedServer<*, *>? = null
    private val paramsParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()

    override suspend fun start() {
        server = embeddedServer(Netty, host = config.httpHost, port = config.httpPort) {
            routing {
                route("/{action}") {
                    handle {
                        val action = call.parameters["action"] ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@handle
                        }

                        if (!config.accessToken.isNullOrBlank()) {
                            val receivedAuth = call.request.header("Authorization")
                            val queryToken = call.request.queryParameters["access_token"]
                            val hasToken = receivedAuth != null || queryToken != null
                            val tokenMatch = receivedAuth == config.authHeader || queryToken == config.accessToken

                            if (!hasToken) {
                                val body =
                                    json.encodeToString(ActionResponse.serializer(), ActionResponse.unauthorized())
                                call.respondText(body, ContentType.Application.Json, HttpStatusCode.Unauthorized)
                                return@handle
                            }
                            if (!tokenMatch) {
                                val body = json.encodeToString(ActionResponse.serializer(), ActionResponse.forbidden())
                                call.respondText(body, ContentType.Application.Json, HttpStatusCode.Forbidden)
                                return@handle
                            }
                        }

                        val params = try {
                            parseParams(call)
                        } catch (e: Exception) {
                            logger.debug("Failed to parse request body: {}", e.message)
                            val body = json.encodeToString(ActionResponse.serializer(), ActionResponse.badRequest())
                            call.respondText(body, ContentType.Application.Json, HttpStatusCode.BadRequest)
                            return@handle
                        }
                        if (params == null) {
                            call.respond(HttpStatusCode.NotAcceptable)
                            return@handle
                        }

                        val (statusCode, responseJson) = try {
                            when (val result = actionHandler(action, paramsParser.deserialize(action, params))) {
                                is AsyncActionResult -> {
                                    val resp = ActionResponse.async()
                                    HttpStatusCode.OK to json.encodeToString(ActionResponse.serializer(), resp)
                                }

                                else -> {
                                    val data = resultParser.serialize(action, result)
                                    val resp = ActionResponse.ok(data)
                                    HttpStatusCode.OK to json.encodeToString(ActionResponse.serializer(), resp)
                                }
                            }
                        } catch (e: DispatchException) {
                            when (e) {
                                is ActionNotFoundException -> {
                                    logger.debug("Action not found: {}", action)
                                    val resp = ActionResponse.notFound()
                                    HttpStatusCode.NotFound to json.encodeToString(ActionResponse.serializer(), resp)
                                }

                                else -> {
                                    logger.warn("Dispatch error for action {}: retcode={}", action, e.retcode)
                                    val resp = ActionResponse.failed(e.retcode)
                                    HttpStatusCode.BadRequest to json.encodeToString(ActionResponse.serializer(), resp)
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn("Failed to handle action $action", e)
                            val resp = ActionResponse.badRequest()
                            HttpStatusCode.BadRequest to json.encodeToString(ActionResponse.serializer(), resp)
                        }
                        call.respondText(responseJson, ContentType.Application.Json, statusCode)
                    }
                }
            }
        }
        val started = CompletableDeferred<Unit>()
        server!!.monitor.subscribe(ApplicationStarted) { started.complete(Unit) }
        server!!.start(wait = false)
        started.await()
        logger.info("HTTP Action server started on {}:{}", config.httpHost, config.httpPort)
    }

    override suspend fun stop() {
        server?.stop(STOP_GRACE_PERIOD, STOP_TIMEOUT)
        logger.info("HTTP Action server stopped")
    }

    private suspend fun parseParams(call: ApplicationCall): JsonObject? {
        val contentType = call.request.contentType()

        return when {
            contentType.match(ContentType.Application.Json) -> {
                val body = call.receiveText()
                val parsed = json.parseToJsonElement(body).jsonObject
                // support both raw params and ActionRequest wrapper
                parsed["params"]?.jsonObject ?: parsed
            }

            contentType.match(ContentType.Application.FormUrlEncoded) -> {
                val params = call.receiveParameters()
                buildJsonObject {
                    params.entries().forEach { (key, values) ->
                        put(key, values.firstOrNull() ?: "")
                    }
                }
            }

            call.request.httpMethod == HttpMethod.Get -> {
                val queryParams = call.request.queryParameters
                buildJsonObject {
                    queryParams.entries().forEach { (key, values) ->
                        val value = values.firstOrNull() ?: ""
                        val parsed = value.toLongOrNull()
                            ?: value.toDoubleOrNull()
                            ?: value.toBooleanStrictOrNull()
                        when (parsed) {
                            is Long -> put(key, parsed)
                            is Double -> put(key, parsed)
                            is Boolean -> put(key, parsed)
                            else -> put(key, value)
                        }
                    }
                }
            }

            else -> null
        }
    }
}
