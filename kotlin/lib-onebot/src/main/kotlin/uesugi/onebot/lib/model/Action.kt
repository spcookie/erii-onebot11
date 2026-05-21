package uesugi.onebot.lib.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray

/**
 * 所有 API 动作的基类接口。
 * 每个动作都知道自己的动作名和能序列化成 params JSON。
 */
interface Action {
    val actionName: String
    fun toParams(): JsonObject
}

// ==================== 消息发送 ====================

data class SendPrivateMsg(
    val userId: Long,
    val message: MessageContent,
    val autoEscape: Boolean = false,
) : Action {
    override val actionName = "send_private_msg"
    override fun toParams() = buildJsonObject {
        put("user_id", userId)
        put("message", JsonArray(message.map { segmentToJson(it) }))
        put("auto_escape", autoEscape)
    }
}

data class SendGroupMsg(
    val groupId: Long,
    val message: MessageContent,
    val autoEscape: Boolean = false,
) : Action {
    override val actionName = "send_group_msg"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("message", JsonArray(message.map { segmentToJson(it) }))
        put("auto_escape", autoEscape)
    }
}

data class SendMsg(
    val messageType: String? = null,
    val userId: Long? = null,
    val groupId: Long? = null,
    val message: MessageContent,
    val autoEscape: Boolean = false,
) : Action {
    override val actionName = "send_msg"
    override fun toParams() = buildJsonObject {
        messageType?.let { put("message_type", it) }
        userId?.let { put("user_id", it) }
        groupId?.let { put("group_id", it) }
        put("message", JsonArray(message.map { segmentToJson(it) }))
        put("auto_escape", autoEscape)
    }
}

data class DeleteMsg(val messageId: Long) : Action {
    override val actionName = "delete_msg"
    override fun toParams() = buildJsonObject { put("message_id", messageId) }
}

data class GetMsg(val messageId: Long) : Action {
    override val actionName = "get_msg"
    override fun toParams() = buildJsonObject { put("message_id", messageId) }
}

data class GetForwardMsg(val id: String) : Action {
    override val actionName = "get_forward_msg"
    override fun toParams() = buildJsonObject { put("id", id) }
}

data class SendLike(val userId: Long, val times: Int = 1) : Action {
    override val actionName = "send_like"
    override fun toParams() = buildJsonObject {
        put("user_id", userId)
        put("times", times.coerceIn(0, 10))
    }
}

// ==================== 群管理 ====================

data class SetGroupKick(
    val groupId: Long,
    val userId: Long,
    val rejectAddRequest: Boolean = false,
) : Action {
    override val actionName = "set_group_kick"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("user_id", userId)
        put("reject_add_request", rejectAddRequest)
    }
}

data class SetGroupBan(
    val groupId: Long,
    val userId: Long,
    val duration: Long = 1800,
) : Action {
    override val actionName = "set_group_ban"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("user_id", userId)
        put("duration", duration)
    }
}

data class SetGroupAnonymousBan(
    val groupId: Long,
    val anonymousFlag: String? = null,
    val duration: Long = 1800,
) : Action {
    override val actionName = "set_group_anonymous_ban"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        if (anonymousFlag != null) put("anonymous_flag", anonymousFlag)
        put("duration", duration)
    }
}

data class SetGroupWholeBan(val groupId: Long, val enable: Boolean = true) : Action {
    override val actionName = "set_group_whole_ban"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("enable", enable)
    }
}

data class SetGroupAdmin(val groupId: Long, val userId: Long, val enable: Boolean = true) : Action {
    override val actionName = "set_group_admin"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("user_id", userId)
        put("enable", enable)
    }
}

data class SetGroupAnonymous(val groupId: Long, val enable: Boolean = true) : Action {
    override val actionName = "set_group_anonymous"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("enable", enable)
    }
}

data class SetGroupCard(val groupId: Long, val userId: Long, val card: String = "") : Action {
    override val actionName = "set_group_card"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("user_id", userId)
        put("card", card)
    }
}

data class SetGroupName(val groupId: Long, val groupName: String) : Action {
    override val actionName = "set_group_name"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("group_name", groupName)
    }
}

data class SetGroupLeave(val groupId: Long, val isDismiss: Boolean = false) : Action {
    override val actionName = "set_group_leave"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("is_dismiss", isDismiss)
    }
}

data class SetGroupSpecialTitle(
    val groupId: Long,
    val userId: Long,
    val specialTitle: String = "",
    val duration: Long = -1,
) : Action {
    override val actionName = "set_group_special_title"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("user_id", userId)
        put("special_title", specialTitle)
        put("duration", duration)
    }
}

// ==================== 请求处理 ====================

data class SetFriendAddRequest(
    val flag: String,
    val approve: Boolean = true,
    val remark: String = "",
) : Action {
    override val actionName = "set_friend_add_request"
    override fun toParams() = buildJsonObject {
        put("flag", flag)
        put("approve", approve)
        put("remark", remark)
    }
}

data class SetGroupAddRequest(
    val flag: String,
    val subType: String,
    val approve: Boolean = true,
    val reason: String = "",
) : Action {
    override val actionName = "set_group_add_request"
    override fun toParams() = buildJsonObject {
        put("flag", flag)
        put("sub_type", subType)
        put("approve", approve)
        put("reason", reason)
    }
}

// ==================== 信息查询 ====================

object GetLoginInfo : Action {
    override val actionName = "get_login_info"
    override fun toParams() = emptyJsonParams
}

data class GetStrangerInfo(val userId: Long, val noCache: Boolean = false) : Action {
    override val actionName = "get_stranger_info"
    override fun toParams() = buildJsonObject {
        put("user_id", userId)
        put("no_cache", noCache)
    }
}

object GetFriendList : Action {
    override val actionName = "get_friend_list"
    override fun toParams() = emptyJsonParams
}

data class GetGroupInfo(val groupId: Long, val noCache: Boolean = false) : Action {
    override val actionName = "get_group_info"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("no_cache", noCache)
    }
}

object GetGroupList : Action {
    override val actionName = "get_group_list"
    override fun toParams() = emptyJsonParams
}

data class GetGroupMemberInfo(val groupId: Long, val userId: Long, val noCache: Boolean = false) : Action {
    override val actionName = "get_group_member_info"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("user_id", userId)
        put("no_cache", noCache)
    }
}

data class GetGroupMemberList(val groupId: Long) : Action {
    override val actionName = "get_group_member_list"
    override fun toParams() = buildJsonObject { put("group_id", groupId) }
}

data class GetGroupHonorInfo(val groupId: Long, val type: String = "all") : Action {
    override val actionName = "get_group_honor_info"
    override fun toParams() = buildJsonObject {
        put("group_id", groupId)
        put("type", type)
    }
}

// ==================== 系统/工具 ====================

data class GetCookies(val domain: String = "") : Action {
    override val actionName = "get_cookies"
    override fun toParams() = buildJsonObject { put("domain", domain) }
}

object GetCsrfToken : Action {
    override val actionName = "get_csrf_token"
    override fun toParams() = emptyJsonParams
}

data class GetCredentials(val domain: String = "") : Action {
    override val actionName = "get_credentials"
    override fun toParams() = buildJsonObject { put("domain", domain) }
}

data class GetRecord(val file: String, val outFormat: String = "mp3") : Action {
    override val actionName = "get_record"
    override fun toParams() = buildJsonObject {
        put("file", file)
        put("out_format", outFormat)
    }
}

data class GetImage(val file: String) : Action {
    override val actionName = "get_image"
    override fun toParams() = buildJsonObject { put("file", file) }
}

object CanSendImage : Action {
    override val actionName = "can_send_image"
    override fun toParams() = emptyJsonParams
}

object CanSendRecord : Action {
    override val actionName = "can_send_record"
    override fun toParams() = emptyJsonParams
}

object GetStatus : Action {
    override val actionName = "get_status"
    override fun toParams() = emptyJsonParams
}

object GetVersionInfo : Action {
    override val actionName = "get_version_info"
    override fun toParams() = emptyJsonParams
}

data class SetRestart(val delay: Long = 0) : Action {
    override val actionName = "set_restart"
    override fun toParams() = buildJsonObject { put("delay", delay) }
}

object CleanCache : Action {
    override val actionName = "clean_cache"
    override fun toParams() = emptyJsonParams
}

// ==================== 隐藏 API ====================

data class HandleQuickOperation(
    val context: JsonObject,
    val operation: JsonObject,
) : Action {
    override val actionName = ".handle_quick_operation"
    override fun toParams() = buildJsonObject {
        put("context", context)
        put("operation", operation)
    }
}

// ==================== 工具函数 ====================

internal val emptyJsonParams = JsonObject(emptyMap())

/**
 * 将 MessageSegment 转换为 JSON 对象，用于序列化到 params 中。
 */
internal fun segmentToJson(segment: MessageSegment): JsonElement {
    val map = buildJsonObject {
        put("type", segment.type)
        val dataJson = buildJsonObject {
            for ((k, v) in segment.data) {
                put(k, v)
            }
        }
        put("data", dataJson)
    }
    return map
}
