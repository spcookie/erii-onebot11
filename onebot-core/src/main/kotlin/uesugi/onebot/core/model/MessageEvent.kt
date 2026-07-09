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

/**
 * 消息发送事件（message_sent）。
 * 机器人主动发送消息后由协议端（如 NapCat）上报，结构同 [GroupMessageEvent] 或 [PrivateMessageEvent]，
 * 但 post_type = "message_sent"。
 * 通过 [messageType] 区分群聊/私聊，使用 [tryAsGroupMessage] / [tryAsPrivateMessage] 获取对应类型。
 */
@Serializable
data class MessageSentEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "message_sent",
    override val messageType: String,
    override val subType: String = "normal",
    val messageId: Int = 0,
    val groupId: Long? = null,
    val userId: Long = 0,
    val targetId: Long = 0,
    val anonymous: Anonymous? = null,
    val message: MessageContent = emptyList(),
    val rawMessage: String = "",
    val font: Int = 0,
    val sender: GroupSender = GroupSender(0, ""),
) : MessageEvent {

    val isGroupMessage: Boolean get() = messageType == "group"
    val isPrivateMessage: Boolean get() = messageType == "private"

    fun tryAsGroupMessage(): GroupMessageEvent? {
        if (!isGroupMessage || groupId == null) return null
        return GroupMessageEvent(
            time = time, selfId = selfId, postType = postType,
            messageType = messageType, subType = subType,
            messageId = messageId, groupId = groupId, userId = userId,
            anonymous = anonymous, message = message, rawMessage = rawMessage,
            font = font, sender = sender
        )
    }

    fun tryAsPrivateMessage(): PrivateMessageEvent? {
        if (!isPrivateMessage) return null
        return PrivateMessageEvent(
            time = time, selfId = selfId, postType = postType,
            messageType = messageType, subType = subType,
            messageId = messageId, userId = userId,
            message = message, rawMessage = rawMessage,
            font = font, sender = Sender(userId = sender.userId, nickname = sender.nickname, sex = sender.sex, age = sender.age)
        )
    }

    fun getGroupMessage(): GroupMessageEvent =
        tryAsGroupMessage() ?: error("Not a group message, messageType=$messageType")

    fun getPrivateMessage(): PrivateMessageEvent =
        tryAsPrivateMessage() ?: error("Not a private message, messageType=$messageType")

    companion object {
        fun of(event: GroupMessageEvent): MessageSentEvent = MessageSentEvent(
            time = event.time, selfId = event.selfId,
            messageType = event.messageType, subType = event.subType,
            messageId = event.messageId, groupId = event.groupId, userId = event.userId,
            anonymous = event.anonymous, message = event.message, rawMessage = event.rawMessage,
            font = event.font, sender = event.sender
        )

        fun of(event: PrivateMessageEvent): MessageSentEvent = MessageSentEvent(
            time = event.time, selfId = event.selfId,
            messageType = event.messageType, subType = event.subType,
            messageId = event.messageId, userId = event.userId,
            message = event.message, rawMessage = event.rawMessage,
            font = event.font, sender = GroupSender(
                userId = event.sender.userId, nickname = event.sender.nickname,
                sex = event.sender.sex, age = event.sender.age
            )
        )
    }
}
