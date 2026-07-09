package uesugi.onebot.core.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.core.util.EchoTracker
import kotlin.time.Duration.Companion.milliseconds

/**
 * 传输连接统一 Facade。
 *
 * 通过 setter 注入具体通道实现，负责生命周期管理。
 * 构建逻辑由各模块的 TransportBuilder 负责：
 * - [SdkTransportBuilder]（onebot-sdk）：组装 SDK 端通道
 * - [ServerTransportBuilder]（onebot-lib）：组装服务端通道
 *
 * 支持以下模式：
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
    private val echoTracker = EchoTracker()

    /** Action 调用通道（SDK 侧）。 */
    var actionChannel: ActionChannel? = null

    /** Action 接收通道（服务端侧）。 */
    var actionServerChannel: ActionServerChannel? = null

    /** 事件接收通道（SDK 侧）。 */
    var eventChannel: EventChannel? = null

    /** 事件推送通道（服务端侧）。 */
    var eventPushChannel: EventPushChannel? = null

    private val serverScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            logger.error("Connection serverScope coroutine error", e)
        }
    )
    private var heartbeatJob: Job? = null

    // ===== Action 调用（SDK 端） =====

    suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val client = actionChannel ?: error("No ActionChannel configured")
        return client.call(action, params)
    }

    // ===== Event 接收（SDK 端） =====

    /** 事件流（SDK 端从 OneBot 实现接收事件） */
    val events: Flow<OneBotEvent>
        get() = eventChannel?.events ?: emptyFlow()

    // ===== Event 推送（服务端） =====

    /** 推送事件（服务端向 SDK 推送事件） */
    suspend fun pushEvent(event: OneBotEvent) {
        eventPushChannel?.pushEvent(event)
            ?: error("No EventPushChannel configured")
    }

    // ===== 生命周期 =====

    /** 启动所有通道（并行）。服务端模式下自动发送 lifecycle enable 事件并启动心跳。 */
    suspend fun start() {
        coroutineScope {
            val started = mutableSetOf<Any>()
            actionServerChannel?.let { channel -> if (started.add(channel)) launch { channel.start() } }
            actionChannel?.let { channel -> if (started.add(channel)) launch { channel.start() } }
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
            actionServerChannel?.let { launch { it.stop() } }
            actionChannel?.let { launch { it.stop() } }
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
}
