package uesugi.onebot.lib.transport.impl.ws

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.ActionServerChannel
import uesugi.onebot.core.transport.EventPushChannel
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.core.transport.handleActionRequest
import kotlin.time.Duration.Companion.milliseconds

// ===== 反向 WS 客户端（实现侧，OneBot 实现作为 WS 客户端连接 SDK 服务器）=====

/**
 * 反向 WS Action 客户端（实现侧）。
 *
 * OneBot 实现作为 WS 客户端，连接 SDK 的 WS 服务器，设置 X-Client-Role: API。
 * 接收 SDK 发来的 Action 请求，通过 [actionHandler] 处理后返回响应。
 * 断线自动重连。
 */
class WsReverseActionClient(
    private val config: OneBotConfig,
    override val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    private val client: HttpClient = HttpClient(CIO) { install(WebSockets) }
) : ActionServerChannel {

    private val logger = LoggerFactory.getLogger(WsReverseActionClient::class.java)
    private val json = JsonFactory.compact
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val apiUrl: String = config.wsReverseClientApiUrl ?: config.wsReverseClientUrl
    ?: error("wsReverseClientApiUrl or wsReverseClientUrl is required")
    private var ready = CompletableDeferred<Unit>()

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("WsReverseActionClient coroutine error", e)
        }
    )

    override suspend fun start() {
        scope.launch {
            connectWithRetry(apiUrl, "API")
        }
        ready.await()
    }

    override suspend fun stop() {
        scope.cancel()
    }

    private suspend fun connectWithRetry(url: String, role: String) {
        while (scope.isActive) {
            try {
                client.webSocket(url, request = {
                    header("X-Client-Role", role)
                    header("X-Self-ID", config.selfIdStr)
                    config.authHeader?.let { header(HttpHeaders.Authorization, it) }
                }) {
                    logger.info("WsReverseActionClient connected to {}", url)
                    ready.complete(Unit)
                    ready = CompletableDeferred()
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val responseJson = try {
                            val elem = json.parseToJsonElement(frame.readText())
                            if (elem is JsonObject && elem.containsKey("action")) {
                                val request = json.decodeFromJsonElement(ActionRequest.serializer(), elem)
                                handleAction(request)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            logger.debug("Invalid action request frame: {}", e.message)
                            null
                        }
                        if (responseJson != null) {
                            outgoing.send(Frame.Text(responseJson))
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("WsReverseActionClient disconnected({}): {}", url, e.message)
            }
            delay(config.wsReverseClientReconnectInterval.milliseconds)
        }
    }

    private suspend fun handleAction(request: ActionRequest): String {
        val actionResponse = handleActionRequest(request, actionHandler, paramParser, resultParser, logger)
        return json.encodeToString(ActionResponse.serializer(), actionResponse)
    }
}

/**
 * 反向 WS Event 客户端（实现侧）。
 *
 * OneBot 实现作为 WS 客户端，连接 SDK 的 WS 服务器，设置 X-Client-Role: Event。
 * 通过 [pushEvent] 向 SDK 推送事件。
 * 断线自动重连。
 */
class WsReverseEventClient(
    private val config: OneBotConfig,
    private val client: HttpClient = HttpClient(CIO) { install(WebSockets) }
) : EventPushChannel {

    private val logger = LoggerFactory.getLogger(WsReverseEventClient::class.java)
    private val eventParser = EventParser(config.messageFormat)
    private val baseJson = JsonFactory.base
    private val eventUrl: String = config.wsReverseClientEventUrl ?: config.wsReverseClientUrl
    ?: error("wsReverseClientEventUrl or wsReverseClientUrl is required")
    private var ready = CompletableDeferred<Unit>()

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("WsReverseEventClient coroutine error", e)
        }
    )

    private var session: WebSocketSession? = null
    private val sessionMutex = Mutex()

    override suspend fun pushEvent(event: OneBotEvent) {
        val s = sessionMutex.withLock { session } ?: run {
            logger.warn("No active Event session, dropping event")
            return
        }
        try {
            val eventJson = baseJson.encodeToString(JsonObject.serializer(), eventParser.serialize(event))
            s.outgoing.send(Frame.Text(eventJson))
        } catch (e: Exception) {
            logger.warn("Failed to push event", e)
        }
    }

    override suspend fun start() {
        scope.launch {
            connectWithRetry(eventUrl, "Event")
        }
        ready.await()
    }

    override suspend fun stop() {
        sessionMutex.withLock { session = null }
        scope.cancel()
    }

    private suspend fun connectWithRetry(url: String, role: String) {
        while (scope.isActive) {
            try {
                client.webSocket(url, request = {
                    header("X-Client-Role", role)
                    header("X-Self-ID", config.selfIdStr)
                    config.authHeader?.let { header(HttpHeaders.Authorization, it) }
                }) {
                    sessionMutex.withLock { session = this }
                    logger.info("WsReverseEventClient connected to {}", url)
                    ready.complete(Unit)
                    ready = CompletableDeferred()
                    for (frame in incoming) {
                        if (frame is Frame.Close) break
                    }
                }
            } catch (e: Exception) {
                logger.warn("WsReverseEventClient disconnected: {}", e.message)
            } finally {
                sessionMutex.withLock { session = null }
            }
            delay(config.wsReverseClientReconnectInterval.milliseconds)
        }
    }
}

/**
 * 反向 WS Universal 客户端（实现侧）。
 *
 * OneBot 实现作为 WS 客户端，连接 SDK 的 WS 服务器，设置 X-Client-Role: Universal。
 * 单连接同时处理 Action 接收/响应 和 Event 推送。
 * 断线自动重连。
 */
class WsReverseUniversalClient(
    private val config: OneBotConfig,
    override val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    private val client: HttpClient = HttpClient(CIO) { install(WebSockets) }
) : ActionServerChannel, EventPushChannel {

    private val logger = LoggerFactory.getLogger(WsReverseUniversalClient::class.java)
    private val json = JsonFactory.compact
    private val baseJson = JsonFactory.base
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val eventParser = EventParser(config.messageFormat)
    private val url: String = config.wsReverseClientUrl
        ?: error("wsReverseClientUrl is required for Universal client")
    private var ready = CompletableDeferred<Unit>()

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("WsReverseUniversalClient coroutine error", e)
        }
    )

    private var session: WebSocketSession? = null
    private val sessionMutex = Mutex()

    // ===== ActionServerChannel =====

    override suspend fun pushEvent(event: OneBotEvent) {
        val s = sessionMutex.withLock { session } ?: run {
            logger.warn("No active Universal session, dropping event")
            return
        }
        try {
            val eventJson = baseJson.encodeToString(JsonObject.serializer(), eventParser.serialize(event))
            s.outgoing.send(Frame.Text(eventJson))
        } catch (e: Exception) {
            logger.warn("Failed to push event", e)
        }
    }

    override suspend fun start() {
        scope.launch {
            connectWithRetry(url, "Universal")
        }
        ready.await()
    }

    override suspend fun stop() {
        sessionMutex.withLock { session = null }
        scope.cancel()
    }

    private suspend fun connectWithRetry(url: String, role: String) {
        while (scope.isActive) {
            try {
                client.webSocket(url, request = {
                    header("X-Client-Role", role)
                    header("X-Self-ID", config.selfIdStr)
                    config.authHeader?.let { header(HttpHeaders.Authorization, it) }
                }) {
                    sessionMutex.withLock { session = this }
                    logger.info("WsReverseUniversalClient connected to {}", url)
                    ready.complete(Unit)
                    ready = CompletableDeferred()
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()
                        val responseJson = try {
                            val elem = json.parseToJsonElement(text)
                            if (elem is JsonObject && elem.containsKey("action")) {
                                val request = json.decodeFromJsonElement(ActionRequest.serializer(), elem)
                                handleAction(request)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            logger.debug("Invalid universal frame: {}", e.message)
                            null
                        }
                        if (responseJson != null) {
                            outgoing.send(Frame.Text(responseJson))
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("WsReverseUniversalClient disconnected: {}", e.message)
            } finally {
                sessionMutex.withLock { session = null }
            }
            delay(config.wsReverseClientReconnectInterval.milliseconds)
        }
    }

    private suspend fun handleAction(request: ActionRequest): String {
        val actionResponse = handleActionRequest(request, actionHandler, paramParser, resultParser, logger)
        return json.encodeToString(ActionResponse.serializer(), actionResponse)
    }
}
