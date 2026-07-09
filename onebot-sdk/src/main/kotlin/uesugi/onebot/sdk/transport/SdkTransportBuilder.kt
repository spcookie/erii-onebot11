package uesugi.onebot.sdk.transport

import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.transport.Connection
import uesugi.onebot.core.util.EchoTracker
import uesugi.onebot.sdk.transport.impl.http.HttpActionClient
import uesugi.onebot.sdk.transport.impl.http.HttpEventServer
import uesugi.onebot.sdk.transport.impl.ws.WsForwardApiClient
import uesugi.onebot.sdk.transport.impl.ws.WsForwardEventClient
import uesugi.onebot.sdk.transport.impl.ws.WsForwardUniversalClient
import uesugi.onebot.sdk.transport.impl.ws.WsReverseServer

/**
 * SDK 端传输构建器。
 *
 * 根据 [OneBotConfig] 组装正确的 SDK 端通道组合到 [Connection] 中。
 */
class SdkTransportBuilder(private val config: OneBotConfig) {
    private val echoTracker = EchoTracker()

    fun build(): Connection {
        val connection = Connection(config)

        if (config.httpEnable) {
            connection.actionChannel = HttpActionClient(config)
        }

        if (config.httpPostEnable) {
            connection.eventChannel = HttpEventServer(config)
        }

        if (config.wsForwardClientEnable) {
            if (config.wsForwardClientUseUniversal) {
                val universal = WsForwardUniversalClient(config, echoTracker)
                connection.actionChannel = universal
                connection.eventChannel = universal
            } else {
                val apiClient = WsForwardApiClient(config, echoTracker)
                connection.actionChannel = apiClient
                val eventClient = WsForwardEventClient(config, actionHandler = { action, params ->
                    apiClient.call(action, params)
                })
                if (config.wsForwardClientEventUrl != null || config.wsForwardClientUrl != null) {
                    connection.eventChannel = eventClient
                }
            }
        }

        // 反向 WS 服务端（SDK 作为 WS 服务器，OneBot 实现连接过来）
        if (config.wsReverseServerEnable) {
            val server = WsReverseServer(config, echoTracker)
            if (connection.actionChannel == null) connection.actionChannel = server
            if (connection.eventChannel == null) connection.eventChannel = server
        }

        return connection
    }
}
