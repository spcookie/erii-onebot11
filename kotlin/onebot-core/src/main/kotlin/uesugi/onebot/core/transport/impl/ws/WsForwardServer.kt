package uesugi.onebot.core.transport.impl.ws

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.ActionNotFoundException
import uesugi.onebot.core.dispatch.DispatchException
import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.*

/**
 * 正向 WebSocket 服务端。
 *
 * OneBot 实现侧：启动 WS 服务器，等待客户端连接。
 * - `/api` — 接收 Action 请求，返回 Action 响应
 * - `/event` — 向客户端推送事件
 * - `/` — 同一连接处理 Action + Event（Universal）
 */
class WsForwardServer(
    private val config: OneBotConfig,
    private val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    private val onConnect: (suspend () -> Unit)? = null
) : EventPushChannel {

    private val logger = LoggerFactory.getLogger(WsForwardServer::class.java)
    private val json = JsonFactory.compact
    private val eventJson = JsonFactory.base
    private var server: EmbeddedServer<*, *>? = null
    private val sessionMutex = Mutex()
    private val eventSessions = mutableSetOf<WebSocketSession>()
    private val paramsParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val eventParser = EventParser(config.messageFormat)

    override suspend fun pushEvent(event: OneBotEvent) {
        val serialized = eventParser.serialize(event)
        val eventText = eventJson.encodeToString(JsonObject.serializer(), serialized)
        val sessions = sessionMutex.withLock { eventSessions.toSet() }
        val dead = mutableSetOf<WebSocketSession>()
        for (session in sessions) {
            try {
                session.outgoing.send(Frame.Text(eventText))
            } catch (e: Exception) {
                logger.warn("Failed to push event to session", e)
                dead.add(session)
            }
        }
        if (dead.isNotEmpty()) {
            sessionMutex.withLock { eventSessions.removeAll(dead) }
        }
    }

    override suspend fun start() {
        server = embeddedServer(Netty, host = config.wsHost, port = config.wsPort) {
            install(WebSockets)
            routing {
                webSocket("/") { handleUniversalSession() }
                webSocket("/api") { handleApiSession() }
                webSocket("/event") { handleEventSession() }
            }
        }
        server!!.start(wait = false)
        logger.info("WS Forward server started on ws://{}:{}", config.wsHost, config.wsPort)
    }

    override suspend fun stop() {
        sessionMutex.withLock { eventSessions.clear() }
        server?.stop(STOP_GRACE_PERIOD, STOP_TIMEOUT)
        logger.info("WS Forward server stopped")
    }

    private suspend fun DefaultWebSocketServerSession.handleUniversalSession() {
        if (!tryAddEventSession(this)) {
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Too many event sessions"))
            return
        }
        onConnect?.invoke()
        try {
            processActionFrames()
        } catch (e: Exception) {
            logger.debug("WS universal session closed: {}", e.message)
        } finally {
            removeEventSession(this)
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleApiSession() {
        try {
            processActionFrames()
        } catch (e: Exception) {
            logger.debug("WS API session closed: {}", e.message)
        }
    }

    private suspend fun DefaultWebSocketServerSession.processActionFrames() {
        for (frame in incoming) {
            if (frame !is Frame.Text) continue
            val elem = json.parseToJsonElement(frame.readText())
            if (elem is JsonObject && elem.containsKey("action")) {
                handleActionFrame(elem)
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleActionFrame(elem: JsonObject) {
        val request = json.decodeFromJsonElement(ActionRequest.serializer(), elem)

        val actionResponse = try {
            when (val result =
                actionHandler(request.action, paramsParser.deserialize(request.action, request.params))) {
                is AsyncActionResult -> ActionResponse.async(request.echo)
                else -> {
                    val data = resultParser.serialize(request.action, result)
                    ActionResponse.ok(data, request.echo)
                }
            }
        } catch (e: ActionNotFoundException) {
            logger.debug("Action not found: {}", request.action)
            ActionResponse.notFound(request.echo)
        } catch (e: DispatchException) {
            logger.warn("Dispatch error for action {}: retcode={}", request.action, e.retcode)
            ActionResponse.failed(e.retcode, request.echo)
        } catch (e: Exception) {
            logger.warn("Failed to handle action {}", request.action, e)
            ActionResponse.badRequest(request.echo)
        }
        val respJson = json.encodeToString(ActionResponse.serializer(), actionResponse)
        outgoing.send(Frame.Text(respJson))
    }

    private suspend fun DefaultWebSocketServerSession.handleEventSession() {
        if (!tryAddEventSession(this)) {
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Too many event sessions"))
            return
        }
        onConnect?.invoke()
        try {
            for (frame in incoming) {
                if (frame is Frame.Close) break
            }
        } catch (e: Exception) {
            logger.debug("WS event session closed: {}", e.message)
        } finally {
            removeEventSession(this)
        }
    }

    private suspend fun tryAddEventSession(session: WebSocketSession): Boolean =
        sessionMutex.withLock {
            if (eventSessions.size >= MAX_EVENT_SESSIONS) return false
            eventSessions.add(session)
            true
        }

    private suspend fun removeEventSession(session: WebSocketSession) {
        sessionMutex.withLock { eventSessions.remove(session) }
    }
}
