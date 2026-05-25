package uesugi.onebot.core.pipeline

import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.model.OneBotEvent

typealias ActionHandler = suspend (String, OneBotActionParams) -> OneBotActionResult
typealias EventHandler = suspend (OneBotEvent) -> Unit

typealias ActionMiddleware = suspend (String, OneBotActionParams, next: ActionHandler) -> OneBotActionResult
typealias EventMiddleware = suspend (OneBotEvent, next: EventHandler) -> Unit

/**
 * 中间件接口。
 *
 * 实现此接口的中间件可以同时拦截 Action 和 Event。
 * Action 中间件可通过抛出 [uesugi.onebot.core.dispatch.MiddlewareException] 来短路并返回错误 retcode。
 */
interface Middleware {
    suspend fun interceptAction(action: String, params: OneBotActionParams, next: ActionHandler): OneBotActionResult =
        next(action, params)

    suspend fun interceptEvent(event: OneBotEvent, next: EventHandler) {
        next(event)
    }
}

/**
 * 中间件管道（洋葱模式）。
 *
 * 按注册顺序包装处理器：
 * - 第一个注册的中间件是最外层
 * - 处理器本身在最内层
 */
class Pipeline {
    private val actionMiddlewares = mutableListOf<ActionMiddleware>()
    private val eventMiddlewares = mutableListOf<EventMiddleware>()

    fun useAction(mw: ActionMiddleware) {
        actionMiddlewares.add(mw)
    }

    fun useEvent(mw: EventMiddleware) {
        eventMiddlewares.add(mw)
    }

    fun use(mw: Middleware) {
        useAction { action, params, next -> mw.interceptAction(action, params, next) }
        useEvent { event, next -> mw.interceptEvent(event, next) }
    }

    /** 用 Action 中间件链包装处理器（洋葱模式） */
    fun wrapAction(handler: ActionHandler): ActionHandler {
        return actionMiddlewares.reversed().fold(handler) { next, mw ->
            { action, params -> mw(action, params, next) }
        }
    }

    /** 用 Event 中间件链包装处理器（洋葱模式） */
    fun wrapEvent(handler: EventHandler): EventHandler {
        return eventMiddlewares.reversed().fold(handler) { next, mw ->
            { event -> mw(event, next) }
        }
    }
}
