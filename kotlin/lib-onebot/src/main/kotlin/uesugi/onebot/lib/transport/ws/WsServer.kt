package uesugi.onebot.lib.transport.ws

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.ActionRequest
import uesugi.onebot.lib.model.ActionResponse
import uesugi.onebot.lib.model.EventEnvelope
import uesugi.onebot.lib.transport.ActionChannel
import uesugi.onebot.lib.transport.EventChannel
import uesugi.onebot.lib.transport.TransportConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket 服务端：OneBot 作为 WS 服务端。
 * 同时提供 API 和事件通道。
 */
class WsServer(
    private val config: TransportConfig,
) : ActionChannel, EventChannel {

    private val logger = LoggerFactory.getLogger(WsServer::class.java)
    private var server: Any? = null
    private val json = Json { ignoreUnknownKeys = true }

    // 连接的客户端（sessionId → session）
    private val apiClients = ConcurrentHashMap<String, WebSocketSession>()
    private val eventClients = ConcurrentHashMap<String, WebSocketSession>()

    // 事件流
    private val _eventFlow = MutableSharedFlow<EventEnvelope>(extraBufferCapacity = 64)
    override val eventFlow: Flow<EventEnvelope> = _eventFlow.asSharedFlow()

    // 动作处理器
    private var actionHandler: (suspend (ActionRequest) -> ActionResponse)? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onRequest(handler: suspend (ActionRequest) -> ActionResponse) {
        actionHandler = handler
    }

    override suspend fun start() {
        server = embeddedServer(Netty, host = config.host, port = config.port) {
            install(WebSockets)

            routing {
                webSocket(WsPaths.ALL) {
                    handleWsSession(this, WsRole.UNIVERSAL)
                }
                webSocket(WsPaths.API) {
                    handleWsSession(this, WsRole.API)
                }
                webSocket(WsPaths.EVENT) {
                    handleWsSession(this, WsRole.EVENT)
                }
            }
        }.start(wait = false)

        logger.info("WS server started at ws://{}:{}", config.host, config.port)
    }

    override suspend fun stop() {
        scope.cancel()
        apiClients.values.forEach { it.close() }
        eventClients.values.forEach { it.close() }
        (server as? AutoCloseable)?.close()
        @Suppress("DEPRECATION")
        (server as? io.ktor.server.engine.ApplicationEngine)?.stop(1000, 2000)
        logger.info("WS server stopped")
    }

    override suspend fun send(request: ActionRequest): ActionResponse {
        throw UnsupportedOperationException("WsServer uses broadcast; use onRequest() pattern instead")
    }

    override suspend fun push(event: EventEnvelope) {
        val message = buildJsonObject {
            put("time", event.time)
            put("self_id", event.selfId)
            put("post_type", event.postType)
        }.toString()

        // 推送到所有 event/universal 客户端
        eventClients.values.forEach { session ->
            scope.launch {
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    logger.debug("Failed to send event to client: ${e.message}")
                }
            }
        }
    }

    private suspend fun handleWsSession(session: WebSocketSession, role: WsRole) {
        val sessionId = session.hashCode().toString()
        when (role) {
            WsRole.API, WsRole.UNIVERSAL -> apiClients[sessionId] = session
            WsRole.EVENT, WsRole.UNIVERSAL -> eventClients[sessionId] = session
        }

        logger.debug("WS client connected: session={}, role={}", sessionId, role)

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    handleMessage(session, text, role)
                }
            }
        } catch (e: Exception) {
            logger.debug("WS client disconnected: session={}, {}", sessionId, e.message)
        } finally {
            apiClients.remove(sessionId)
            eventClients.remove(sessionId)
        }
    }

    private suspend fun handleMessage(session: WebSocketSession, text: String, role: WsRole) {
        // 只有 API 或 Universal 角色处理 API 请求
        if (role == WsRole.EVENT) return

        val handler = actionHandler ?: return

        try {
            val element = json.parseToJsonElement(text)
            val obj = element.jsonObject

            val action = obj["action"]?.jsonPrimitive?.contentOrNull ?: return
            val params = obj["params"]?.jsonObject ?: JsonObject(emptyMap())
            val echo = obj["echo"]?.jsonPrimitive?.contentOrNull

            val request = ActionRequest(action, params, echo)
            val response = handler(request)
            session.send(Frame.Text(response.toJsonObject().toString()))
        } catch (e: Exception) {
            logger.error("Error handling WS message: {}", e.message)
        }
    }
}
