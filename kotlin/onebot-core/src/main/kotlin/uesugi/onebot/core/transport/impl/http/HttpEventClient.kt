package uesugi.onebot.core.transport.impl.http

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.OneBotEvent
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.EventPushChannel
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.core.util.Signing

/**
 * HTTP 事件推送客户端（服务端侧使用）。
 *
 * OneBot 实现通过此客户端向配置的 URL POST 事件 JSON。
 * 如果配置了 secret，则在 X-Signature 头中附加 HMAC-SHA1 签名。
 */
class HttpEventClient(
    private val config: OneBotConfig,
    private val client: HttpClient = HttpClient()
) : EventPushChannel {

    private val logger = LoggerFactory.getLogger(HttpEventClient::class.java)
    private val json: Json = JsonFactory.base
    private val postUrl: String = config.httpPostUrl
        ?: error("httpPostUrl is required for HTTP POST mode")
    private val eventParser = EventParser(config.messageFormat)

    override suspend fun pushEvent(event: OneBotEvent) {
        try {
            val eventJson = json.encodeToString(JsonObject.serializer(), eventParser.serialize(event))

            val response = client.post(postUrl) {
                contentType(ContentType.Application.Json)
                header("X-Self-ID", config.selfIdStr)
                config.secret?.let { secret ->
                    header("X-Signature", Signing.signBody(eventJson, secret))
                }
                setBody(eventJson)
            }

            if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
                logger.warn("Event push returned HTTP {}", response.status.value)
            }
        } catch (e: Exception) {
            logger.error("Failed to push event", e)
        }
    }

    override suspend fun start() { /* stateless */
    }

    override suspend fun stop() {
        client.close()
    }
}
