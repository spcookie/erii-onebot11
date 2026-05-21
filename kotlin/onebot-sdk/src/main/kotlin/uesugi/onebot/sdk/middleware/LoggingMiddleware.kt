package uesugi.onebot.sdk.middleware

import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.Action
import uesugi.onebot.lib.model.ActionResponse
import uesugi.onebot.lib.model.OneBotEvent

/**
 * 日志中间件：记录所有动作和事件的日志。
 */
class LoggingMiddleware : Middleware {
    private val logger = LoggerFactory.getLogger(LoggingMiddleware::class.java)

    override suspend fun interceptAction(action: Action, next: suspend (Action) -> ActionResponse): ActionResponse {
        logger.debug("Action: {}", action.actionName)
        val response = next(action)
        logger.debug("Response: {} status={} retcode={}", action.actionName, response.status, response.retcode)
        return response
    }

    override suspend fun interceptEvent(event: OneBotEvent, next: suspend (OneBotEvent) -> Unit) {
        logger.debug("Event: post_type={}", event.postType)
        next(event)
    }
}

/**
 * 限流中间件：限制 send_message 类动作的频率。
 */
class RateLimitMiddleware(
    private val minIntervalMs: Long = 500,
) : Middleware {
    private var lastSendTime = 0L

    override suspend fun interceptAction(action: Action, next: suspend (Action) -> ActionResponse): ActionResponse {
        if (action.actionName.startsWith("send_")) {
            val elapsed = System.currentTimeMillis() - lastSendTime
            if (elapsed < minIntervalMs) {
                kotlinx.coroutines.delay(minIntervalMs - elapsed)
            }
            lastSendTime = System.currentTimeMillis()
        }
        return next(action)
    }
}

/**
 * 重试中间件：指定次数内重试失败的动作。
 */
class RetryMiddleware(
    private val maxRetries: Int = 3,
    private val delayMs: Long = 1000,
) : Middleware {
    private val logger = LoggerFactory.getLogger(RetryMiddleware::class.java)

    override suspend fun interceptAction(action: Action, next: suspend (Action) -> ActionResponse): ActionResponse {
        var lastResponse: ActionResponse? = null
        for (attempt in 0..maxRetries) {
            try {
                val response = next(action)
                if (response.isOk || attempt == maxRetries) {
                    return response
                }
                lastResponse = response
                logger.debug("Retrying action: {} (attempt {})", action.actionName, attempt + 1)
                kotlinx.coroutines.delay(delayMs)
            } catch (e: Exception) {
                if (attempt == maxRetries) throw e
                kotlinx.coroutines.delay(delayMs)
            }
        }
        return lastResponse!!
    }
}
