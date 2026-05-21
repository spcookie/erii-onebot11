package uesugi.onebot.lib.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import uesugi.onebot.lib.model.ActionRequest
import uesugi.onebot.lib.model.ActionResponse
import uesugi.onebot.lib.model.EventEnvelope

/**
 * 动作通道——发送 API 调用并接收响应。
 */
interface ActionChannel {
    /** 发送动作请求，等待并返回响应 */
    suspend fun send(request: ActionRequest): ActionResponse

    /** 注册动作处理器（服务端使用） */
    fun onRequest(handler: suspend (ActionRequest) -> ActionResponse)

    /** 启动通道 */
    suspend fun start()

    /** 停止通道 */
    suspend fun stop()
}

/**
 * 事件通道——推送或接收事件。
 */
interface EventChannel {
    /** 推送事件（实现端使用） */
    suspend fun push(event: EventEnvelope)

    /** 接收事件流（应用端使用） */
    val eventFlow: Flow<EventEnvelope>

    /** 启动通道 */
    suspend fun start()

    /** 停止通道 */
    suspend fun stop()
}

/**
 * 传输层配置
 */
data class TransportConfig(
    /** 主机地址 */
    val host: String = "127.0.0.1",
    /** 端口 */
    val port: Int = 5700,
    /** 访问令牌 */
    val accessToken: String? = null,
    /** HMAC-SHA1 密钥（HTTP POST 模式） */
    val secret: String? = null,
    /** 机器人 QQ 号 */
    val selfId: Long = 0,
    /** 心跳间隔（毫秒） */
    val heartbeatInterval: Long = 15000,
    /** 重连间隔（毫秒，WS 模式） */
    val reconnectInterval: Long = 3000,
    /** 请求超时（毫秒） */
    val timeoutMs: Long = 30000,
)
