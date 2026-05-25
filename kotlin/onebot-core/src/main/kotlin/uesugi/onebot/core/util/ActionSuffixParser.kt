package uesugi.onebot.core.util

/**
 * Action 后缀枚举。
 *
 * OneBot 规范定义了两个 Action 名称后缀：
 * - _async：异步调用，立即返回 async
 * - _rate_limited：限流调用，按配置间隔排队执行
 */
enum class ActionSuffix(val suffix: String) {
    ASYNC("_async"),
    RATE_LIMITED("_rate_limited")
}

/**
 * Action 后缀解析器。
 *
 * 从完整 Action 名称中分离基础名称和后缀。
 * 例如："send_msg_async" → ("send_msg", ActionSuffix.ASYNC)
 */
object ActionSuffixParser {

    /**
     * 解析 action 名称后缀。
     *
     * @return Pair(基础 action 名, 后缀) 或 Pair(原始名称, null)
     */
    fun parse(action: String): Pair<String, ActionSuffix?> {
        for (suffix in ActionSuffix.entries) {
            if (action.endsWith(suffix.suffix)) {
                val baseAction = action.removeSuffix(suffix.suffix)
                return baseAction to suffix
            }
        }
        return action to null
    }

}
