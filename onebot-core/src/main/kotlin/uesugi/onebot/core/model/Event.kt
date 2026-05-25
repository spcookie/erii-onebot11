package uesugi.onebot.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * OneBot 事件基类接口（sealed，支持多态序列化）。
 *
 * 所有事件共享三个公共字段：
 * - time: 事件发生时间戳
 * - selfId: 收到事件的机器人 QQ 号
 * - postType: 事件类别（message / notice / request / meta_event）
 */
@Serializable
sealed interface OneBotEvent {
    val time: Long
    val selfId: Long
    val postType: String
}

@Serializable
sealed interface MessageEvent : OneBotEvent {
    val messageType: String
    val subType: String
}

@Serializable
sealed interface NoticeEvent : OneBotEvent {
    val noticeType: String
}

@Serializable
sealed interface RequestEvent : OneBotEvent {
    val requestType: String
}

@Serializable
sealed interface MetaEvent : OneBotEvent {
    val metaEventType: String
}

val OneBotEvent.detailType: String
    get() = when (this) {
        is MessageEvent -> subType
        is NoticeEvent -> noticeType
        is RequestEvent -> requestType
        is MetaEvent -> metaEventType
        is RawEvent -> rawDetailType
    }

/**
 * 兜底事件类型。
 *
 * 用于承载协议扩展或未来版本中未知事件子类型的原始数据。
 * 所有公共字段从 JSON 解析，其余字段保存在 [raw] 中。
 */
@Serializable
data class RawEvent(
    override val time: Long = 0,
    override val selfId: Long = 0,
    override val postType: String = "",
    val rawDetailType: String = "",
    val raw: JsonObject = JsonObject(emptyMap()),
) : OneBotEvent
