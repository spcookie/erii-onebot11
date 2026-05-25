package uesugi.onebot.core.codec

import uesugi.onebot.core.model.MessageSegment
import uesugi.onebot.core.model.textSegment

/**
 * CQ 码解析器：将 CQ 码字符串解析为消息段列表。
 *
 * CQ 码格式：[CQ:type,key1=value1,key2=value2]
 * - CQ 码外的文本当作 text 段
 * - CQ 码内的参数需要反转义
 *
 * 特殊规则：
 * - node 段可能内嵌 CQ 码（content 字段），在此不做递归解析
 * - 不完整 CQ 码（无 ]）作为纯文本保留
 */
object CqCodeParser {

    // 匹配 [CQ:type,...] 格式，type 不能为空
    private val CQ_CODE_REGEX = Regex("""\[CQ:([^,\]]+)(?:,([^\]]*))?\]""")

    fun parse(raw: String): List<MessageSegment> {
        if (raw.isEmpty()) return emptyList()

        val segments = mutableListOf<MessageSegment>()
        val matches = CQ_CODE_REGEX.findAll(raw)
        var lastEnd = 0

        for (match in matches) {
            // CQ 码之前的文本 → text 段（需要反转义）
            if (match.range.first > lastEnd) {
                val textBefore = raw.substring(lastEnd, match.range.first)
                val unescaped = CqEscape.unescapeText(textBefore)
                if (unescaped.isNotEmpty()) {
                    segments.add(textSegment(unescaped))
                }
            }

            // 解析 CQ 码
            val type = match.groupValues[1]
            val paramsStr = match.groupValues.getOrNull(2) ?: ""
            val params = parseParams(paramsStr)

            segments.add(MessageSegment(type, params))
            lastEnd = match.range.last + 1
        }

        // 最后一个 CQ 码之后的文本
        if (lastEnd < raw.length) {
            val textAfter = raw.substring(lastEnd)
            val unescaped = CqEscape.unescapeText(textAfter)
            if (unescaped.isNotEmpty()) {
                segments.add(textSegment(unescaped))
            }
        }

        return segments
    }

    /**
     * 解析 CQ 码参数：key1=value1,key2=value2
     * 注意：逗号需要反转义（&#44; → ,），等号只取第一个
     */
    internal fun parseParams(paramsStr: String): Map<String, String> {
        if (paramsStr.isBlank()) return emptyMap()

        val params = mutableMapOf<String, String>()
        // 按逗号分割，但要注意转义的逗号（&#44;）不分割
        val pairs = splitParams(paramsStr)

        for (pair in pairs) {
            val eqIdx = pair.indexOf('=')
            if (eqIdx <= 0) continue // 无 key 或 key 为空
            val key = pair.substring(0, eqIdx).trim()
            val value = pair.substring(eqIdx + 1)
            if (key.isNotEmpty()) {
                params[key] = CqEscape.unescapeParam(value)
            }
        }

        return params
    }

    /**
     * 按逗号分割参数，跳过转义的逗号（&#44;）
     */
    private fun splitParams(paramsStr: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < paramsStr.length) {
            when {
                paramsStr.startsWith("&#44;", i) -> {
                    current.append("&#44;")
                    i += 5
                }
                paramsStr[i] == ',' -> {
                    parts.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(paramsStr[i])
                    i++
                }
            }
        }
        parts.add(current.toString())
        return parts
    }
}
