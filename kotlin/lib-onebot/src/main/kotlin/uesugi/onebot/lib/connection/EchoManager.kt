package uesugi.onebot.lib.connection

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.ActionResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Echo 管理器：负责 API 请求与响应的匹配。
 */
class EchoManager(
    private val timeoutMs: Long = 30000,
) {
    private val logger = LoggerFactory.getLogger(EchoManager::class.java)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<ActionResponse>>()
    private val counter = AtomicLong(0)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 生成唯一的 echo 值
     */
    fun generateEcho(): String {
        val ts = System.currentTimeMillis().toString(36)
        val seq = counter.incrementAndGet().toString(36)
        return "$ts-$seq"
    }

    /**
     * 注册 echo，返回对应的 Deferred。
     */
    suspend fun register(echo: String): CompletableDeferred<ActionResponse> {
        val deferred = CompletableDeferred<ActionResponse>(parent = scope.coroutineContext[Job])
        pending[echo] = deferred

        // 超时自动取消
        scope.launch {
            delay(timeoutMs)
            if (pending.remove(echo) != null && !deferred.isCompleted) {
                deferred.completeExceptionally(
                    CancellationException("Action timeout: $echo")
                )
                logger.debug("Echo timeout: {}", echo)
            }
        }

        return deferred
    }

    /**
     * 收到响应时匹配 echo。
     * @return true 如果成功匹配
     */
    fun resolve(echo: String, response: ActionResponse): Boolean {
        val deferred = pending.remove(echo)
        if (deferred != null && !deferred.isCompleted) {
            deferred.complete(response)
            logger.debug("Echo resolved: {}, status={}", echo, response.status)
            return true
        }
        logger.debug("Echo not found: {}", echo)
        return false
    }

    /**
     * 取消所有待处理的请求。
     */
    fun cancelAll(cause: Throwable = CancellationException("Connection closed")) {
        pending.values.forEach { deferred ->
            if (!deferred.isCompleted) {
                deferred.completeExceptionally(cause)
            }
        }
        pending.clear()
        logger.debug("All pending requests cancelled")
    }

    /**
     * 获取待处理请求数量
     */
    val pendingCount: Int get() = pending.size

    fun stop() {
        cancelAll()
        scope.cancel()
    }
}
