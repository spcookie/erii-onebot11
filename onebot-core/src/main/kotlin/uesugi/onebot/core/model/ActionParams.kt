package uesugi.onebot.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@Serializable
sealed interface OneBotActionParams

@Serializable
data class RawActionParams(
    val raw: JsonObject = JsonObject(emptyMap())
) : OneBotActionParams

@Serializable
object EmptyActionParams : OneBotActionParams

// ===== 消息相关 =====

@Serializable
data class SendPrivateMsgRequest(
    val userId: Long,
    val message: MessageContent = emptyList(),
    val autoEscape: Boolean = false
) : OneBotActionParams

@Serializable
data class SendGroupMsgRequest(
    val groupId: Long,
    val message: MessageContent = emptyList(),
    val autoEscape: Boolean = false
) : OneBotActionParams

@Serializable
data class SendMsgRequest(
    val messageType: String,
    val userId: Long? = null,
    val groupId: Long? = null,
    val message: MessageContent = emptyList(),
    val autoEscape: Boolean = false
) : OneBotActionParams

@Serializable
data class DeleteMsgRequest(val messageId: Int) : OneBotActionParams

@Serializable
data class GetMsgRequest(val messageId: Int) : OneBotActionParams

@Serializable
data class GetForwardMsgRequest(val id: String) : OneBotActionParams

// ===== 点赞 =====

@Serializable
data class SendLikeRequest(
    val userId: Long,
    val times: Int = 1
) : OneBotActionParams

// ===== 群管理 =====

@Serializable
data class SetGroupKickRequest(
    val groupId: Long,
    val userId: Long,
    val rejectAddRequest: Boolean = false
) : OneBotActionParams

@Serializable
data class SetGroupBanRequest(
    val groupId: Long,
    val userId: Long,
    val duration: Long = 30 * 60
) : OneBotActionParams

@Serializable
data class SetGroupAnonymousBanRequest(
    val groupId: Long,
    val anonymous: Anonymous? = null,
    val anonymousFlag: String? = null,
    val flag: String? = null,
    val duration: Long = 30 * 60
) : OneBotActionParams

@Serializable
data class SetGroupWholeBanRequest(
    val groupId: Long,
    val enable: Boolean = true
) : OneBotActionParams

@Serializable
data class SetGroupAdminRequest(
    val groupId: Long,
    val userId: Long,
    val enable: Boolean = true
) : OneBotActionParams

@Serializable
data class SetGroupAnonymousRequest(
    val groupId: Long,
    val enable: Boolean = true
) : OneBotActionParams

@Serializable
data class SetGroupCardRequest(
    val groupId: Long,
    val userId: Long,
    val card: String = ""
) : OneBotActionParams

@Serializable
data class SetGroupNameRequest(
    val groupId: Long,
    val groupName: String
) : OneBotActionParams

@Serializable
data class SetGroupLeaveRequest(
    val groupId: Long,
    val isDismiss: Boolean = false
) : OneBotActionParams

@Serializable
data class SetGroupSpecialTitleRequest(
    val groupId: Long,
    val userId: Long,
    val specialTitle: String,
    val duration: Long = -1
) : OneBotActionParams

// ===== 请求处理 =====

@Serializable
data class SetFriendAddRequestRequest(
    val flag: String,
    val approve: Boolean = true,
    val remark: String = ""
) : OneBotActionParams

@Serializable
data class SetGroupAddRequestRequest(
    val flag: String,
    val subType: String = "add",
    val type: String? = null,
    val approve: Boolean = true,
    val reason: String = ""
) : OneBotActionParams

// ===== 信息获取 =====

@Serializable
data class GetStrangerInfoRequest(
    val userId: Long,
    val noCache: Boolean = false
) : OneBotActionParams

@Serializable
data class GetGroupInfoRequest(
    val groupId: Long,
    val noCache: Boolean = false
) : OneBotActionParams

@Serializable
data class GetGroupMemberInfoRequest(
    val groupId: Long,
    val userId: Long,
    val noCache: Boolean = false
) : OneBotActionParams

@Serializable
data class GetGroupMemberListRequest(val groupId: Long) : OneBotActionParams

@Serializable
data class GetGroupHonorInfoRequest(
    val groupId: Long,
    val type: String = "all"
) : OneBotActionParams

// ===== 凭证获取 =====

@Serializable
data class GetCookiesRequest(val domain: String = "") : OneBotActionParams

@Serializable
data class GetCredentialsRequest(val domain: String = "") : OneBotActionParams

// ===== 媒体 =====

@Serializable
data class GetRecordRequest(
    val file: String,
    val outFormat: String = "mp3"
) : OneBotActionParams

@Serializable
data class GetImageRequest(val file: String) : OneBotActionParams

// ===== 系统 =====

@Serializable
data class SetRestartRequest(val delay: Long = 0) : OneBotActionParams

@Serializable
data class HandleQuickOperationRequest(
    val context: JsonObject,
    val operation: JsonObject
) : OneBotActionParams
