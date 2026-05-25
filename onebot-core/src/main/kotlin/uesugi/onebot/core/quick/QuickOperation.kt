package uesugi.onebot.core.model

import kotlinx.serialization.Serializable

/**
 * OneBot 11 快速操作响应。
 *
 * 事件上报后端在 HTTP 响应中返回此对象，
 * OneBot 实现解析后自动执行对应的操作（快速回复、撤回、踢人等）。
 *
 * 所有字段均为可选，只有显式提供的字段才会触发相应操作。
 * 按 OneBot 11 规范，wire format 使用 snake_case。
 */
@Serializable
data class QuickOperation(
    val reply: MessageContent = emptyList(),
    val autoEscape: Boolean = false,
    val atSender: Boolean = true,
    val delete: Boolean = false,
    val kick: Boolean = false,
    val ban: Boolean = false,
    val banDuration: Long = 30 * 60,
    val approve: Boolean? = null,
    val remark: String? = null,
    val reason: String? = null
)
