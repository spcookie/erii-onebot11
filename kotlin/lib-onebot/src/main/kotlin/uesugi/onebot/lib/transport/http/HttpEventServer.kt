package uesugi.onebot.lib.transport.http

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.EventEnvelope
import uesugi.onebot.lib.transport.EventChannel
import uesugi.onebot.lib.transport.TransportConfig
import uesugi.onebot.lib.transport.hmacSha1

/**
 * HTTP 事件服务端：接收 OneBot 实现端推送的事件（HTTP POST 模式）。
 */
class HttpEventServer(
    private val config: TransportConfig,
) : EventChannel {

    private val logger = LoggerFactory.getLogger(HttpEventServer::class.java)
    private var server: Any? = null
    private val _eventFlow = MutableSharedFlow<EventEnvelope>(extraBufferCapacity = 64)
    override val eventFlow: Flow<EventEnvelope> = _eventFlow.asSharedFlow()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun push(event: EventEnvelope) {
        throw UnsupportedOperationException("HttpEventServer is server-side, use onEvent() instead")
    }

    override suspend fun start() {
        server = embeddedServer(Netty, host = config.host, port = config.port) {
            routing {
                post("/") {
                    processEvent(call)
                }
                // 也支持自定义路径
                post("/event") {
                    processEvent(call)
                }
            }
        }.start(wait = false)

        logger.info("HTTP event server started at http://{}:{}", config.host, config.port)
    }

    override suspend fun stop() {
        (server as? AutoCloseable)?.close()
        @Suppress("DEPRECATION")
        (server as? io.ktor.server.engine.ApplicationEngine)?.stop(1000, 2000)
        logger.info("HTTP event server stopped")
    }

    private suspend fun processEvent(call: ApplicationCall) {
        // 读取 X-Self-ID
        val selfId = call.request.headers["X-Self-ID"]?.toLongOrNull() ?: 0L

        // 读取 body（只能读一次）
        val body = call.receiveText()

        // 验证签名
        if (config.secret != null) {
            val signature = call.request.headers["X-Signature"]
            if (!verifySignature(body, signature, config.secret!!)) {
                logger.warn("Invalid signature for event from self_id={}", selfId)
                call.respond(HttpStatusCode.Forbidden)
                return
            }
        }

        // 解析事件
        try {
            val element = json.parseToJsonElement(body)
            val envelope = EventEnvelope(
                time = element.jsonObject["time"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis() / 1000,
                selfId = element.jsonObject["self_id"]?.jsonPrimitive?.longOrNull ?: selfId,
                postType = element.jsonObject["post_type"]?.jsonPrimitive?.contentOrNull ?: "unknown",
            )
            _eventFlow.emit(envelope)
        } catch (e: Exception) {
            logger.error("Failed to parse event: ${e.message}")
        }

        // 快速操作响应（NoContent = 无操作）
        call.respond(HttpStatusCode.NoContent)
    }

    private fun verifySignature(body: String, signature: String?, secret: String): Boolean {
        if (signature == null) return false
        val expected = "sha1=" + hmacSha1(body, secret)
        return signature.equals(expected, ignoreCase = true)
    }
}