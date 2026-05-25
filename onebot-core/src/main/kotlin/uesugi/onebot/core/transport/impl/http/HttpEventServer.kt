package uesugi.onebot.core.transport.impl.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.OneBotEvent
import uesugi.onebot.core.model.QuickOpHandler
import uesugi.onebot.core.model.QuickOperation
import uesugi.onebot.core.model.quickOpHandler
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.EventChannel
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.core.transport.STOP_GRACE_PERIOD
import uesugi.onebot.core.transport.STOP_TIMEOUT
import uesugi.onebot.core.util.Signing
import kotlin.time.Duration.Companion.milliseconds

/**
 * HTTP 事件服务端（SDK/客户端侧使用）。
 *
 * SDK 启动嵌入式 HTTP 服务器，接收 OneBot 实现推送的 POST 事件。
 * 接收端点：`/` 和 `/event`
 * 事件处理器通过 [OneBotEvent.respondQuickOp] 返回快速操作，
 * 服务端等待 [timeoutMs] 后在 HTTP 响应中返回。
 */
class HttpEventServer(
    private val config: OneBotConfig
) : EventChannel {

    private val logger = LoggerFactory.getLogger(HttpEventServer::class.java)
    private val eventChannel = Channel<OneBotEvent>(Channel.UNLIMITED)
    private var server: EmbeddedServer<*, *>? = null
    private val parser: EventParser = EventParser(config.messageFormat)
    private val json = JsonFactory.base

    override val events: Flow<OneBotEvent> = eventChannel.receiveAsFlow()

    override suspend fun start() {
        server = embeddedServer(Netty, host = config.httpPostHost, port = config.httpPostPort) {
            routing {
                post("/") { handleEventPost(call) }
                post("/event") { handleEventPost(call) }
            }
        }
        server!!.start(wait = false)
        logger.info("HTTP Event server listening on {}:{}", config.httpPostHost, config.httpPostPort)
    }

    private suspend fun handleEventPost(call: ApplicationCall) {
        val body = call.receiveText()
        if (!config.secret.isNullOrBlank()) {
            val sig = call.request.header("X-Signature")
            if (!Signing.verifySign(body, sig, config.secret)) {
                call.respond(HttpStatusCode.Forbidden)
                return
            }
        }
        try {
            val event = parser.deserialize(body)
            val deferred = CompletableDeferred<QuickOperation?>()

            event.quickOpHandler = QuickOpHandler { op -> deferred.complete(op) }
            try {
                // 分发事件到异步处理器
                eventChannel.trySend(event)

                // 等待处理器返回快速操作（带超时）
                val quickOp = withTimeoutOrNull(config.httpPostTimeout.milliseconds) { deferred.await() }
                if (quickOp != null) {
                    val respBody = json.encodeToString(QuickOperation.serializer(), quickOp)
                    call.respondText(respBody, ContentType.Application.Json)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            } finally {
                event.quickOpHandler = null
            }
        } catch (e: Exception) {
            logger.error("Failed to parse event", e)
            call.respond(HttpStatusCode.BadRequest)
        }
    }

    override suspend fun stop() {
        server?.stop(STOP_GRACE_PERIOD, STOP_TIMEOUT)
        eventChannel.close()
        logger.info("HTTP Event server stopped")
    }
}
