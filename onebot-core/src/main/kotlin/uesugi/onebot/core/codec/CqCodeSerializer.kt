package uesugi.onebot.core.codec

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import uesugi.onebot.core.model.MessageSegment

/**
 * CQ 码序列化器：将消息段列表序列化为 CQ 码字符串。
 *
 * 规则：
 * - text 段 → 经过转义的纯文本（内部使用 escapeText）
 * - 其他段 → [CQ:type,key1=value1,key2=value2]（参数值使用 escapeParam）
 */
object CqCodeSerializer {

    fun serialize(segments: List<MessageSegment>): String {
        val sb = StringBuilder()
        for (segment in segments) {
            if (segment.type == "text") {
                sb.append(CqEscape.escapeText(segment.data["text"]?.jsonPrimitive?.content ?: ""))
            } else {
                sb.append("[CQ:${segment.type}")
                for ((key, value) in segment.data) {
                    sb.append(',')
                    sb.append(key)
                    sb.append('=')
                    sb.append(CqEscape.escapeParam(if (value is JsonPrimitive) value.content else value.toString()))
                }
                sb.append(']')
            }
        }
        return sb.toString()
    }
}
