package uesugi.onebot.core.transport.impl.ws

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.ActionChannel
import uesugi.onebot.core.transport.EventChannel
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.core.util.EchoTracker
import kotlin.time.Duration.Companion.milliseconds

// ===== 共享辅助函数 =====

/** 通过 EchoTracker 发送 WS Action 请求并等待响应 */
private suspend fun doCall(
    session: WebSocketSession,
    echoTracker: EchoTracker,
    json: Json,
    action: String,
    params: JsonObject,
    timeout: Long
): ActionResponse {
    val echo = echoTracker.generateEcho()
    val deferred = echoTracker.register(echo, timeout)
    val request = ActionRequest(action = action, params = params, echo = echo)
    val requestJson = json.encodeToString(ActionRequest.serializer(), request)
    session.outgoing.send(Frame.Text(requestJson))
    return deferred.await()
}

/** 带自动重连的 WebSocket 连接循环 */
private suspend fun connectWithRetry(
    url: String,
    reconnectInterval: Long,
    scope: CoroutineScope,
    client: HttpClient,
    logger: Logger,
    request: HttpRequestBuilder.() -> Unit = {},
    onConnected: (WebSocketSession) -> Unit = {},
    onFrame: suspend WebSocketSession.(String) -> Unit
) {
    while (scope.isActive) {
        try {
            client.webSocket(url, request = request) {
                onConnected(this)
                logger.info("WS forward connected to {}", url)
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    onFrame(frame.readText())
                }
            }
        } catch (e: Exception) {
            logger.warn("WS forward disconnected: {}", e.message)
        }
        delay(reconnectInterval.milliseconds)
    }
}

/** 创建带异常处理器的 CoroutineScope */
private fun createScope(logger: Logger): CoroutineScope {
    val handler = CoroutineExceptionHandler { _, e ->
        logger.error("Transport coroutine error", e)
    }
    return CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)
}

// ===== 正向 WS API 客户端（SDK 侧）=====

class WsForwardApiClient(
    private val config: OneBotConfig,
    private val echoTracker: EchoTracker,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : ActionChannel {

    private val logger = LoggerFactory.getLogger(WsForwardApiClient::class.java)
    private val json = JsonFactory.compact
    private var session: WebSocketSession? = null
    private var scope: CoroutineScope? = null
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val apiUrl: String = config.wsForwardClientApiUrl ?: config.wsForwardClientUrl
    ?: error("wsForwardClientApiUrl or wsForwardClientUrl is required")
    private var ready = CompletableDeferred<Unit>()

    override suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val s = session ?: error("WebSocket not connected")
        val jsonParams = paramParser.serialize(action, params)
        val resp = doCall(s, echoTracker, json, action, jsonParams, config.timeout)
        return resultParser.deserialize(action, resp.data)
    }

    override suspend fun start() {
        scope = createScope(logger)
        echoTracker.setScope(scope!!)
        scope!!.launch {
            connectWithRetry(
                apiUrl, config.wsForwardClientReconnectInterval, scope!!, client, logger,
                request = { config.authHeader?.let { header(HttpHeaders.Authorization, it) } },
                onConnected = { session = it; ready.complete(Unit); ready = CompletableDeferred() },
                onFrame = { text ->
                    try {
                        val resp = json.decodeFromString(ActionResponse.serializer(), text)
                        if (resp.retcode != 0) {
                            logger.warn("API call failed: retcode={}, echo={}", resp.retcode, resp.echo)
                        }
                        resp.echo?.let { echoTracker.resolve(it, resp) }
                    } catch (e: Exception) {
                        logger.debug("Non API response on API channel: {}", e.message)
                    }
                }
            )
        }
        ready.await()
    }

    override suspend fun stop() {
        echoTracker.cancelAll()
        session?.close()
        scope?.cancel()
    }
}

// ===== 正向 WS Event 客户端（SDK 侧）=====

class WsForwardEventClient(
    private val config: OneBotConfig,
    private val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : EventChannel {

    private val logger = LoggerFactory.getLogger(WsForwardEventClient::class.java)
    private val eventChannel = Channel<OneBotEvent>(CONFLATED)
    private var scope: CoroutineScope? = null
    private val eventParser = EventParser(config.messageFormat)
    private val eventUrl: String = config.wsForwardClientEventUrl ?: config.wsForwardClientUrl
    ?: error("wsForwardClientEventUrl or wsForwardClientUrl is required")
    private val baseJson = JsonFactory.base
    private var ready = CompletableDeferred<Unit>()

    override val events: Flow<OneBotEvent> = eventChannel.receiveAsFlow()

    override suspend fun start() {
        scope = createScope(logger)
        scope!!.launch {
            connectWithRetry(
                eventUrl, config.wsForwardClientReconnectInterval, scope!!, client, logger,
                request = { config.authHeader?.let { header(HttpHeaders.Authorization, it) } },
                onConnected = { ready.complete(Unit); ready = CompletableDeferred() },
                onFrame = { text ->
                    try {
                        val event = eventParser.deserialize(text)
                        event.quickOpHandler = createQuickOpHandler(event, baseJson, actionHandler, logger)
                        eventChannel.send(event)
                    } catch (e: Exception) {
                        logger.debug("Failed to parse event: {}", e.message)
                    }
                }
            )
        }
        ready.await()
    }

    override suspend fun stop() {
        eventChannel.close()
        scope?.cancel()
    }
}

// ===== 正向 WS Universal 客户端（SDK 侧）=====

class WsForwardUniversalClient(
    private val config: OneBotConfig,
    private val echoTracker: EchoTracker,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : ActionChannel, EventChannel {

    private val logger = LoggerFactory.getLogger(WsForwardUniversalClient::class.java)
    private val json = JsonFactory.compact
    private val baseJson = JsonFactory.base
    private val eventChannel = Channel<OneBotEvent>(CONFLATED)
    private var session: WebSocketSession? = null
    private var scope: CoroutineScope? = null
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val eventParser = EventParser(config.messageFormat)
    private val url: String = config.wsForwardClientUrl
        ?: error("wsForwardClientUrl is required for universal client")
    private var ready = CompletableDeferred<Unit>()

    override val events: Flow<OneBotEvent> = eventChannel.receiveAsFlow()

    override suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val s = session ?: error("WebSocket not connected")
        val jsonParams = paramParser.serialize(action, params)
        val resp = doCall(s, echoTracker, json, action, jsonParams, config.timeout)
        return resultParser.deserialize(action, resp.data)
    }

    override suspend fun start() {
        scope = createScope(logger)
        echoTracker.setScope(scope!!)
        scope!!.launch {
            connectWithRetry(
                url, config.wsForwardClientReconnectInterval, scope!!, client, logger,
                request = { config.authHeader?.let { header(HttpHeaders.Authorization, it) } },
                onConnected = { session = it; ready.complete(Unit); ready = CompletableDeferred() },
                onFrame = { text ->
                    try {
                        val elem = json.parseToJsonElement(text)
                        if (elem is JsonObject) {
                            when {
                                elem.containsKey("echo") && elem.containsKey("retcode") -> {
                                    val resp = json.decodeFromJsonElement(ActionResponse.serializer(), elem)
                                    if (resp.retcode != 0) {
                                        logger.warn("API call failed: retcode={}, echo={}", resp.retcode, resp.echo)
                                    }
                                    if (resp.echo != null) {
                                        echoTracker.resolve(resp.echo, resp)
                                    } else {
                                        logger.warn("Received response without echo, discarding")
                                    }
                                }

                                elem.containsKey("post_type") -> {
                                    val event = eventParser.deserialize(text)
                                    event.quickOpHandler = createQuickOpHandler(
                                        event, baseJson,
                                        { action, params -> this@WsForwardUniversalClient.call(action, params) },
                                        logger
                                    )
                                    eventChannel.send(event)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to process frame", e)
                    }
                }
            )
        }
        ready.await()
    }

    override suspend fun stop() {
        echoTracker.cancelAll()
        eventChannel.close()
        session?.close()
        scope?.cancel()
    }
}
