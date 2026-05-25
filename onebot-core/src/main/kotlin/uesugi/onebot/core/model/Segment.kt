package uesugi.onebot.core.model

import kotlinx.serialization.Serializable

/**
 * 消息段模型。
 *
 * OneBot 消息由消息段数组构成，每个段有 type（类型）和 data（参数）。
 * type 包括 text、image、at、reply 等 20 种。
 */
@Serializable
data class MessageSegment(
    val type: String,
    val data: Map<String, String> = emptyMap()
)

/** 消息内容 = 消息段列表（数组格式） */
typealias MessageContent = List<MessageSegment>

/** 创建 text 消息段 */
fun textSegment(text: String): MessageSegment = MessageSegment(
    type = "text",
    data = mapOf("text" to text)
)

/** 创建 at 消息段（qq 为 QQ 号，或 "all" 表示 @全体成员） */
fun atSegment(qq: Long): MessageSegment = MessageSegment(
    type = "at",
    data = mapOf("qq" to qq.toString())
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
        put("file", file)
        if (type != null) put("type", type)
        if (url != null) put("url", url)
        if (cache != null) put("cache", if (cache) "1" else "0")
        if (proxy != null) put("proxy", if (proxy) "1" else "0")
        if (timeout != null) put("timeout", timeout.toString())
    }
)

/** 创建 reply 消息段 */
fun replySegment(id: Long): MessageSegment = MessageSegment(
    type = "reply",
    data = mapOf("id" to id.toString())
)

/** 创建 face 消息段 */
fun faceSegment(id: String): MessageSegment = MessageSegment(
    type = "face",
    data = mapOf("id" to id)
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
        put("file", file)
        if (magic != null) put("magic", if (magic) "1" else "0")
        if (url != null) put("url", url)
        if (cache != null) put("cache", if (cache) "1" else "0")
        if (proxy != null) put("proxy", if (proxy) "1" else "0")
        if (timeout != null) put("timeout", timeout.toString())
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
        put("file", file)
        if (url != null) put("url", url)
        if (cache != null) put("cache", if (cache) "1" else "0")
        if (proxy != null) put("proxy", if (proxy) "1" else "0")
        if (timeout != null) put("timeout", timeout.toString())
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
    data = mapOf("type" to type, "id" to id)
)

/** 创建 anonymous 消息段（匿名发消息）。ignore=true 表示无法匿名时继续发送 */
fun anonymousSegment(ignore: Boolean? = null): MessageSegment = MessageSegment(
    type = "anonymous",
    data = if (ignore != null) mapOf("ignore" to if (ignore) "1" else "0") else emptyMap()
)

/** 创建 share 消息段（链接分享） */
fun shareSegment(url: String, title: String, content: String = "", image: String = ""): MessageSegment = MessageSegment(
    type = "share",
    data = mapOf("url" to url, "title" to title, "content" to content, "image" to image)
)

/** 创建 contact 消息段（推荐好友 type="qq" / 推荐群 type="group"） */
fun contactSegment(type: String, id: Long): MessageSegment = MessageSegment(
    type = "contact",
    data = mapOf("type" to type, "id" to id.toString())
)

/** 创建 location 消息段（位置分享） */
fun locationSegment(lat: Double, lon: Double, title: String = "", content: String = ""): MessageSegment =
    MessageSegment(
        type = "location",
        data = mapOf("lat" to lat.toString(), "lon" to lon.toString(), "title" to title, "content" to content)
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
        "type" to type,
        "id" to id,
        "url" to url,
        "audio" to audio,
        "title" to title,
        "content" to content,
        "image" to image
    )
)

/** 创建 forward 消息段（合并转发，仅接收） */
fun forwardSegment(id: String): MessageSegment = MessageSegment(
    type = "forward",
    data = mapOf("id" to id)
)

/** 创建 node 消息段（合并转发节点，仅发送） */
fun nodeSegment(id: String): MessageSegment = MessageSegment(
    type = "node",
    data = mapOf("id" to id)
)

/** 创建 xml 消息段 */
fun xmlSegment(data: String): MessageSegment = MessageSegment(
    type = "xml",
    data = mapOf("data" to data)
)

/** 创建 json 消息段 */
fun jsonSegment(data: String): MessageSegment = MessageSegment(
    type = "json",
    data = mapOf("data" to data)
)
