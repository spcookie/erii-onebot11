package uesugi.onebot.sdk.middleware

import uesugi.onebot.lib.model.Action
import uesugi.onebot.lib.model.ActionResponse
import uesugi.onebot.lib.model.OneBotEvent

/**
 * 中间件接口：可拦截动作执行和事件处理。
 */
interface Middleware {
    suspend fun interceptAction(action: Action, next: suspend (Action) -> ActionResponse): ActionResponse {
        return next(action)
    }

    suspend fun interceptEvent(event: OneBotEvent, next: suspend (OneBotEvent) -> Unit) {
        next(event)
    }
}
