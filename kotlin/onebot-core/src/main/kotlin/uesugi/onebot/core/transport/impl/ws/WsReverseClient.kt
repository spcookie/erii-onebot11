package uesugi.onebot.core.transport.impl.ws

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
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
    logger: org.slf4j.Logger,
    onConnected: (WebSocketSession) -> Unit = {},
    onFrame: suspend WebSocketSession.(String) -> Unit
) {
    while (scope.isActive) {
        try {
            client.webSocket(url) {
                onConnected(this)
                logger.info("WS reverse connected to {}", url)
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    onFrame(frame.readText())
                }
            }
        } catch (e: Exception) {
            logger.warn("WS reverse disconnected: {}", e.message)
        }
        delay(reconnectInterval.milliseconds)
    }
}

// ===== 反向 WS API 客户端 =====

class WsReverseApiClient(
    private val config: OneBotConfig,
    private val echoTracker: EchoTracker,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : ActionChannel {

    private val logger = LoggerFactory.getLogger(WsReverseApiClient::class.java)
    private val json = JsonFactory.compact
    private var session: WebSocketSession? = null
    private var scope: CoroutineScope? = null
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val apiUrl: String = config.wsReverseApiUrl ?: config.wsReverseUrl
    ?: error("wsReverseApiUrl or wsReverseUrl is required")

    override suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val s = session ?: error("WebSocket not connected")
        val jsonParams = paramParser.serialize(action, params)
        val resp = doCall(s, echoTracker, json, action, jsonParams, config.timeout)
        return resultParser.deserialize(action, resp.data)
    }

    override suspend fun start() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        echoTracker.setScope(scope!!)
        scope!!.launch {
            connectWithRetry(
                apiUrl, config.wsReverseReconnectInterval, scope!!, client, logger,
                onConnected = { session = it },
                onFrame = { text ->
                    try {
                        val resp = json.decodeFromString(ActionResponse.serializer(), text)
                        resp.echo?.let { echoTracker.resolve(it, resp) }
                    } catch (e: Exception) {
                        logger.debug("Non API response on API channel: {}", e.message)
                    }
                }
            )
        }
    }

    override suspend fun stop() {
        echoTracker.cancelAll()
        session?.close()
        scope?.cancel()
    }
}

// ===== 反向 WS Event 客户端 =====

class WsReverseEventClient(
    private val config: OneBotConfig,
    private val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : EventChannel {

    private val logger = LoggerFactory.getLogger(WsReverseEventClient::class.java)
    private val eventChannel = Channel<OneBotEvent>(CONFLATED)
    private var scope: CoroutineScope? = null
    private val eventParser = EventParser(config.messageFormat)
    private val eventUrl: String = config.wsReverseEventUrl ?: config.wsReverseUrl
    ?: error("wsReverseEventUrl or wsReverseUrl is required")
    private val baseJson = JsonFactory.base

    override val events: Flow<OneBotEvent> = eventChannel.receiveAsFlow()

    override suspend fun start() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope!!.launch {
            connectWithRetry(
                eventUrl, config.wsReverseReconnectInterval, scope!!, client, logger,
                onFrame = { text ->
                    try {
                        val event = eventParser.deserialize(text)
                        event.quickOpHandler = QuickOpHandler { op ->
                            val context = baseJson.encodeToJsonElement(OneBotEvent.serializer(), event).jsonObject
                            val operationJson = baseJson.encodeToJsonElement(QuickOperation.serializer(), op).jsonObject
                            val params = JsonObject(mapOf("context" to context, "operation" to operationJson))
                            actionHandler(ActionName.HANDLE_QUICK_OPERATION, RawActionParams(params))
                        }
                        eventChannel.send(event)
                    } catch (e: Exception) {
                        logger.debug("Failed to parse event: {}", e.message)
                    }
                }
            )
        }
    }

    override suspend fun stop() {
        eventChannel.close()
        scope?.cancel()
    }
}

// ===== 反向 WS Universal 客户端 =====

class WsReverseUniversalClient(
    private val config: OneBotConfig,
    private val echoTracker: EchoTracker,
    private val client: HttpClient = HttpClient { install(WebSockets) }
) : ActionChannel, EventChannel {

    private val logger = LoggerFactory.getLogger(WsReverseUniversalClient::class.java)
    private val json = JsonFactory.compact
    private val baseJson = JsonFactory.base
    private val eventChannel = Channel<OneBotEvent>(CONFLATED)
    private var session: WebSocketSession? = null
    private var scope: CoroutineScope? = null
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()
    private val url: String = config.wsReverseUrl
        ?: error("wsReverseUrl is required for universal client")

    override val events: Flow<OneBotEvent> = eventChannel.receiveAsFlow()

    override suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val s = session ?: error("WebSocket not connected")
        val jsonParams = paramParser.serialize(action, params)
        val resp = doCall(s, echoTracker, json, action, jsonParams, config.timeout)
        return resultParser.deserialize(action, resp.data)
    }

    override suspend fun start() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        echoTracker.setScope(scope!!)
        scope!!.launch {
            connectWithRetry(
                url, config.wsReverseReconnectInterval, scope!!, client, logger,
                onConnected = { session = it },
                onFrame = { text ->
                    val elem = json.parseToJsonElement(text)
                    if (elem is JsonObject) {
                        when {
                            elem.containsKey("echo") && elem.containsKey("retcode") -> {
                                val resp = json.decodeFromJsonElement(ActionResponse.serializer(), elem)
                                if (resp.echo != null) {
                                    echoTracker.resolve(resp.echo, resp)
                                } else {
                                    logger.warn("Received response without echo, discarding")
                                }
                            }

                            elem.containsKey("post_type") -> {
                                val event = json.decodeFromJsonElement(OneBotEvent.serializer(), elem)
                                event.quickOpHandler = QuickOpHandler { op ->
                                    val context =
                                        baseJson.encodeToJsonElement(OneBotEvent.serializer(), event).jsonObject
                                    val operationJson =
                                        baseJson.encodeToJsonElement(QuickOperation.serializer(), op).jsonObject
                                    val params = JsonObject(mapOf("context" to context, "operation" to operationJson))
                                    this@WsReverseUniversalClient.call(
                                        ActionName.HANDLE_QUICK_OPERATION,
                                        RawActionParams(params)
                                    )
                                }
                                eventChannel.send(event)
                            }
                        }
                    }
                }
            )
        }
    }

    override suspend fun stop() {
        echoTracker.cancelAll()
        eventChannel.close()
        session?.close()
        scope?.cancel()
    }
}
