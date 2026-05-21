package uesugi.onebot.sdk.event

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.OneBotEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * 事件总线：类型化事件分发。
 */
class EventBus {
    private val logger = LoggerFactory.getLogger(EventBus::class.java)
    private val handlers = ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<EventHandler>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    data class EventHandler(
        val type: KClass<out OneBotEvent>,
        val priority: Int = 0,
        val handler: suspend (OneBotEvent) -> Unit,
    )

    /**
     * 注册事件处理器。
     */
    fun <T : OneBotEvent> on(type: KClass<T>, priority: Int = 0, handler: suspend (T) -> Unit) {
        val entry = EventHandler(type, priority) { event ->
            @Suppress("UNCHECKED_CAST")
            handler(event as T)
        }
        handlers.getOrPut(type) { CopyOnWriteArrayList() }.add(entry)
        handlers[type]!!.sortByDescending { it.priority }
    }

    /**
     * 取消注册。
     */
    fun <T : OneBotEvent> off(type: KClass<T>) {
        handlers.remove(type)
    }

    /**
     * 分发事件到匹配的处理器。
     */
    suspend fun dispatch(event: OneBotEvent) {
        val matched = mutableListOf<EventHandler>()

        // 精确匹配
        handlers[event::class]?.let { matched.addAll(it) }

        // 父类型匹配（遍历 sealed class 层级）
        for ((type, list) in handlers) {
            if (type != event::class && type.java.isAssignableFrom(event::class.java)) {
                matched.addAll(list)
            }
        }

        if (matched.isEmpty()) {
            logger.trace("No handler for event: {}", event::class.simpleName)
            return
        }

        logger.debug("Dispatching {} → {} handlers", event::class.simpleName, matched.size)

        // 异步分发，异常隔离
        for (entry in matched) {
            scope.launch {
                try {
                    entry.handler(event)
                } catch (e: Exception) {
                    logger.warn("Handler error for {}: {}", event::class.simpleName, e.message)
                }
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        handlers.clear()
    }
}
