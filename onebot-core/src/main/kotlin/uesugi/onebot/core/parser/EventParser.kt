package uesugi.onebot.core.parser

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.core.codec.MessageFormatConverter
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.JsonFactory

/**
 * 事件解析器（register 驱动）。
 *
 * 通过两级/三级鉴别器构建 key，查找注册的序列化器完成：
 * - 反序列化：[parse] 将原始 JSON 字符串 → [OneBotEvent]
 * - 序列化：[serialize] 将 [OneBotEvent] → [JsonObject]
 *
 * 内置 message 字段格式转换：
 * - 反序列化时自动兼容 CQ 码字符串格式输入
 * - 序列化时根据 [messageFormat] 配置决定输出格式
 *
 * key 规则：
 * - "message:private" / "message:group"
 * - "notice:group_upload" / "notice:notify:poke" 等
 * - "request:friend" / "request:group"
 * - "meta_event:lifecycle" / "meta_event:heartbeat"
 *
 * 未知 key 降级为 [RawEvent] 保留所有原始数据。
 */
class EventParser(
    private val messageFormat: String = MESSAGE_FORMAT_DEFAULT
) {
    companion object {
        const val MESSAGE_FORMAT_DEFAULT = "array"
    }

    private val json = JsonFactory.base
    private val logger = LoggerFactory.getLogger(EventParser::class.java)
    private val serializers = mutableMapOf<String, KSerializer<*>>()

    init {
        // message
        register("message:private", PrivateMessageEvent.serializer())
        register("message:group", GroupMessageEvent.serializer())

        // notice — direct
        register("notice:group_upload", GroupUploadEvent.serializer())
        register("notice:group_admin", GroupAdminEvent.serializer())
        register("notice:group_decrease", GroupDecreaseEvent.serializer())
        register("notice:group_increase", GroupIncreaseEvent.serializer())
        register("notice:group_ban", GroupBanEvent.serializer())
        register("notice:friend_add", FriendAddEvent.serializer())
        register("notice:group_recall", GroupRecallEvent.serializer())
        register("notice:friend_recall", FriendRecallEvent.serializer())

        // notice — notify
        register("notice:notify:poke", PokeEvent.serializer())
        register("notice:notify:lucky_king", LuckyKingEvent.serializer())
        register("notice:notify:honor", HonorEvent.serializer())

        // request
        register("request:friend", FriendRequestEvent.serializer())
        register("request:group", GroupRequestEvent.serializer())

        // message_sent
        register("message_sent:group", MessageSentEvent.serializer())
        register("message_sent:private", MessageSentEvent.serializer())

        // meta_event
        register("meta_event:lifecycle", LifecycleMetaEvent.serializer())
        register("meta_event:heartbeat", HeartbeatMetaEvent.serializer())
    }

    fun <T : OneBotEvent> register(key: String, serializer: KSerializer<T>) {
        serializers[key] = serializer
    }

    // ===== 反序列化：String → OneBotEvent =====

    fun deserialize(raw: String): OneBotEvent {
        val root = json.parseToJsonElement(raw).jsonObject
        // 兼容 CQ 码字符串格式的 message 字段
        val converted = MessageFormatConverter.convertInput(root)

        val postType = converted["post_type"]?.jsonPrimitive?.content
        if (postType == null) {
            logger.warn("Missing post_type in event JSON, returning RawEvent")
            return rawEvent(converted)
        }

        val key = buildKey(postType, converted)
        val serializer = serializers[key]
        if (serializer != null) {
            @Suppress("UNCHECKED_CAST")
            return json.decodeFromJsonElement(serializer as DeserializationStrategy<OneBotEvent>, converted)
        }

        logger.debug("Unknown event key: {}, returning RawEvent", key)
        return rawEvent(converted)
    }

    // ===== 序列化：OneBotEvent → JsonObject =====

    /**
     * 将 [OneBotEvent] 序列化为 [JsonObject]。
     *
     * 根据事件对象的运行时类型推导 key，查找注册的序列化器完成编码。
     * 未注册的事件类型回退为直接编码。
     */
    fun serialize(event: OneBotEvent): JsonObject {
        val key = eventKey(event)
        val serializer = serializers[key]
        val result = if (serializer != null) {
            @Suppress("UNCHECKED_CAST")
            json.encodeToJsonElement(
                serializer as SerializationStrategy<OneBotEvent>, event
            ).jsonObject
        } else {
            // fallback：使用对象自身的序列化器
            when (event) {
                is RawEvent -> json.encodeToJsonElement(RawEvent.serializer(), event).jsonObject
                else -> {
                    logger.debug("Unknown event key: {} for type {}, using direct encode", key, event::class.simpleName)
                    json.encodeToJsonElement(OneBotEvent.serializer(), event).jsonObject
                }
            }
        }
        return MessageFormatConverter.convertOutput(result, messageFormat)
    }

    // ===== key 构建（双向） =====

    /** 从 JSON 字段构建 key（反序列化方向） */
    private fun buildKey(postType: String, root: JsonObject): String {
        return when (postType) {
            "message" -> "$postType:${root["message_type"]?.jsonPrimitive?.content ?: ""}"
            "notice" -> {
                val noticeType = root["notice_type"]?.jsonPrimitive?.content ?: ""
                if (noticeType == "notify") {
                    "$postType:$noticeType:${root["sub_type"]?.jsonPrimitive?.content ?: ""}"
                } else {
                    "$postType:$noticeType"
                }
            }

            "request" -> "$postType:${root["request_type"]?.jsonPrimitive?.content ?: ""}"
            "meta_event" -> "$postType:${root["meta_event_type"]?.jsonPrimitive?.content ?: ""}"
            "message_sent" -> "$postType:${root["message_type"]?.jsonPrimitive?.content ?: ""}"
            else -> postType
        }
    }

    /** 从事件对象类型推导 key（序列化方向） */
    private fun eventKey(event: OneBotEvent): String {
        return when (event) {
            is PrivateMessageEvent -> "message:private"
            is GroupMessageEvent -> "message:group"
            is GroupUploadEvent -> "notice:group_upload"
            is GroupAdminEvent -> "notice:group_admin"
            is GroupDecreaseEvent -> "notice:group_decrease"
            is GroupIncreaseEvent -> "notice:group_increase"
            is GroupBanEvent -> "notice:group_ban"
            is FriendAddEvent -> "notice:friend_add"
            is GroupRecallEvent -> "notice:group_recall"
            is FriendRecallEvent -> "notice:friend_recall"
            is PokeEvent -> "notice:notify:poke"
            is LuckyKingEvent -> "notice:notify:lucky_king"
            is HonorEvent -> "notice:notify:honor"
            is FriendRequestEvent -> "request:friend"
            is GroupRequestEvent -> "request:group"
            is LifecycleMetaEvent -> "meta_event:lifecycle"
            is HeartbeatMetaEvent -> "meta_event:heartbeat"
            is MessageSentEvent -> "message_sent:${event.messageType}"
            is RawEvent -> ""
        }
    }

    // ===== 降级处理 =====

    private fun rawEvent(root: JsonObject): RawEvent {
        val postType = root["post_type"]?.jsonPrimitive?.contentOrNull ?: ""
        val detailField = when (postType) {
            "message" -> "message_type"
            "notice" -> "notice_type"
            "request" -> "request_type"
            "meta_event" -> "meta_event_type"
            else -> null
        }
        return RawEvent(
            time = root["time"]?.jsonPrimitive?.longOrNull ?: 0,
            selfId = root["self_id"]?.jsonPrimitive?.longOrNull ?: 0,
            postType = postType,
            rawDetailType = detailField?.let { root[it]?.jsonPrimitive?.contentOrNull } ?: "",
            raw = root
        )
    }
}
