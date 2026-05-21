package uesugi.onebot.lib.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

// ==================== 事件基类 ====================

/**
 * 所有事件的基类。
 * 使用两级判别：post_type → 子类型字段，确定具体事件类型。
 */
@Serializable
sealed class OneBotEvent {
    abstract val time: Long
    abstract val selfId: Long
    abstract val postType: String
}

// ==================== 消息事件 ====================

@Serializable
@SerialName("message")
sealed class MessageEvent : OneBotEvent() {
    override val postType = "message"
    abstract val messageType: String
    abstract val subType: String
    abstract val messageId: Long
    @Serializable(with = MessageContentSerializer::class)
    abstract val message: MessageContent
    abstract val rawMessage: String
    abstract val font: Int
}

@Serializable
@SerialName("message.private")
data class PrivateMessageEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("message_type") override val messageType: String = "private",
    @SerialName("sub_type") override val subType: String = "friend",
    @SerialName("message_id") override val messageId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    override val message: MessageContent = emptyList(),
    @SerialName("raw_message") override val rawMessage: String = "",
    override val font: Int = 0,
    val sender: PrivateSender = PrivateSender(),
) : MessageEvent()

@Serializable
@SerialName("message.group")
data class GroupMessageEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("message_type") override val messageType: String = "group",
    @SerialName("sub_type") override val subType: String = "normal",
    @SerialName("message_id") override val messageId: Long = 0,
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val anonymous: Anonymous? = null,
    override val message: MessageContent = emptyList(),
    @SerialName("raw_message") override val rawMessage: String = "",
    override val font: Int = 0,
    val sender: GroupSender = GroupSender(),
) : MessageEvent()

// ==================== 通知事件 ====================

@Serializable
@SerialName("notice")
sealed class NoticeEvent : OneBotEvent() {
    override val postType = "notice"
    abstract val noticeType: String
}

// --- 群文件上传 ---
@Serializable
@SerialName("notice.group_upload")
data class GroupUploadEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "group_upload",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val file: FileInfo = FileInfo(),
) : NoticeEvent()

// --- 群管理员变动 ---
@Serializable
@SerialName("notice.group_admin")
data class GroupAdminEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "group_admin",
    @SerialName("sub_type") val subType: String = "set",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
) : NoticeEvent()

// --- 群成员减少 ---
@Serializable
@SerialName("notice.group_decrease")
data class GroupDecreaseEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "group_decrease",
    @SerialName("sub_type") val subType: String = "leave",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("operator_id") val operatorId: Long = 0,
) : NoticeEvent()

// --- 群成员增加 ---
@Serializable
@SerialName("notice.group_increase")
data class GroupIncreaseEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "group_increase",
    @SerialName("sub_type") val subType: String = "approve",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("operator_id") val operatorId: Long = 0,
) : NoticeEvent()

// --- 群禁言 ---
@Serializable
@SerialName("notice.group_ban")
data class GroupBanEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "group_ban",
    @SerialName("sub_type") val subType: String = "ban",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("operator_id") val operatorId: Long = 0,
    val duration: Long = 0,
) : NoticeEvent()

// --- 好友添加 ---
@Serializable
@SerialName("notice.friend_add")
data class FriendAddEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "friend_add",
    @SerialName("user_id") val userId: Long = 0,
) : NoticeEvent()

// --- 群消息撤回 ---
@Serializable
@SerialName("notice.group_recall")
data class GroupRecallEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "group_recall",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("operator_id") val operatorId: Long = 0,
    @SerialName("message_id") val messageId: Long = 0,
) : NoticeEvent()

// --- 好友消息撤回 ---
@Serializable
@SerialName("notice.friend_recall")
data class FriendRecallEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "friend_recall",
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("message_id") val messageId: Long = 0,
) : NoticeEvent()

// --- 群内戳一戳 ---
@Serializable
@SerialName("notice.poke")
data class PokeEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "notify",
    @SerialName("sub_type") val subType: String = "poke",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("target_id") val targetId: Long = 0,
) : NoticeEvent()

// --- 群红包运气王 ---
@Serializable
@SerialName("notice.lucky_king")
data class LuckyKingEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "notify",
    @SerialName("sub_type") val subType: String = "lucky_king",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("target_id") val targetId: Long = 0,
) : NoticeEvent()

// --- 群荣誉变更 ---
@Serializable
@SerialName("notice.honor")
data class HonorEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("notice_type") override val noticeType: String = "notify",
    @SerialName("sub_type") val subType: String = "honor",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("honor_type") val honorType: String = "talkative",
    @SerialName("user_id") val userId: Long = 0,
) : NoticeEvent()

// ==================== 请求事件 ====================

@Serializable
@SerialName("request")
sealed class RequestEvent : OneBotEvent() {
    override val postType = "request"
    abstract val requestType: String
    abstract val comment: String
    abstract val flag: String
}

@Serializable
@SerialName("request.friend")
data class FriendRequestEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("request_type") override val requestType: String = "friend",
    override val comment: String = "",
    override val flag: String = "",
    @SerialName("user_id") val userId: Long = 0,
) : RequestEvent()

@Serializable
@SerialName("request.group")
data class GroupRequestEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("request_type") override val requestType: String = "group",
    @SerialName("sub_type") val subType: String = "add",
    override val comment: String = "",
    override val flag: String = "",
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
) : RequestEvent()

// ==================== 元事件 ====================

@Serializable
@SerialName("meta_event")
sealed class MetaEvent : OneBotEvent() {
    override val postType = "meta_event"
    abstract val metaEventType: String
}

@Serializable
@SerialName("meta_event.lifecycle")
data class LifecycleMetaEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("meta_event_type") override val metaEventType: String = "lifecycle",
    @SerialName("sub_type") val subType: String = "enable",
) : MetaEvent()

@Serializable
@SerialName("meta_event.heartbeat")
data class HeartbeatMetaEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("meta_event_type") override val metaEventType: String = "heartbeat",
    val status: StatusInfo = StatusInfo(),
    val interval: Long = 5000,
) : MetaEvent()

// ==================== 自定义事件序列化器 ====================

/**
 * 自定义事件序列化器，根据 post_type 和子类型字段多态反序列化。
 */
object OneBotEventSerializer : JsonContentPolymorphicSerializer<OneBotEvent>(OneBotEvent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<OneBotEvent> {
        val json = element.jsonObject
        val postType = json["post_type"]?.jsonPrimitive?.contentOrNull ?: return PolymorphicEvent.serializer()

        return when (postType) {
            "message" -> {
                val messageType = json["message_type"]?.jsonPrimitive?.contentOrNull
                when (messageType) {
                    "group" -> GroupMessageEvent.serializer()
                    else -> PrivateMessageEvent.serializer()
                }
            }
            "notice" -> {
                val noticeType = json["notice_type"]?.jsonPrimitive?.contentOrNull
                when (noticeType) {
                    "group_upload" -> GroupUploadEvent.serializer()
                    "group_admin" -> GroupAdminEvent.serializer()
                    "group_decrease" -> GroupDecreaseEvent.serializer()
                    "group_increase" -> GroupIncreaseEvent.serializer()
                    "group_ban" -> GroupBanEvent.serializer()
                    "friend_add" -> FriendAddEvent.serializer()
                    "group_recall" -> GroupRecallEvent.serializer()
                    "friend_recall" -> FriendRecallEvent.serializer()
                    "notify" -> {
                        val subType = json["sub_type"]?.jsonPrimitive?.contentOrNull
                        when (subType) {
                            "poke" -> PokeEvent.serializer()
                            "lucky_king" -> LuckyKingEvent.serializer()
                            "honor" -> HonorEvent.serializer()
                            else -> PolymorphicEvent.serializer()
                        }
                    }
                    else -> PolymorphicEvent.serializer()
                }
            }
            "request" -> {
                val requestType = json["request_type"]?.jsonPrimitive?.contentOrNull
                when (requestType) {
                    "group" -> GroupRequestEvent.serializer()
                    else -> FriendRequestEvent.serializer()
                }
            }
            "meta_event" -> {
                val metaType = json["meta_event_type"]?.jsonPrimitive?.contentOrNull
                when (metaType) {
                    "lifecycle" -> LifecycleMetaEvent.serializer()
                    "heartbeat" -> HeartbeatMetaEvent.serializer()
                    else -> PolymorphicEvent.serializer()
                }
            }
            else -> PolymorphicEvent.serializer()
        }
    }
}

/**
 * 未知事件类型的降级处理。
 */
@Serializable
data class PolymorphicEvent(
    override val time: Long = 0,
    @SerialName("self_id") override val selfId: Long = 0,
    @SerialName("post_type") override val postType: String = "unknown",
    val raw: JsonObject = JsonObject(emptyMap()),
) : OneBotEvent()
