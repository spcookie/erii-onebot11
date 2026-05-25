package uesugi.onebot.core.model

import kotlinx.serialization.Serializable

// ===== 群文件上传 =====

@Serializable
data class GroupUploadEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "group_upload",
    val groupId: Long = 0,
    val userId: Long = 0,
    val file: FileInfo = FileInfo("", "", 0, 0)
) : NoticeEvent

// ===== 群管理员变更 =====

@Serializable
data class GroupAdminEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "group_admin",
    val subType: String = "set",
    val groupId: Long = 0,
    val userId: Long = 0
) : NoticeEvent

// ===== 群成员减少 =====

@Serializable
data class GroupDecreaseEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "group_decrease",
    val subType: String = "leave",
    val groupId: Long = 0,
    val operatorId: Long = 0,
    val userId: Long = 0
) : NoticeEvent

// ===== 群成员增加 =====

@Serializable
data class GroupIncreaseEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "group_increase",
    val subType: String = "approve",
    val groupId: Long = 0,
    val operatorId: Long = 0,
    val userId: Long = 0
) : NoticeEvent

// ===== 群禁言 =====

@Serializable
data class GroupBanEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "group_ban",
    val subType: String = "ban",
    val groupId: Long = 0,
    val operatorId: Long = 0,
    val userId: Long = 0,
    val duration: Long = 0
) : NoticeEvent

// ===== 好友添加 =====

@Serializable
data class FriendAddEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "friend_add",
    val userId: Long = 0
) : NoticeEvent

// ===== 群消息撤回 =====

@Serializable
data class GroupRecallEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "group_recall",
    val groupId: Long = 0,
    val userId: Long = 0,
    val operatorId: Long = 0,
    val messageId: Int = 0
) : NoticeEvent

// ===== 好友消息撤回 =====

@Serializable
data class FriendRecallEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "friend_recall",
    val userId: Long = 0,
    val messageId: Int = 0
) : NoticeEvent

// ===== 群内戳一戳 =====

@Serializable
data class PokeEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "notify",
    val subType: String = "poke",
    val groupId: Long = 0,
    val userId: Long = 0,
    val targetId: Long = 0
) : NoticeEvent

// ===== 群红包运气王 =====

@Serializable
data class LuckyKingEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "notify",
    val subType: String = "lucky_king",
    val groupId: Long = 0,
    val userId: Long = 0,
    val targetId: Long = 0
) : NoticeEvent

// ===== 群成员荣誉变更 =====

@Serializable
data class HonorEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "notice",
    override val noticeType: String = "notify",
    val subType: String = "honor",
    val groupId: Long = 0,
    val userId: Long = 0,
    val honorType: String = ""
) : NoticeEvent
