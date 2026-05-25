package uesugi.onebot.core.model

import kotlinx.serialization.Serializable

/**
 * 生命周期元事件。
 * post_type = "meta_event", meta_event_type = "lifecycle"
 * sub_type: enable（启用）, disable（停用）, connect（WebSocket 连接成功）
 */
@Serializable
data class LifecycleMetaEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "meta_event",
    override val metaEventType: String = "lifecycle",
    val subType: String = "enable"
) : MetaEvent

/**
 * 心跳元事件。
 * post_type = "meta_event", meta_event_type = "heartbeat"
 */
@Serializable
data class HeartbeatMetaEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String = "meta_event",
    override val metaEventType: String = "heartbeat",
    val status: StatusInfo = StatusInfo(),
    val interval: Long = 15000
) : MetaEvent
