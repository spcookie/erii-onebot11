package uesugi.onebot.sdk.message

import kotlinx.serialization.Serializable

// ===== 类型安全的 data 类 =====

@Serializable
data class TextData(val text: String)

@Serializable
data class MarkdownData(val content: String)

@Serializable
data class AtData(val qq: String)

@Serializable
data class ImageData(
    val file: String,
    val type: String? = null,
    val url: String? = null,
    val cache: String? = null,
    val proxy: String? = null,
    val timeout: String? = null
)

@Serializable
data class ReplyData(val id: String)

@Serializable
data class FaceData(val id: String)

@Serializable
data class RecordData(
    val file: String,
    val magic: String? = null,
    val url: String? = null,
    val cache: String? = null,
    val proxy: String? = null,
    val timeout: String? = null
)

@Serializable
data class VideoData(
    val file: String,
    val url: String? = null,
    val cache: String? = null,
    val proxy: String? = null,
    val timeout: String? = null
)

@Serializable
object RpsData

@Serializable
object DiceData

@Serializable
object ShakeData

@Serializable
data class PokeData(val type: String = "", val id: String = "")

@Serializable
data class AnonymousData(val ignore: String? = null)

@Serializable
data class ShareData(
    val url: String,
    val title: String,
    val content: String = "",
    val image: String = ""
)

@Serializable
data class ContactData(val type: String, val id: String)

@Serializable
data class LocationData(
    val lat: String,
    val lon: String,
    val title: String = "",
    val content: String = ""
)

@Serializable
data class MusicData(
    val type: String,
    val id: String = "",
    val url: String = "",
    val audio: String = "",
    val title: String = "",
    val content: String = "",
    val image: String = ""
)

@Serializable
data class ForwardData(val id: String)

@Serializable
data class NodeData(val id: String)

@Serializable
data class XmlData(val data: String)

@Serializable
data class JsonData(val data: String)
