package uesugi.onebot.sdk.message

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import uesugi.onebot.core.model.MessageSegment
import uesugi.onebot.core.transport.JsonFactory

// ===== 类型安全转换扩展 =====

/** 将 MessageSegment 的 data 解码为指定类型 */
inline fun <reified T> MessageSegment.toData(): T =
    JsonFactory.base.decodeFromJsonElement(serializer(), JsonObject(data))

/** 获取 text 消息段的文本内容 */
val MessageSegment.text: String? get() = json("text")

/** 获取 markdown 消息段的原生 Markdown 内容 */
val MessageSegment.markdownContent: String? get() = json("content")

/** 获取 at 消息段的 QQ 号 */
val MessageSegment.atQq: Long? get() = json("qq")?.toLongOrNull()

/** 获取 at 消息段是否 @全体成员 */
val MessageSegment.atAll: Boolean get() = json("qq") == "all"

/** 获取 reply 消息段的消息 ID */
val MessageSegment.replyId: Long? get() = json("id")?.toLongOrNull()

/** 获取 image 消息段的文件/URL */
val MessageSegment.imageFile: String? get() = json("file")
val MessageSegment.imageUrl: String? get() = json("url")

/** 获取 record 消息段的文件/URL */
val MessageSegment.recordFile: String? get() = json("file")
val MessageSegment.recordUrl: String? get() = json("url")

/** 获取 video 消息段的文件/URL */
val MessageSegment.videoFile: String? get() = json("file")
val MessageSegment.videoUrl: String? get() = json("url")

/** 获取 face 消息段的 ID */
val MessageSegment.faceId: String? get() = json("id")

// ===== 私有工具 =====

private fun MessageSegment.json(key: String): String? =
    data[key]?.jsonPrimitive?.content
