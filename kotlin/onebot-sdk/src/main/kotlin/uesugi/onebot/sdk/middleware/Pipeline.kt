package uesugi.onebot.sdk.middleware

import uesugi.onebot.lib.model.Action
import uesugi.onebot.lib.model.ActionResponse
import uesugi.onebot.lib.model.OneBotEvent

/**
 * 中间件管道：通过函数包装实现链式调用。
 */
class MiddlewarePipeline {
    private val middlewares = mutableListOf<Middleware>()

    fun use(middleware: Middleware) {
        middlewares.add(middleware)
    }

    fun remove(middleware: Middleware) {
        middlewares.remove(middleware)
    }

    suspend fun executeAction(action: Action, next: suspend (Action) -> ActionResponse): ActionResponse {
        var handler: suspend (Action) -> ActionResponse = next
        // 逆序组装，先注册的先执行
        for (m in middlewares.reversed()) {
            val current = handler
            handler = { a -> m.interceptAction(a, current) }
        }
        return handler(action)
    }

    suspend fun processEvent(event: OneBotEvent, next: suspend (OneBotEvent) -> Unit) {
        var handler: suspend (OneBotEvent) -> Unit = next
        for (m in middlewares.reversed()) {
            val current = handler
            handler = { e -> m.interceptEvent(e, current) }
        }
        handler(event)
    }
}
