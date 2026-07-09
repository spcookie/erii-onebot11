package uesugi.onebot.lib.transport.impl.ws

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.slf4j.Logger
import uesugi.onebot.core.config.OneBotConfig

/** WsForwardServer 专用 WS 鉴权。 */
internal suspend fun DefaultWebSocketServerSession.authenticateWs(config: OneBotConfig, logger: Logger): Boolean {
    if (config.accessToken.isNullOrBlank()) return true
    val auth = call.request.headers["Authorization"]
    if (auth == null) {
        logger.warn("WS auth failed: no token provided")
        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
        return false
    }
    if (auth != config.authHeader) {
        logger.warn("WS auth failed: token mismatch")
        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Forbidden"))
        return false
    }
    return true
}
