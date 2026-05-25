package uesugi.onebot.core.dispatch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import uesugi.onebot.core.model.AsyncActionResult
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.model.RawActionParams
import uesugi.onebot.core.util.ActionSuffix
import uesugi.onebot.core.util.ActionSuffixParser
import uesugi.onebot.core.util.RateLimiter

/**
 * Action 分发器。
 *
 * 管理 Action handler 注册表，处理 _async / _rate_limited 后缀语义，
 * 最终分发给对应 handler。
 *
 * 当 action 未注册时抛出 [ActionNotFoundException]，
 * 由 transport 层映射为协议对应的错误响应（HTTP → 404, WS → retcode 1404）。
 *
 * 支持两种 handler 注册方式：
 * - [onAction] raw 模式：handler 接收 JsonObject，自行解析参数
 * - [onAction] typed 模式：handler 接收强类型的请求对象
 */
class ActionDispatcher(rateLimitInterval: Long = 0) {
    private val rawHandlers = mutableMapOf<String, suspend (JsonObject) -> OneBotActionResult>()

    @PublishedApi
    internal val typedHandlers = mutableMapOf<String, suspend (OneBotActionParams) -> OneBotActionResult>()
    private val rateLimiter = RateLimiter(rateLimitInterval)
    private var scope: CoroutineScope? = null

    /** 注册 raw handler（接收 JsonObject） */
    @JvmName("onActionRaw")
    fun onAction(action: String, handler: suspend (JsonObject) -> OneBotActionResult) {
        rawHandlers[action] = handler
    }

    /** 注册类型安全的 handler（接收反序列化后的请求对象） */
    @JvmName("onActionTyped")
    inline fun <reified T : OneBotActionParams> onAction(
        action: String,
        noinline handler: suspend (T) -> OneBotActionResult
    ) {
        typedHandlers[action] = { params -> handler(params as T) }
    }

    fun setScope(scope: CoroutineScope) {
        this.scope = scope
    }

    /**
     * 分发 action 到注册的 handler。
     *
     * 处理 _async / _rate_limited 后缀语义后返回 [OneBotActionResult]。
     * 未注册的 action 抛出 [ActionNotFoundException]。
     */
    suspend fun dispatch(action: String, params: OneBotActionParams): OneBotActionResult {
        val s = scope ?: error("Scope not set. Call setScope() before dispatch.")
        val (baseAction, suffix) = ActionSuffixParser.parse(action)

        return when (suffix) {
            ActionSuffix.ASYNC -> {
                s.launch { invoke(baseAction, params) }
                AsyncActionResult
            }

            ActionSuffix.RATE_LIMITED -> {
                rateLimiter.enqueue(s, baseAction, params) { _, p -> invoke(baseAction, p) }
            }

            null -> invoke(baseAction, params)
        }
    }

    private suspend fun invoke(action: String, params: OneBotActionParams): OneBotActionResult {
        val typed = typedHandlers[action]
        if (typed != null) return typed(params)
        val raw = rawHandlers[action] ?: throw ActionNotFoundException(action)
        val jsonParams = if (params is RawActionParams) params.raw else JsonObject(emptyMap())
        return raw(jsonParams)
    }

    suspend fun stop() {
        rateLimiter.stop()
    }
}
