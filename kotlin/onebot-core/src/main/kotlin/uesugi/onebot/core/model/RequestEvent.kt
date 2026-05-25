package uesugi.onebot.core.model

import kotlinx.serialization.Serializable

/**
 * 好友请求事件。
 * post_type = "request", request_type = "friend"
 */
@Serializable
data class FriendRequestEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "request",
    override val requestType: String = "friend",
    val userId: Long = 0,
    val comment: String = "",
    val flag: String = ""
) : RequestEvent

/**
 * 群请求/邀请事件。
 * post_type = "request", request_type = "group"
 * sub_type: add, invite
 */
@Serializable
data class GroupRequestEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "request",
    override val requestType: String = "group",
    val subType: String = "add",
    val groupId: Long = 0,
    val userId: Long = 0,
    val comment: String = "",
    val flag: String = ""
) : RequestEvent
