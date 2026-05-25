package uesugi.onebot.core.transport.impl.ws

import io.ktor.client.*
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
import uesugi.onebot.core.dispatch.ActionNotFoundException
import uesugi.onebot.core.dispatch.DispatchException
import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.ActionServerChannel
import uesugi.onebot.core.transport.EventPushChannel
import uesugi.onebot.core.transport.JsonFactory
import kotlin.time.Duration.Companion.milliseconds

// ===== 反向 WS 客户端（实现侧，OneBot 实现作为 WS 客户端连接 SDK 服务器）=====

/**
 * 反向 WS Action 客户端（实现侧）。
 *
 * OneBot 实现作为 WS 客户端，连接 SDK 的 WS 服务器，设置 X-Client-Role: API。
 * 接收 SDK 发来的 Action 请求，通过 [actionHandler] 处理后返回响应。
 * 断线自动重连。
 */
class ReverseWsActionClient(
    private val config: OneBotConfig,
    override val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : ActionServerChannel {

    private val logger = LoggerFactory.getLogger(ReverseWsActionClient::class.java)
    private val json = JsonFactory.compact
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val apiUrl: String = config.wsReverseClientApiUrl ?: config.wsReverseClientUrl
    ?: error("wsReverseClientApiUrl or wsReverseClientUrl is required")

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("ReverseWsActionClient coroutine error", e)
        }
    )

    override suspend fun start() {
        scope.launch {
            connectWithRetry(apiUrl, "API")
        }
    }

    override suspend fun stop() {
        scope.cancel()
    }

    private suspend fun connectWithRetry(url: String, role: String) {
        while (scope.isActive) {
            try {
                client.webSocket(url) {
                    request {
                        header("X-Client-Role", role)
                        header("X-Self-ID", config.selfIdStr)
                        config.authHeader?.let { header(HttpHeaders.Authorization, it) }
                    }
                    logger.info("ReverseWsActionClient connected to {}", url)
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
                logger.warn("ReverseWsActionClient disconnected: {}", e.message)
            }
            delay(config.wsReverseClientReconnectInterval.milliseconds)
        }
    }

    private suspend fun handleAction(request: ActionRequest): String {
        val actionResponse = try {
            when (val result = actionHandler(request.action, paramParser.deserialize(request.action, request.params))) {
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
class ReverseWsEventClient(
    private val config: OneBotConfig,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : EventPushChannel {

    private val logger = LoggerFactory.getLogger(ReverseWsEventClient::class.java)
    private val eventParser = EventParser(config.messageFormat)
    private val baseJson = JsonFactory.base
    private val eventUrl: String = config.wsReverseClientEventUrl ?: config.wsReverseClientUrl
    ?: error("wsReverseClientEventUrl or wsReverseClientUrl is required")

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("ReverseWsEventClient coroutine error", e)
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
    }

    override suspend fun stop() {
        sessionMutex.withLock { session = null }
        scope.cancel()
    }

    private suspend fun connectWithRetry(url: String, role: String) {
        while (scope.isActive) {
            try {
                client.webSocket(url) {
                    request {
                        header("X-Client-Role", role)
                        header("X-Self-ID", config.selfIdStr)
                        config.authHeader?.let { header(HttpHeaders.Authorization, it) }
                    }
                    sessionMutex.withLock { session = this }
                    logger.info("ReverseWsEventClient connected to {}", url)
                    for (frame in incoming) {
                        if (frame is Frame.Close) break
                    }
                }
            } catch (e: Exception) {
                logger.warn("ReverseWsEventClient disconnected: {}", e.message)
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
class ReverseWsUniversalClient(
    private val config: OneBotConfig,
    override val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : ActionServerChannel, EventPushChannel {

    private val logger = LoggerFactory.getLogger(ReverseWsUniversalClient::class.java)
    private val json = JsonFactory.compact
    private val baseJson = JsonFactory.base
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val eventParser = EventParser(config.messageFormat)
    private val url: String = config.wsReverseClientUrl
        ?: error("wsReverseClientUrl is required for Universal client")

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("ReverseWsUniversalClient coroutine error", e)
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
    }

    override suspend fun stop() {
        sessionMutex.withLock { session = null }
        scope.cancel()
    }

    private suspend fun connectWithRetry(url: String, role: String) {
        while (scope.isActive) {
            try {
                client.webSocket(url) {
                    request {
                        header("X-Client-Role", role)
                        header("X-Self-ID", config.selfIdStr)
                        config.authHeader?.let { header(HttpHeaders.Authorization, it) }
                    }
                    sessionMutex.withLock { session = this }
                    logger.info("ReverseWsUniversalClient connected to {}", url)
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
                logger.warn("ReverseWsUniversalClient disconnected: {}", e.message)
            } finally {
                sessionMutex.withLock { session = null }
            }
            delay(config.wsReverseClientReconnectInterval.milliseconds)
        }
    }

    private suspend fun handleAction(request: ActionRequest): String {
        val actionResponse = try {
            val result = actionHandler(request.action, paramParser.deserialize(request.action, request.params))
            when (result) {
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
        return json.encodeToString(ActionResponse.serializer(), actionResponse)
    }
}
