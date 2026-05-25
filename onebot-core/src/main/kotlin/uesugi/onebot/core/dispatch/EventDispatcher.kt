package uesugi.onebot.core.dispatch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import uesugi.onebot.core.model.OneBotEvent

typealias EventHandler = suspend (OneBotEvent) -> Unit

/**
 * 事件处理器注册与分发。
 *
 * 按 post_type 分组注册处理器，支持通配（onAll）注册。
 * 分发时为每个处理器启动独立协程，错误隔离。
 */
class EventDispatcher {
    private val logger = LoggerFactory.getLogger(EventDispatcher::class.java)
    private val handlers = mutableMapOf<String, MutableList<EventHandler>>()
    private val wildcardHandlers = mutableListOf<EventHandler>()

    fun on(postType: String, handler: EventHandler) {
        handlers.getOrPut(postType) { mutableListOf() }.add(handler)
    }

    fun onAll(handler: EventHandler) {
        wildcardHandlers.add(handler)
    }

    /** 分发事件：为每个匹配的处理器启动独立协程 */
    fun dispatch(event: OneBotEvent, scope: CoroutineScope) {
        val matched = handlers[event.postType].orEmpty()
        val all = matched + wildcardHandlers

        all.forEach { handler ->
            scope.launch {
                try {
                    handler(event)
                } catch (e: Exception) {
                    logger.error("Event handler failed for post_type={}", event.postType, e)
                }
            }
        }
    }
}
