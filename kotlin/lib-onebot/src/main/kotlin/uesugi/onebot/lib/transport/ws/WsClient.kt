package uesugi.onebot.lib.transport.ws

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket 客户端：作为 WS 客户端主动连接到 OneBot 实现端（反向 WS）。
 * 支持三种角色：API、Event、Universal。
 */
class WsClient(
    private val config: TransportConfig,
    private val wsUrl: String,
    private val role: WsRole = WsRole.UNIVERSAL,
) : ActionChannel, EventChannel {

    private val logger = LoggerFactory.getLogger(WsClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var session: WebSocketSession? = null

    private val httpClient = HttpClient(CIO) {
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
        }
    }

    // Echo → CompletableDeferred 映射（用于 API 请求/响应匹配）
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<ActionResponse>>()

    // 事件流
    private val _eventFlow = MutableSharedFlow<EventEnvelope>(extraBufferCapacity = 64)
    override val eventFlow: Flow<EventEnvelope> = _eventFlow.asSharedFlow()

    // 动作处理器（服务端模式使用）
    private var actionHandler: (suspend (ActionRequest) -> ActionResponse)? = null

    override fun onRequest(handler: suspend (ActionRequest) -> ActionResponse) {
        actionHandler = handler
    }

    override suspend fun start() {
        running.set(true)
        scope.launch { connectLoop() }
        logger.info("WS client started, target={}, role={}", wsUrl, role)
    }

    override suspend fun stop() {
        running.set(false)
        // 取消所有 pending 请求
        pendingRequests.values.forEach {
            it.completeExceptionally(CancellationException("WS client stopped"))
        }
        pendingRequests.clear()
        scope.cancel()
        httpClient.close()
        logger.info("WS client stopped")
    }

    /**
     * 连接循环：断线自动重连
     */
    private suspend fun connectLoop() {
        while (running.get()) {
            try {
                connect()
            } catch (e: Exception) {
                logger.warn("WS connection failed: {}, reconnecting in {}ms", e.message, config.reconnectInterval)
            }

            if (running.get()) {
                delay(config.reconnectInterval)
            }
        }
    }

    private suspend fun connect() {
        val url = buildWsUrl()
        logger.info("WS connecting to {} ...", url)

        httpClient.webSocket(url) {
            session = this

            // 仅反向 WS 需要发送这些头
            // Ktor WebSocket 客户端通过 URL 参数传递身份信息
            logger.info("WS connected, role={}", role)

            // 启动心跳（如果需要）
            val heartbeatJob = if (config.heartbeatInterval > 0) {
                scope.launch { heartbeatLoop() }
            } else null

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            handleIncoming(text)
                        }
                        is Frame.Close -> {
                            logger.info("WS closed by server")
                            break
                        }
                        else -> {}
                    }
                }
            } finally {
                heartbeatJob?.cancel()
                session = null
            }
        }
    }

    private fun buildWsUrl(): String {
        val sb = StringBuilder(wsUrl)
        config.accessToken?.let {
            if (sb.contains('?')) sb.append('&') else sb.append('?')
            sb.append("access_token=").append(it)
        }
        return sb.toString()
    }

    private suspend fun DefaultWebSocketSession.heartbeatLoop() {
        while (isActive) {
            delay(config.heartbeatInterval)
            try {
                // 发送空帧或 ping
                send(Frame.Ping(ByteArray(0)))
            } catch (_: Exception) { }
        }
    }

    private suspend fun handleIncoming(text: String) {
        try {
            val element = json.parseToJsonElement(text)
            val obj = element.jsonObject

            // 判断是 API 响应还是事件推送
            val echo = obj["echo"]?.jsonPrimitive?.contentOrNull
            val postType = obj["post_type"]?.jsonPrimitive?.contentOrNull

            if (echo != null && postType == null) {
                // API 响应：匹配 echo
                val response = ActionResponse(
                    status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "ok",
                    retcode = obj["retcode"]?.jsonPrimitive?.intOrNull ?: 0,
                    data = obj["data"],
                    echo = echo,
                )
                pendingRequests.remove(echo)?.complete(response)
                logger.debug("WS response matched: echo={}, status={}", echo, response.status)
            } else if (postType != null) {
                // 事件推送
                val envelope = EventEnvelope(
                    time = obj["time"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis() / 1000,
                    selfId = obj["self_id"]?.jsonPrimitive?.longOrNull ?: 0,
                    postType = postType,
                )
                _eventFlow.emit(envelope)
                logger.debug("WS event received: post_type={}", postType)
            } else if (obj.containsKey("action") && actionHandler != null) {
                // 服务端模式：收到 API 请求
                val action = obj["action"]?.jsonPrimitive?.contentOrNull ?: return
                val params = obj["params"]?.jsonObject ?: JsonObject(emptyMap())
                val reqEcho = echo

                val request = ActionRequest(action, params, reqEcho)
                val response = actionHandler!!(request)
                session?.send(Frame.Text(response.toJsonObject().toString()))
            }
        } catch (e: Exception) {
            logger.error("Error handling WS message: {}", e.message)
        }
    }

    override suspend fun send(request: ActionRequest): ActionResponse {
        val session = session ?: throw IllegalStateException("WS not connected")
        val echo = request.echo ?: generateEcho()

        val deferred = CompletableDeferred<ActionResponse>()
        pendingRequests[echo] = deferred

        try {
            val message = buildJsonObject {
                put("action", request.action)
                put("params", request.params)
                put("echo", echo)
            }

            session.send(Frame.Text(message.toString()))
            logger.debug("WS action sent: {}, echo={}", request.action, echo)

            // 超时等待响应
            return withTimeout(config.timeoutMs) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(echo)
            logger.warn("WS action timeout: {}, echo={}", request.action, echo)
            throw e
        } catch (e: Exception) {
            pendingRequests.remove(echo)
            throw e
        }
    }

    override suspend fun push(event: EventEnvelope) {
        val session = session ?: throw IllegalStateException("WS not connected")
        val message = buildJsonObject {
            put("time", event.time)
            put("self_id", event.selfId)
            put("post_type", event.postType)
        }
        session.send(Frame.Text(message.toString()))
    }

    private var echoCounter = 0L
    private fun generateEcho(): String {
        val id = System.currentTimeMillis().toString(36) + "-" + (++echoCounter).toString(36)
        return id
    }
}
