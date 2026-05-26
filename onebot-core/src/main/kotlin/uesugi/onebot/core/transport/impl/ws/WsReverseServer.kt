package uesugi.onebot.core.transport.impl.ws

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.*
import uesugi.onebot.core.util.EchoTracker

/**
 * 反向 WebSocket 服务端（SDK 侧使用）。
 *
 * 根据 OneBot 11 反向 WS 规范，SDK 启动 WS 服务器，
 * OneBot 实现作为 WS 客户端主动连接过来。
 *
 * 通过 HTTP 升级请求中的 [X-Client-Role] Header 区分连接类型：
 * - "API" → ActionChannel（SDK 发送 API 请求，接收响应）
 * - "Event" → EventChannel（SDK 接收 OneBot 推送的事件）
 * - "Universal" → 单连接同时处理 Action + Event
 *
 * 连接握手 Header：
 * - X-Client-Role: API / Event / Universal
 * - X-Self-ID: 机器人 QQ 号
 * - Authorization: Bearer <token>（如果配置了 access_token）
 */
class WsReverseServer(
    private val config: OneBotConfig,
    private val echoTracker: EchoTracker
) : ActionChannel, EventChannel {

    private val logger = LoggerFactory.getLogger(WsReverseServer::class.java)
    private val json = JsonFactory.compact
    private val baseJson = JsonFactory.base
    private val eventParser = EventParser(config.messageFormat)
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("WsReverseServer coroutine error", e)
        }
    )

    private val eventChannel = Channel<OneBotEvent>(Channel.UNLIMITED)
    private val sessionsMutex = Mutex()
    private val apiSessions = mutableSetOf<WebSocketSession>()
    private val universalSessions = mutableSetOf<WebSocketSession>()

    private var server: EmbeddedServer<*, *>? = null

    override val events: Flow<OneBotEvent> = eventChannel.receiveAsFlow()

    // ===== ActionChannel =====

    override suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val session = selectSession()
            ?: error("No API or Universal WebSocket session available")

        val jsonParams = paramParser.serialize(action, params)
        val echo = echoTracker.generateEcho()
        val deferred = echoTracker.register(echo, config.timeout)
        val request = ActionRequest(action = action, params = jsonParams, echo = echo)
        val requestJson = json.encodeToString(ActionRequest.serializer(), request)

        try {
            session.outgoing.send(Frame.Text(requestJson))
        } catch (e: Exception) {
            echoTracker.resolve(echo, ActionResponse.failed(-1, echo))
            throw e
        }

        val resp = deferred.await()
        return resultParser.deserialize(action, resp.data)
    }

    /** 选一个可用 session：优先 Universal，再 API */
    private suspend fun selectSession(): WebSocketSession? =
        sessionsMutex.withLock {
            universalSessions.firstOrNull() ?: apiSessions.firstOrNull()
        }

    // ===== 生命周期 =====

    override suspend fun start() {
        echoTracker.setScope(scope)
        server = embeddedServer(Netty, host = config.wsReverseServerHost, port = config.wsReverseServerPort) {
            install(WebSockets)
            routing {
                webSocket("/") { handleSession() }
            }
        }
        val started = CompletableDeferred<Unit>()
        server!!.monitor.subscribe(ApplicationStarted) { started.complete(Unit) }
        server!!.start(wait = false)
        started.await()
        logger.info("WsReverseServer started on ws://{}:{}", config.wsReverseServerHost, config.wsReverseServerPort)
    }

    override suspend fun stop() {
        sessionsMutex.withLock {
            apiSessions.clear()
            universalSessions.clear()
        }
        eventChannel.close()
        echoTracker.cancelAll()
        server?.stop(STOP_GRACE_PERIOD, STOP_TIMEOUT)
        scope.cancel()
        logger.info("WsReverseServer stopped")
    }

    // ===== WebSocket Session 处理 =====

    private suspend fun DefaultWebSocketServerSession.handleSession() {
        val role = call.request.headers["X-Client-Role"]?.lowercase() ?: "universal"
        val selfId = call.request.headers["X-Self-ID"] ?: "unknown"

        if (!authenticateWs(config, logger)) return

        logger.info("Reverse WS client connected: role={}, selfId={}", role, selfId)

        when (role) {
            "api" -> handleApiSession(selfId)
            "event" -> handleEventSession(selfId)
            else -> handleUniversalSession(selfId)
        }
    }

    // --- API Session ---

    private suspend fun DefaultWebSocketServerSession.handleApiSession(selfId: String) {
        if (!tryAddSession(this, apiSessions)) {
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Too many API sessions"))
            return
        }
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                try {
                    val resp = json.decodeFromString(ActionResponse.serializer(), frame.readText())
                    if (resp.retcode != 0) {
                        logger.warn("API call failed via reverse WS: retcode={}, echo={}", resp.retcode, resp.echo)
                    }
                    resp.echo?.let { echoTracker.resolve(it, resp) }
                } catch (e: Exception) {
                    logger.debug("Non API response on reverse API channel: {}", e.message)
                }
            }
        } catch (e: Exception) {
            logger.debug("API session closed: selfId={}, reason={}", selfId, e.message)
        } finally {
            sessionsMutex.withLock { apiSessions.remove(this) }
        }
    }

    // --- Event Session ---

    private suspend fun DefaultWebSocketServerSession.handleEventSession(selfId: String) {
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                try {
                    val event = eventParser.deserialize(frame.readText())
                    setupQuickOpHandler(event)
                    eventChannel.send(event)
                } catch (e: Exception) {
                    logger.debug("Failed to parse event from selfId={}: {}", selfId, e.message)
                }
            }
        } catch (e: Exception) {
            logger.debug("Event session closed: selfId={}, reason={}", selfId, e.message)
        }
    }

    // --- Universal Session ---

    private suspend fun DefaultWebSocketServerSession.handleUniversalSession(selfId: String) {
        if (!tryAddSession(this, universalSessions)) {
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Too many Universal sessions"))
            return
        }
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                try {
                    val elem = json.parseToJsonElement(text)
                    if (elem is JsonObject) {
                        when {
                            elem.containsKey("echo") && elem.containsKey("retcode") -> {
                                val resp = json.decodeFromJsonElement(ActionResponse.serializer(), elem)
                                if (resp.retcode != 0) {
                                    logger.warn(
                                        "API call failed via reverse WS: retcode={}, echo={}",
                                        resp.retcode,
                                        resp.echo
                                    )
                                }
                                if (resp.echo != null) {
                                    echoTracker.resolve(resp.echo, resp)
                                } else {
                                    logger.warn("Received response without echo, discarding")
                                }
                            }

                            elem.containsKey("post_type") -> {
                                val event = eventParser.deserialize(text)
                                setupQuickOpHandler(event)
                                eventChannel.send(event)
                            }

                            else -> {
                                logger.debug("Unknown frame type from selfId={}", selfId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.debug("Failed to process universal frame from selfId={}: {}", selfId, e.message)
                }
            }
        } catch (e: Exception) {
            logger.debug("Universal session closed: selfId={}, reason={}", selfId, e.message)
        } finally {
            sessionsMutex.withLock { universalSessions.remove(this) }
        }
    }

    // --- 工具 ---

    private fun setupQuickOpHandler(event: OneBotEvent) {
        event.quickOpHandler = createQuickOpHandler(event, baseJson, ::call, logger)
    }

    // --- Session 管理 ---

    private suspend fun tryAddSession(session: WebSocketSession, sessions: MutableSet<WebSocketSession>): Boolean =
        sessionsMutex.withLock {
            if (sessions.size >= 64) return false
            sessions.add(session)
            true
        }
}
