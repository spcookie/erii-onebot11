package uesugi.onebot.core.pipeline

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uesugi.onebot.core.dispatch.MiddlewareException
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult

/**
 * 限流中间件。
 *
 * 对 send_* 开头的 Action 进行速率限制。
 * 超过限制时抛出 [MiddlewareException] (retcode=103)。
 */
class RateLimitMiddleware(
    private val intervalMs: Long = 500L,
    private val maxBurst: Int = 10
) : Middleware {

    private var tokens = maxBurst.toDouble()
    private var lastRefillNanos = System.nanoTime()
    private val mutex = Mutex()

    override suspend fun interceptAction(
        action: String,
        params: OneBotActionParams,
        next: ActionHandler
    ): OneBotActionResult {
        if (!action.startsWith("send_")) {
            return next(action, params)
        }

        val allowed = mutex.withLock {
            refillTokens()
            if (tokens < 1.0) {
                false
            } else {
                tokens -= 1.0
                true
            }
        }

        if (!allowed) {
            throw MiddlewareException(103, "Rate limited")
        }
        return next(action, params)
    }

    private fun refillTokens() {
        val now = System.nanoTime()
        val elapsedMs = (now - lastRefillNanos) / 1_000_000.0
        tokens = (tokens + elapsedMs / intervalMs).coerceAtMost(maxBurst.toDouble())
        lastRefillNanos = now
    }
}
