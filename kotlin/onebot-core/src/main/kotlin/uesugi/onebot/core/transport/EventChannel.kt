package uesugi.onebot.core.transport

import kotlinx.coroutines.flow.Flow
import uesugi.onebot.core.model.OneBotEvent

/**
 * 事件接收通道接口（SDK/客户端侧使用）。
 *
 * 客户端通过此通道接收来自 OneBot 实现推送的事件。
 */
interface EventChannel {
    /** 事件流 */
    val events: Flow<OneBotEvent>

    /** 启动通道 */
    suspend fun start()

    /** 停止通道 */
    suspend fun stop()
}

/**
 * 事件推送通道接口（服务端实现侧使用）。
 *
 * OneBot 实现通过此通道向外部推送事件。
 */
interface EventPushChannel {
    /**
     * 推送一个事件。
     *
     * @param event 事件对象
     */
    suspend fun pushEvent(event: OneBotEvent)

    /** 启动通道 */
    suspend fun start()

    /** 停止通道 */
    suspend fun stop()
}
