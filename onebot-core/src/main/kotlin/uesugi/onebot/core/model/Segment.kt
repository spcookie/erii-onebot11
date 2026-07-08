package uesugi.onebot.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * 消息段模型。
 *
 * OneBot 消息由消息段数组构成，每个段有 type（类型）和 data（参数）。
 * type 包括 text、image、at、reply 等 20 种。
 */
@Serializable
data class MessageSegment(
    val type: String,
    val data: Map<String, JsonElement> = emptyMap()
)

/** 消息内容 = 消息段列表（数组格式） */
typealias MessageContent = List<MessageSegment>

/** 创建 text 消息段 */
fun textSegment(text: String): MessageSegment = MessageSegment(
    type = "text",
    data = mapOf("text" to JsonPrimitive(text))
)

/** 创建 markdown 消息段，content 为平台原生 Markdown 内容 */
fun markdownSegment(content: String): MessageSegment = MessageSegment(
    type = "markdown",
    data = mapOf("content" to JsonPrimitive(content))
)

/** 创建 at 消息段（qq 为 QQ 号，或 "all" 表示 @全体成员） */
fun atSegment(qq: Long): MessageSegment = MessageSegment(
    type = "at",
    data = mapOf("qq" to JsonPrimitive(qq.toString()))
)

/** 创建 image 消息段。type="flash" 表示闪照 */
fun imageSegment(
    file: String,
    type: String? = null,
    url: String? = null,
    cache: Boolean? = null,
    proxy: Boolean? = null,
    timeout: Int? = null
): MessageSegment = MessageSegment(
    type = "image",
    data = buildMap {
        put("file", JsonPrimitive(file))
        if (type != null) put("type", JsonPrimitive(type))
        if (url != null) put("url", JsonPrimitive(url))
        if (cache != null) put("cache", JsonPrimitive(if (cache) "1" else "0"))
        if (proxy != null) put("proxy", JsonPrimitive(if (proxy) "1" else "0"))
        if (timeout != null) put("timeout", JsonPrimitive(timeout.toString()))
    }
)

/** 创建 reply 消息段 */
fun replySegment(id: Long): MessageSegment = MessageSegment(
    type = "reply",
    data = mapOf("id" to JsonPrimitive(id.toString()))
)

/** 创建 face 消息段 */
fun faceSegment(id: String): MessageSegment = MessageSegment(
    type = "face",
    data = mapOf("id" to JsonPrimitive(id))
)

/** 创建 record 消息段。magic=true 表示变声 */
fun recordSegment(
    file: String,
    magic: Boolean? = null,
    url: String? = null,
    cache: Boolean? = null,
    proxy: Boolean? = null,
    timeout: Int? = null
): MessageSegment = MessageSegment(
    type = "record",
    data = buildMap {
        put("file", JsonPrimitive(file))
        if (magic != null) put("magic", JsonPrimitive(if (magic) "1" else "0"))
        if (url != null) put("url", JsonPrimitive(url))
        if (cache != null) put("cache", JsonPrimitive(if (cache) "1" else "0"))
        if (proxy != null) put("proxy", JsonPrimitive(if (proxy) "1" else "0"))
        if (timeout != null) put("timeout", JsonPrimitive(timeout.toString()))
    }
)

/** 创建 video 消息段 */
fun videoSegment(
    file: String,
    url: String? = null,
    cache: Boolean? = null,
    proxy: Boolean? = null,
    timeout: Int? = null
): MessageSegment = MessageSegment(
    type = "video",
    data = buildMap {
        put("file", JsonPrimitive(file))
        if (url != null) put("url", JsonPrimitive(url))
        if (cache != null) put("cache", JsonPrimitive(if (cache) "1" else "0"))
        if (proxy != null) put("proxy", JsonPrimitive(if (proxy) "1" else "0"))
        if (timeout != null) put("timeout", JsonPrimitive(timeout.toString()))
    }
)

/** 创建 rps 消息段（猜拳魔法表情） */
fun rpsSegment(): MessageSegment = MessageSegment("rps", emptyMap())

/** 创建 dice 消息段（掷骰子魔法表情） */
fun diceSegment(): MessageSegment = MessageSegment("dice", emptyMap())

/** 创建 shake 消息段（窗口抖动） */
fun shakeSegment(): MessageSegment = MessageSegment("shake", emptyMap())

/** 创建 poke 消息段（戳一戳） */
fun pokeSegment(type: String = "", id: String = ""): MessageSegment = MessageSegment(
    type = "poke",
    data = mapOf("type" to JsonPrimitive(type), "id" to JsonPrimitive(id))
)

/** 创建 anonymous 消息段（匿名发消息）。ignore=true 表示无法匿名时继续发送 */
fun anonymousSegment(ignore: Boolean? = null): MessageSegment = MessageSegment(
    type = "anonymous",
    data = if (ignore != null) mapOf("ignore" to JsonPrimitive(if (ignore) "1" else "0")) else emptyMap()
)

/** 创建 share 消息段（链接分享） */
fun shareSegment(url: String, title: String, content: String = "", image: String = ""): MessageSegment = MessageSegment(
    type = "share",
    data = mapOf(
        "url" to JsonPrimitive(url),
        "title" to JsonPrimitive(title),
        "content" to JsonPrimitive(content),
        "image" to JsonPrimitive(image)
    )
)

/** 创建 contact 消息段（推荐好友 type="qq" / 推荐群 type="group"） */
fun contactSegment(type: String, id: Long): MessageSegment = MessageSegment(
    type = "contact",
    data = mapOf("type" to JsonPrimitive(type), "id" to JsonPrimitive(id.toString()))
)

/** 创建 location 消息段（位置分享） */
fun locationSegment(lat: Double, lon: Double, title: String = "", content: String = ""): MessageSegment =
    MessageSegment(
        type = "location",
        data = mapOf(
            "lat" to JsonPrimitive(lat.toString()),
            "lon" to JsonPrimitive(lon.toString()),
            "title" to JsonPrimitive(title),
            "content" to JsonPrimitive(content)
        )
    )

/** 创建 music 消息段。type 为 qq/163/xm（音乐平台）或 custom（自定义音乐） */
fun musicSegment(
    type: String,
    id: String = "",
    url: String = "",
    audio: String = "",
    title: String = "",
    content: String = "",
    image: String = ""
): MessageSegment = MessageSegment(
    type = "music",
    data = mapOf(
        "type" to JsonPrimitive(type),
        "id" to JsonPrimitive(id),
        "url" to JsonPrimitive(url),
        "audio" to JsonPrimitive(audio),
        "title" to JsonPrimitive(title),
        "content" to JsonPrimitive(content),
        "image" to JsonPrimitive(image)
    )
)

/** 创建 forward 消息段（合并转发，仅接收） */
fun forwardSegment(id: String): MessageSegment = MessageSegment(
    type = "forward",
    data = mapOf("id" to JsonPrimitive(id))
)

/** 创建 node 消息段（合并转发节点，仅发送） */
fun nodeSegment(id: String): MessageSegment = MessageSegment(
    type = "node",
    data = mapOf("id" to JsonPrimitive(id))
)

/** 创建 xml 消息段 */
fun xmlSegment(data: String): MessageSegment = MessageSegment(
    type = "xml",
    data = mapOf("data" to JsonPrimitive(data))
)

/** 创建 json 消息段 */
fun jsonSegment(data: String): MessageSegment = MessageSegment(
    type = "json",
    data = mapOf("data" to JsonPrimitive(data))
)
