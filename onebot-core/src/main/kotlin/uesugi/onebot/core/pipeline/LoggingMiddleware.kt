package uesugi.onebot.core.pipeline

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.model.OneBotEvent
import uesugi.onebot.core.model.detailType

/**
 * 日志中间件。
 *
 * 记录每个 Action 请求/响应和 Event 接收。
 */
class LoggingMiddleware(
    private val logger: Logger = LoggerFactory.getLogger("onebot.middleware.logging"),
    private val name: String = "onebot"
) : Middleware {


    override suspend fun interceptAction(
        action: String,
        params: OneBotActionParams,
        next: ActionHandler
    ): OneBotActionResult {
        logger.info("[{}] Action: {}", name, action)
        val response = next(action, params)
        logger.info("[{}] Response: type={}", name, response::class.simpleName)
        return response
    }

    override suspend fun interceptEvent(event: OneBotEvent, next: EventHandler) {
        if (event.detailType != "heartbeat") {
            logger.info("[{}] Event: post_type={} detail_type={}", name, event.postType, event.detailType)
        }
        next(event)
    }
}
