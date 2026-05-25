package uesugi.onebot.core.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.impl.http.HttpActionClient
import uesugi.onebot.core.transport.impl.http.HttpActionServer
import uesugi.onebot.core.transport.impl.http.HttpEventClient
import uesugi.onebot.core.transport.impl.http.HttpEventServer
import uesugi.onebot.core.transport.impl.ws.*
import uesugi.onebot.core.util.EchoTracker
import kotlin.time.Duration.Companion.milliseconds

/**
 * 传输连接统一 Facade。
 *
 * 根据 [OneBotConfig] 组装正确的 Action 和 Event 传输通道组合。
 * 支持以下几种模式：
 *
 * **SDK 端（客户端）模式：**
 * - HTTP：HttpActionClient（Action）+ 可选的 HttpEventServer（Event 接收）
 * - 正向 WS：WsForwardApiClient（Action）+ WsForwardEventClient（Event）
 * - 正向 WS Universal：WsForwardUniversalClient（Action + Event 单一连接）
 *
 * **服务端（实现）模式：**
 * - HTTP：HttpActionServer（Action）+ 可选的 HttpEventClient（Event 推送）
 * - 正向 WS：WsForwardServer（Event 推送 + 内置 Action 处理）
 * - 反向 WS：WsReverseActionClient / WsReverseEventClient / WsReverseUniversalClient
 */
class Connection(private val config: OneBotConfig) {

    private val logger = LoggerFactory.getLogger(Connection::class.java)
    private var actionServer: ActionServerChannel? = null
    private var actionClient: ActionChannel? = null
    private var eventChannel: EventChannel? = null
    private var eventPushChannel: EventPushChannel? = null
    private val echoTracker = EchoTracker()
    private var initialized = false
    private val serverScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("Connection serverScope coroutine error", e)
        }
    )
    private var heartbeatJob: Job? = null

    // ===== SDK 端构建方法 =====

    fun buildClient() {
        require(!initialized) { "Connection already initialized" }

        if (config.httpEnable) {
            actionClient = HttpActionClient(config)
        }

        if (config.httpPostEnable) {
            eventChannel = HttpEventServer(config)
        }

        if (config.wsForwardClientEnable) {
            if (config.wsForwardClientUseUniversal) {
                val universal = WsForwardUniversalClient(config, echoTracker)
                actionClient = universal
                eventChannel = universal
            } else {
                val apiClient = WsForwardApiClient(config, echoTracker)
                actionClient = apiClient
                val eventClient = WsForwardEventClient(config, actionHandler = { action, params ->
                    apiClient.call(action, params)
                })
                if (config.wsForwardClientEventUrl != null || config.wsForwardClientUrl != null) {
                    eventChannel = eventClient
                }
            }
        }

        // 反向 WS 服务端（SDK 作为 WS 服务器，OneBot 实现连接过来）
        if (config.wsReverseServerEnable) {
            val server = WsReverseServer(config, echoTracker)
            if (actionClient == null) actionClient = server
            if (eventChannel == null) eventChannel = server
        }
        initialized = true
    }

    // ===== 服务端构建方法 =====

    fun buildServer(actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult) {
        require(!initialized) { "Connection already initialized" }

        if (config.httpEnable) {
            actionServer = HttpActionServer(config, actionHandler = actionHandler)
        }

        if (config.httpPostEnable && config.httpPostUrl != null) {
            eventPushChannel = HttpEventClient(config)
        }

        if (config.wsForwardServerEnable) {
            eventPushChannel = WsForwardServer(config, actionHandler, onConnect = {
                pushLifecycleEvent("connect")
            })
        }

        // 反向 WS 客户端（实现侧，OneBot 实现作为 WS 客户端连接 SDK 服务器）
        if (config.wsReverseClientEnable) {
            if (config.wsReverseClientUseUniversal) {
                val universal = WsReverseUniversalClient(config, actionHandler)
                actionServer = universal
                eventPushChannel = universal
            } else {
                if (config.wsReverseClientApiUrl != null || config.wsReverseClientUrl != null) {
                    actionServer = WsReverseActionClient(config, actionHandler)
                }
                if (config.wsReverseClientEventUrl != null || config.wsReverseClientUrl != null) {
                    eventPushChannel = WsReverseEventClient(config)
                }
            }
        }
        initialized = true
    }

    // ===== Action 调用 =====

    suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val client = actionClient ?: error("No ActionChannel configured. Call buildClient() or buildServer() first.")
        return client.call(action, params)
    }

    // ===== Event 流（SDK 端） =====

    /** 事件流（SDK 端从 OneBot 实现接收事件） */
    val events: Flow<OneBotEvent>
        get() = eventChannel?.events ?: emptyFlow()

    // ===== Event 推送（服务端） =====

    /** 推送事件（服务端向 SDK 推送事件） */
    suspend fun pushEvent(event: OneBotEvent) {
        eventPushChannel?.pushEvent(event)
            ?: error("No EventPushChannel configured. Call buildServer() first.")
    }

    // ===== 生命周期 =====

    /** 启动所有通道（并行）。服务端模式下自动发送 lifecycle enable 事件并启动心跳。 */
    suspend fun start() {
        coroutineScope {
            val started = mutableSetOf<Any>()
            actionServer?.let { channel -> if (started.add(channel)) launch { channel.start() } }
            actionClient?.let { channel -> if (started.add(channel)) launch { channel.start() } }
            eventChannel?.let { channel -> if (started.add(channel)) launch { channel.start() } }
            eventPushChannel?.let { channel -> if (started.add(channel)) launch { channel.start() } }
        }
        if (eventPushChannel != null) {
            serverScope.launch {
                try {
                    pushLifecycleEvent("enable")
                } catch (e: Exception) {
                    logger.error("Failed to push lifecycle enable event", e)
                }
            }
            if (config.heartbeatEnable) {
                heartbeatJob = serverScope.launch {
                    while (isActive) {
                        delay(config.heartbeatInterval.milliseconds)
                        try {
                            pushEvent(
                                HeartbeatMetaEvent(
                                    time = System.currentTimeMillis() / 1000,
                                    selfId = config.selfId,
                                    interval = config.heartbeatInterval
                                )
                            )
                        } catch (_: Exception) {
                            // push failure shouldn't crash the heartbeat loop
                        }
                    }
                }
            }
        }
    }

    /** 停止所有通道，取消所有待处理请求。服务端模式下自动发送 lifecycle disable 事件。 */
    suspend fun stop() {
        if (eventPushChannel != null) {
            pushLifecycleEvent("disable")
        }
        heartbeatJob?.cancel()
        heartbeatJob = null
        serverScope.cancel()

        echoTracker.cancelAll()
        coroutineScope {
            actionServer?.let { launch { it.stop() } }
            actionClient?.let { launch { it.stop() } }
            eventChannel?.let { launch { it.stop() } }
            eventPushChannel?.let { launch { it.stop() } }
        }
    }

    private suspend fun pushLifecycleEvent(subType: String) {
        try {
            pushEvent(
                LifecycleMetaEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = config.selfId,
                    subType = subType
                )
            )
        } catch (e: Exception) {
            logger.warn("Failed to push lifecycle event {}: {}", subType, e.message)
        }
    }

    val isInitialized: Boolean get() = initialized
}
