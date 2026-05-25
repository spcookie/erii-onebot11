package uesugi.onebot.core.model

import kotlinx.serialization.Serializable

/**
 * 私聊消息发送者。
 */
@Serializable
data class Sender(
    val userId: Long,
    val nickname: String,
    val sex: String = "unknown",
    val age: Int = 0
)

/**
 * 群消息发送者，相比私聊增加了群名片等群内信息。
 */
@Serializable
data class GroupSender(
    val userId: Long,
    val nickname: String,
    val card: String = "",
    val sex: String = "unknown",
    val age: Int = 0,
    val area: String = "",
    val level: String = "",
    val role: String = "member",
    val title: String = ""
)

/**
 * 匿名用户信息（用于群匿名消息）。
 */
@Serializable
data class Anonymous(
    val id: Long,
    val name: String,
    val flag: String
)

/**
 * 群文件信息（用于 group_upload 事件）。
 */
@Serializable
data class FileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val busid: Long
)

/**
 * 群荣誉用户信息。
 */
@Serializable
data class HonorUserInfo(
    val userId: Long,
    val nickname: String,
    val avatar: String = "",
    val description: String = ""
)

/**
 * 当前群龙王信息。
 */
@Serializable
data class CurrentTalkative(
    val userId: Long,
    val nickname: String,
    val avatar: String = "",
    val dayCount: Int = 0
)
