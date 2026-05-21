package uesugi.onebot.lib.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 消息发送者（私聊）
 */
@Serializable
data class PrivateSender(
    @SerialName("user_id") val userId: Long = 0,
    val nickname: String = "",
    val sex: String = "unknown",
    val age: Int = 0,
)

/**
 * 消息发送者（群聊）
 */
@Serializable
data class GroupSender(
    @SerialName("user_id") val userId: Long = 0,
    val nickname: String = "",
    val card: String = "",
    val sex: String = "unknown",
    val age: Int = 0,
    val area: String = "",
    val level: String = "",
    val role: String = "member",
    val title: String = "",
)

/**
 * 匿名信息
 */
@Serializable
data class Anonymous(
    val id: Long = 0,
    val name: String = "",
    val flag: String = "",
)

/**
 * 文件信息
 */
@Serializable
data class FileInfo(
    val id: String = "",
    val name: String = "",
    val size: Long = 0,
    val busid: Long = 0,
)

/**
 * 群荣誉信息
 */
@Serializable
data class HonorInfo(
    @SerialName("user_id") val userId: Long = 0,
    val nickname: String = "",
    val avatar: String = "",
    val description: String = "",
)

/**
 * 群信息
 */
@Serializable
data class GroupInfo(
    @SerialName("group_id") val groupId: Long,
    @SerialName("group_name") val groupName: String = "",
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_member_count") val maxMemberCount: Int = 0,
)

/**
 * 群成员信息
 */
@Serializable
data class GroupMemberInfo(
    @SerialName("group_id") val groupId: Long,
    @SerialName("user_id") val userId: Long,
    val nickname: String = "",
    val card: String = "",
    val sex: String = "unknown",
    val age: Int = 0,
    val area: String = "",
    @SerialName("join_time") val joinTime: Long = 0,
    @SerialName("last_sent_time") val lastSentTime: Long = 0,
    val level: String = "",
    val role: String = "member",
    val title: String = "",
    @SerialName("title_expire_time") val titleExpireTime: Long = 0,
    @SerialName("card_changeable") val cardChangeable: Boolean = true,
    @SerialName("unfriendly") val unfriendly: Boolean = false,
)

/**
 * 好友信息
 */
@Serializable
data class FriendInfo(
    @SerialName("user_id") val userId: Long = 0,
    val nickname: String = "",
    val remark: String = "",
)

/**
 * 陌生人信息
 */
@Serializable
data class StrangerInfo(
    @SerialName("user_id") val userId: Long,
    val nickname: String = "",
    val sex: String = "unknown",
    val age: Int = 0,
)

/**
 * 登录信息
 */
@Serializable
data class LoginInfo(
    @SerialName("user_id") val userId: Long = 0,
    val nickname: String = "",
)

/**
 * 版本信息
 */
@Serializable
data class VersionInfo(
    @SerialName("app_name") val appName: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("protocol_version") val protocolVersion: String = "v11",
)

/**
 * 运行状态
 */
@Serializable
data class StatusInfo(
    val online: Boolean = true,
    val good: Boolean = true,
)
