package uesugi.onebot.lib.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * MessageContent 序列化器。
 * 支持三种格式：
 * - JSON 字符串 → 作为纯文本消息段
 * - JSON 数组 → 消息段数组
 * - 单个 JSON 对象 → 包装为单元素数组
 */
object MessageContentSerializer : KSerializer<MessageContent> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MessageContent")

    override fun serialize(encoder: Encoder, value: MessageContent) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalStateException("MessageContentSerializer requires JSON encoding")
        val array = JsonArray(value.map { segmentToJson(it) })
        jsonEncoder.encodeJsonElement(array)
    }

    override fun deserialize(decoder: Decoder): MessageContent {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("MessageContentSerializer requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonPrimitive -> {
                // 字符串格式 → 单个 text 段
                listOf(textSegment(element.content))
            }
            is JsonArray -> {
                // 数组格式 → 逐个解析段
                element.map { parseSegment(it) }
            }
            is JsonObject -> {
                // 单个对象 → 包装为单元素列表
                listOf(parseSegment(element))
            }
            is JsonNull -> emptyList()
        }
    }

    private fun parseSegment(element: JsonElement): MessageSegment {
        val obj = element.jsonObject
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "text"
        val dataObj = obj["data"]?.jsonObject ?: JsonObject(emptyMap())
        val data = dataObj.mapValues { (_, v) ->
            when (v) {
                is JsonPrimitive -> v.content
                is JsonNull -> ""
                else -> v.toString()
            }
        }
        return MessageSegment(type, data)
    }
}
