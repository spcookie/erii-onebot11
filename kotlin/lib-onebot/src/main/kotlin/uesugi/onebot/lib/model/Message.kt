package uesugi.onebot.lib.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*

/**
 * 消息段——OneBot 消息的基本组成单元。
 * 使用扁平化 Map 存储数据，通过扩展属性提供类型安全访问。
 */
@Serializable
data class MessageSegment(
    val type: String,
    val data: Map<String, String> = emptyMap(),
)

/**
 * 消息内容：可以是纯文本字符串、消息段数组或单个消息段。
 */
typealias MessageContent = List<MessageSegment>

// ============ 扩展属性：类型安全访问器 ============

val MessageSegment.text: String? get() = if (type == "text") data["text"] else null
val MessageSegment.imageFile: String? get() = if (type == "image") data["file"] else null
val MessageSegment.imageUrl: String? get() = if (type == "image") data["url"] else null
val MessageSegment.atQq: String? get() = if (type == "at") data["qq"] else null
val MessageSegment.replyId: String? get() = if (type == "reply") data["id"] else null
val MessageSegment.faceId: String? get() = if (type == "face") data["id"] else null
val MessageSegment.recordFile: String? get() = if (type == "record") data["file"] else null
val MessageSegment.videoFile: String? get() = if (type == "video") data["file"] else null
val MessageSegment.pokeType: String? get() = if (type == "poke") data["type"] else null

// ============ 工厂方法 ============

fun textSegment(text: String) = MessageSegment("text", mapOf("text" to text))
fun atSegment(qq: Long) = MessageSegment("at", mapOf("qq" to qq.toString()))
fun atSegment(qq: String) = MessageSegment("at", mapOf("qq" to qq))
fun imageSegment(file: String) = MessageSegment("image", mapOf("file" to file))
fun faceSegment(id: Int) = MessageSegment("face", mapOf("id" to id.toString()))
fun replySegment(id: Long) = MessageSegment("reply", mapOf("id" to id.toString()))
fun recordSegment(file: String) = MessageSegment("record", mapOf("file" to file))
fun videoSegment(file: String) = MessageSegment("video", mapOf("file" to file))
fun pokeSegment(type: String, id: String = "") = MessageSegment("poke", mapOf("type" to type, "id" to id))
fun shakeSegment() = MessageSegment("shake")
fun anonymousSegment() = MessageSegment("anonymous")
fun rpsSegment() = MessageSegment("rps")
fun diceSegment() = MessageSegment("dice")
fun shareSegment(url: String, title: String, content: String = "", image: String = "") =
    MessageSegment("share", mapOf("url" to url, "title" to title, "content" to content, "image" to image))
fun contactSegment(type: String, id: Long) = MessageSegment("contact", mapOf("type" to type, "id" to id.toString()))
fun locationSegment(lat: Double, lon: Double, title: String = "", content: String = "") =
    MessageSegment("location", mapOf("lat" to lat.toString(), "lon" to lon.toString(), "title" to title, "content" to content))
fun musicSegment(type: String, id: String = "", url: String = "", audio: String = "", title: String = "") =
    MessageSegment("music", mapOf("type" to type, "id" to id, "url" to url, "audio" to audio, "title" to title))
fun xmlSegment(data: String) = MessageSegment("xml", mapOf("data" to data))
fun jsonSegment(data: String) = MessageSegment("json", mapOf("data" to data))

/**
 * 便捷方法：将纯文本字符串包装为单段消息
 */
fun String.toMessage(): List<MessageSegment> = listOf(textSegment(this))
