package uesugi.onebot.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull


@Serializable
sealed interface OneBotActionResult

@Serializable
data class RawActionResult(
    val raw: JsonElement = JsonNull
) : OneBotActionResult

/** 异步 Action 标记结果，transport 层遇到此类型应返回 [ActionResponse.async] */
@Serializable
data object AsyncActionResult : OneBotActionResult


// ===== 通用 =====

@Serializable
data class MessageIdResult(val messageId: Int) : OneBotActionResult

// ===== 消息 =====

@Serializable
data class MessageInfo(
    val time: Long = 0,
    val messageType: String = "",
    val messageId: Int = 0,
    val realId: Int = 0,
    val sender: Sender = Sender(0, ""),
    val message: MessageContent = emptyList()
) : OneBotActionResult

// ===== 登录信息 =====

@Serializable
data class LoginInfo(
    val userId: Long = 0,
    val nickname: String = ""
) : OneBotActionResult

// ===== 陌生人信息 =====

@Serializable
data class StrangerInfo(
    val userId: Long = 0,
    val nickname: String = "",
    val sex: String = "unknown",
    val age: Int = 0
) : OneBotActionResult

// ===== 好友信息 =====

@Serializable
data class FriendInfo(
    val userId: Long = 0,
    val nickname: String = "",
    val remark: String = ""
) : OneBotActionResult

// ===== 群信息 =====

@Serializable
data class GroupInfo(
    val groupId: Long = 0,
    val groupName: String = "",
    val memberCount: Int = 0,
    val maxMemberCount: Int = 0
) : OneBotActionResult

// ===== 群成员信息 =====

@Serializable
data class GroupMemberInfo(
    val groupId: Long = 0,
    val userId: Long = 0,
    val nickname: String = "",
    val card: String = "",
    val sex: String = "unknown",
    val age: Int = 0,
    val area: String = "",
    val joinTime: Long = 0,
    val lastSentTime: Long = 0,
    val level: String = "",
    val role: String = "member",
    val unfriendly: Boolean = false,
    val title: String = "",
    val titleExpireTime: Long = 0,
    val cardChangeable: Boolean = false
) : OneBotActionResult

// ===== 群荣誉信息 =====

@Serializable
data class GroupHonorInfo(
    val groupId: Long = 0,
    val currentTalkative: CurrentTalkative? = null,
    val talkativeList: List<HonorUserInfo> = emptyList(),
    val performerList: List<HonorUserInfo> = emptyList(),
    val legendList: List<HonorUserInfo> = emptyList(),
    val strongNewbieList: List<HonorUserInfo> = emptyList(),
    val emotionList: List<HonorUserInfo> = emptyList()
) : OneBotActionResult

// ===== 凭证 =====

@Serializable
data class CookiesInfo(val cookies: String = "") : OneBotActionResult

@Serializable
data class CsrfTokenInfo(val token: Int = 0) : OneBotActionResult

@Serializable
data class CredentialsInfo(
    val cookies: String = "",
    val csrfToken: Int = 0
) : OneBotActionResult

// ===== 媒体 =====

@Serializable
data class FilePathInfo(val file: String = "") : OneBotActionResult

// ===== 状态 =====

@Serializable
data class StatusInfo(
    val online: Boolean = true,
    val good: Boolean = true
) : OneBotActionResult

// ===== 版本信息 =====

@Serializable
data class VersionInfo(
    val appName: String = "",
    val appVersion: String = "",
    val protocolVersion: String = "v11"
) : OneBotActionResult

// ===== 能力检查 =====

@Serializable
data class CanSendResult(val yes: Boolean = false) : OneBotActionResult

// ===== 消息发送响应 =====
// 使用 MessageIdResponse（见顶部）
