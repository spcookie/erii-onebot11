package uesugi.onebot.core.util

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uesugi.onebot.core.model.ActionResponse
import java.util.concurrent.atomic.AtomicLong

/**
 * WebSocket 模式下的请求/响应匹配器。
 *
 * 每个 Action 请求通过唯一 echo ID 与对应的 ActionResponse 关联。
 * HTTP 模式下不需要（同步调用直接返回响应），WS 模式下使用。
 *
 * 线程安全：使用 Mutex 保护 pending map 的并发访问。
 */
class EchoTracker {
    private val counter = AtomicLong(0)
    private val pending = mutableMapOf<String, Pair<CompletableDeferred<ActionResponse>, Job?>>()
    private val mutex = Mutex()
    private var scope: CoroutineScope? = null

    fun generateEcho(): String = "echo-${counter.incrementAndGet()}-${System.currentTimeMillis()}"

    suspend fun register(echo: String, timeoutMs: Long): CompletableDeferred<ActionResponse> {
        val deferred = CompletableDeferred<ActionResponse>()
        val timeoutJob = scope?.launch {
            try {
                delay(timeoutMs)
                mutex.withLock {
                    pending[echo]?.let { (d, _) ->
                        if (!d.isCompleted) d.complete(ActionResponse.failed(-1, echo))
                    }
                    pending.remove(echo)
                }
            } catch (_: CancellationException) {
                mutex.withLock { pending.remove(echo) }
            }
        }
        mutex.withLock { pending[echo] = Pair(deferred, timeoutJob) }
        return deferred
    }

    suspend fun resolve(echo: String, response: ActionResponse): Boolean {
        val entry = mutex.withLock { pending.remove(echo) } ?: return false
        entry.second?.cancel()  // 取消超时协程
        entry.first.complete(response)
        return true
    }

    suspend fun cancelAll() {
        mutex.withLock {
            pending.values.forEach { (deferred, job) ->
                job?.cancel()
                deferred.cancel()
            }
            pending.clear()
        }
    }

    fun setScope(scope: CoroutineScope) {
        this.scope = scope
    }

    suspend fun pendingCount(): Int = mutex.withLock { pending.size }
}
