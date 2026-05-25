package uesugi.onebot.core.pipeline

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
class LoggingMiddleware(private val name: String = "onebot") : Middleware {
    private val logger = LoggerFactory.getLogger("onebot.middleware.logging")

    override suspend fun interceptAction(
        action: String,
        params: OneBotActionParams,
        next: ActionHandler
    ): OneBotActionResult {
        logger.debug("[{}] Action: {}", name, action)
        val response = next(action, params)
        logger.debug("[{}] Response: type={}", name, response::class.simpleName)
        return response
    }

    override suspend fun interceptEvent(event: OneBotEvent, next: EventHandler) {
        logger.debug("[{}] Event: post_type={} detailType={}", name, event.postType, event.detailType)
        next(event)
    }
}
