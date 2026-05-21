package uesugi.onebot.lib.transport.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.ActionRequest
import uesugi.onebot.lib.model.ActionResponse
import uesugi.onebot.lib.transport.ActionChannel
import uesugi.onebot.lib.transport.TransportConfig

/**
 * HTTP 动作客户端：向 OneBot 实现端发送 API 调用。
 */
class HttpActionClient(
    private val config: TransportConfig,
) : ActionChannel {

    private val logger = LoggerFactory.getLogger(HttpActionClient::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@HttpActionClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
        }
    }

    override fun onRequest(handler: suspend (ActionRequest) -> ActionResponse) {
        throw UnsupportedOperationException("HttpActionClient is client-side, use send() instead")
    }

    override suspend fun start() {
        logger.info("HTTP action client ready: http://{}:{}", config.host, config.port)
    }

    override suspend fun stop() {
        client.close()
        logger.info("HTTP action client stopped")
    }

    override suspend fun send(request: ActionRequest): ActionResponse {
        val baseUrl = "http://${config.host}:${config.port}/${request.action}"

        return try {
            val response: HttpResponse = client.post(baseUrl) {
                // 附加 access_token
                config.accessToken?.let {
                    parameter("access_token", it)
                }
                // echo 作为 query 参数
                request.echo?.let {
                    parameter("echo", it)
                }
                contentType(ContentType.Application.Json)
                setBody(request.params.toString())
            }

            val body = response.bodyAsText()
            logger.debug("Action '{}' response: {}", request.action, body)

            val jsonElement = json.parseToJsonElement(body)
            val obj = jsonElement.jsonObject
            ActionResponse(
                status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "failed",
                retcode = obj["retcode"]?.jsonPrimitive?.intOrNull ?: 1,
                data = obj["data"],
                echo = obj["echo"]?.jsonPrimitive?.contentOrNull ?: request.echo,
            )
        } catch (e: Exception) {
            logger.error("Action '{}' failed: {}", request.action, e.message)
            throw e
        }
    }
}
