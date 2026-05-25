package uesugi.onebot.core.transport

import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult

/**
 * Action 通道接口。
 *
 * 抽象对 OneBot 实现的 API 调用通道，不同传输协议对此接口有不同实现：
 * - HTTP: 同步 POST，直接返回响应
 * - WS: 异步发送消息帧，通过 EchoTracker 匹配响应
 */
interface ActionChannel {
    /**
     * 调用 OneBot API。
     *
     * @param action API 名称（如 send_private_msg）
     * @param params 请求参数
     * @return API 响应
     */
    suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult

    /** 启动通道 */
    suspend fun start()

    /** 停止通道 */
    suspend fun stop()
}

/**
 * Action 服务通道接口（服务端/实现侧使用）。
 *
 * OneBot 实现通过此通道接收并处理来自 SDK 的 API 调用。
 * 对标 [EventChannel]，暴露 [actionHandler] 属性作为核心数据能力。
 */
interface ActionServerChannel {
    /** Action 处理器 */
    val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult

    /** 启动通道 */
    suspend fun start()

    /** 停止通道 */
    suspend fun stop()
}
