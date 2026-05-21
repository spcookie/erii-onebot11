package uesugi.onebot.lib.transport.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.EventEnvelope
import uesugi.onebot.lib.transport.EventChannel
import uesugi.onebot.lib.transport.TransportConfig
import uesugi.onebot.lib.transport.hmacSha1

/**
 * HTTP 事件客户端：向应用端推送事件（OneBot 实现端使用）。
 */
class HttpEventClient(
    private val config: TransportConfig,
    private val targetUrl: String,
) : EventChannel {

    private val logger = LoggerFactory.getLogger(HttpEventClient::class.java)
    override val eventFlow: Flow<EventEnvelope> = emptyFlow()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@HttpEventClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
        }
    }

    override suspend fun push(event: EventEnvelope) {
        try {
            val body = buildJsonObject {
                put("time", event.time)
                put("self_id", event.selfId)
                put("post_type", event.postType)
            }.toString()

            val response: HttpResponse = client.post(targetUrl) {
                contentType(ContentType.Application.Json)
                header("X-Self-ID", config.selfId.toString())

                // 签名
                if (config.secret != null) {
                    val signature = "sha1=" + hmacSha1(body, config.secret!!)
                    header("X-Signature", signature)
                }

                setBody(body)
            }

            logger.debug("Event pushed to {}, status={}", targetUrl, response.status.value)
        } catch (e: Exception) {
            logger.error("Failed to push event to {}: {}", targetUrl, e.message)
        }
    }

    override suspend fun start() {
        logger.info("HTTP event client ready, target={}", targetUrl)
    }

    override suspend fun stop() {
        client.close()
        logger.info("HTTP event client stopped")
    }

}
