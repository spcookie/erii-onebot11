package uesugi.onebot.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import uesugi.onebot.core.model.AsyncActionResult
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import java.util.concurrent.atomic.AtomicReference

/**
 * Action 限流队列。
 *
 * 处理 `_rate_limited` 后缀的 Action：按配置的时间间隔依次执行队列中的任务。
 */
class RateLimiter(private val intervalMs: Long) {
    private data class Task(val params: OneBotActionParams)

    private val logger = LoggerFactory.getLogger(RateLimiter::class.java)
    private val queue = Channel<Task>(1000)
    private val jobRef = AtomicReference<Job?>(null)

    /**
     * 将任务加入限流队列，立即返回 [AsyncActionResult]。
     */
    fun enqueue(
        scope: CoroutineScope,
        action: String,
        params: OneBotActionParams,
        executor: suspend (String, OneBotActionParams) -> OneBotActionResult
    ): OneBotActionResult {
        while (true) {
            val current = jobRef.get()
            if (current?.isActive == true) break
            val newJob = scope.launch { consume(executor) }
            if (jobRef.compareAndSet(current, newJob)) break
            newJob.cancel()
        }

        val result = queue.trySend(Task(params))
        if (result.isFailure) {
            logger.warn("Rate limiter queue full, dropping action [{}]", action)
            return AsyncActionResult
        }
        return AsyncActionResult
    }

    private suspend fun consume(executor: suspend (String, OneBotActionParams) -> OneBotActionResult) {
        for (task in queue) {
            try {
                executor("", task.params)
            } catch (e: Exception) {
                logger.error("Rate-limited action failed", e)
            }
            if (intervalMs > 0) delay(intervalMs)
        }
    }

    suspend fun stop() {
        jobRef.get()?.cancel()
        queue.close()
    }
}
