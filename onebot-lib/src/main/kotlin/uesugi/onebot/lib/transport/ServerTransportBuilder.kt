package uesugi.onebot.lib.transport

import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.transport.Connection
import uesugi.onebot.lib.transport.impl.http.HttpActionServer
import uesugi.onebot.lib.transport.impl.http.HttpEventClient
import uesugi.onebot.lib.transport.impl.ws.WsForwardServer
import uesugi.onebot.lib.transport.impl.ws.WsReverseActionClient
import uesugi.onebot.lib.transport.impl.ws.WsReverseEventClient
import uesugi.onebot.lib.transport.impl.ws.WsReverseUniversalClient

/**
 * 服务端传输构建器。
 *
 * 根据 [OneBotConfig] 组装正确的服务端通道组合到 [Connection] 中。
 */
class ServerTransportBuilder(
    private val config: OneBotConfig,
    private val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult
) {

    fun build(): Connection {
        val connection = Connection(config)

        if (config.httpEnable) {
            connection.actionServerChannel = HttpActionServer(config, actionHandler = actionHandler)
        }

        if (config.httpPostEnable && config.httpPostUrl != null) {
            connection.eventPushChannel = HttpEventClient(config)
        }

        if (config.wsForwardServerEnable) {
            connection.eventPushChannel = WsForwardServer(config, actionHandler, onConnect = {
                connection.pushEvent(
                    uesugi.onebot.core.model.LifecycleMetaEvent(
                        time = System.currentTimeMillis() / 1000,
                        selfId = config.selfId,
                        subType = "connect"
                    )
                )
            })
        }

        // 反向 WS 客户端（实现侧，OneBot 实现作为 WS 客户端连接 SDK 服务器）
        if (config.wsReverseClientEnable) {
            if (config.wsReverseClientUseUniversal) {
                val universal = WsReverseUniversalClient(config, actionHandler)
                connection.actionServerChannel = universal
                connection.eventPushChannel = universal
            } else {
                if (config.wsReverseClientApiUrl != null || config.wsReverseClientUrl != null) {
                    connection.actionServerChannel = WsReverseActionClient(config, actionHandler)
                }
                if (config.wsReverseClientEventUrl != null || config.wsReverseClientUrl != null) {
                    connection.eventPushChannel = WsReverseEventClient(config)
                }
            }
        }

        return connection
    }
}
