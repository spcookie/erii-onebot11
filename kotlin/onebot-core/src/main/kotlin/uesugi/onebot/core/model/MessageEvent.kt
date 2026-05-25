package uesugi.onebot.core.model

import kotlinx.serialization.Serializable

/**
 * 私聊消息事件。
 * post_type = "message", message_type = "private"
 * sub_type: friend, group（临时会话）, other
 */
@Serializable
data class PrivateMessageEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "message",
    override val messageType: String = "private",
    override val subType: String = "friend",
    val messageId: Int = 0,
    val userId: Long = 0,
    val message: MessageContent = emptyList(),
    val rawMessage: String = "",
    val font: Int = 0,
    val sender: Sender = Sender(0, "")
) : MessageEvent

/**
 * 群聊消息事件。
 * post_type = "message", message_type = "group"
 * sub_type: normal, anonymous, notice
 */
@Serializable
data class GroupMessageEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "message",
    override val messageType: String = "group",
    override val subType: String = "normal",
    val messageId: Int = 0,
    val groupId: Long = 0,
    val userId: Long = 0,
    val anonymous: Anonymous? = null,
    val message: MessageContent = emptyList(),
    val rawMessage: String = "",
    val font: Int = 0,
    val sender: GroupSender = GroupSender(0, "")
) : MessageEvent
