package uesugi.onebot.core.transport.impl.ws

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.Logger
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.ActionNotFoundException
import uesugi.onebot.core.dispatch.DispatchException
import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser

// ===== Action 处理 =====

/** WsForwardServer / WsReverseActionClient / WsReverseUniversalClient 共用。 */
internal suspend fun handleActionRequest(
    request: ActionRequest,
    actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
    paramParser: ActionParamParser,
    resultParser: ActionResultParser,
    logger: Logger,
): ActionResponse = try {
    when (val result = actionHandler(request.action, paramParser.deserialize(request.action, request.params))) {
        is AsyncActionResult -> ActionResponse.async(request.echo)
        else -> {
            val data = resultParser.serialize(request.action, result)
            ActionResponse.ok(data, request.echo)
        }
    }
} catch (e: DispatchException) {
    when (e) {
        is ActionNotFoundException -> {
            logger.debug("Action not found: {}", request.action)
            ActionResponse.notFound(request.echo)
        }

        else -> {
            logger.warn("Dispatch error for action {}: retcode={}", request.action, e.retcode)
            ActionResponse.failed(e.retcode, request.echo)
        }
    }
} catch (e: Exception) {
    logger.warn("Failed to handle action {}", request.action, e)
    ActionResponse.badRequest(request.echo)
}

// ===== WS 鉴权 =====

/** WsForwardServer / WsReverseServer 共用。 */
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

// ===== QuickOp 处理 =====

/** WsForwardEventClient / WsForwardUniversalClient / WsReverseServer 共用。 */
internal fun createQuickOpHandler(
    event: OneBotEvent,
    baseJson: Json,
    callAction: suspend (String, OneBotActionParams) -> OneBotActionResult,
    logger: Logger,
): QuickOpHandler = QuickOpHandler { op ->
    try {
        val context = baseJson.encodeToJsonElement(OneBotEvent.serializer(), event).jsonObject
        val operationJson = baseJson.encodeToJsonElement(QuickOperation.serializer(), op).jsonObject
        val params = JsonObject(mapOf("context" to context, "operation" to operationJson))
        callAction(ActionName.HANDLE_QUICK_OPERATION, RawActionParams(params))
    } catch (e: Exception) {
        logger.error("Quick operation handler failed", e)
    }
}
