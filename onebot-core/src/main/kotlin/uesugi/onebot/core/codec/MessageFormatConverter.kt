package uesugi.onebot.core.codec

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.MessageSegment

/**
 * 消息格式转换器：在 JSON 层面完成 message 字段的 array ↔ string 转换。
 */
object MessageFormatConverter {

    // ===== 输出：array → string =====

    /**
     * 将 JSON 中的 message 字段按 [messageFormat] 配置转换。
     * - "string"：将 message JsonArray 序列化为 CQ 码字符串
     * - "array"：保持原样
     */
    fun convertOutput(eventJson: JsonObject, messageFormat: String): JsonObject {
        if (messageFormat != OneBotConfig.MESSAGE_FORMAT_STRING) return eventJson
        val message = eventJson["message"] ?: return eventJson
        if (message !is JsonArray) return eventJson
        val segments = message.mapNotNull { it.jsonObject.toMessageSegmentOrNull() }
        val cqString = CqCodeSerializer.serialize(segments)
        return JsonObject(eventJson.toMutableMap().apply { put("message", JsonPrimitive(cqString)) })
    }

    // ===== 输入：string → array =====

    /**
     * 输入兼容转换：如果 JSON 中的 message 字段是 CQ 码字符串，解析为数组格式。
     * 已经是数组格式则原样返回。
     */
    fun convertInput(params: JsonObject): JsonObject {
        val message = params["message"] ?: return params
        if (message !is JsonPrimitive || !message.isString) return params
        val segments = CqCodeParser.parse(message.content)
        val array = JsonArray(segments.map { segmentToJson(it) })
        return JsonObject(params.toMutableMap().apply { put("message", array) })
    }

    // ===== 内部工具 =====

    private fun segmentToJson(segment: MessageSegment): JsonObject {
        return JsonObject(
            mapOf(
                "type" to JsonPrimitive(segment.type),
                "data" to JsonObject(segment.data)
            )
        )
    }

    private fun JsonObject.toMessageSegmentOrNull(): MessageSegment? {
        val type = this["type"]?.let {
            if (it is JsonPrimitive) it.content else null
        } ?: return null
        val data = this["data"]?.jsonObject ?: emptyMap()
        return MessageSegment(type, data)
    }
}
