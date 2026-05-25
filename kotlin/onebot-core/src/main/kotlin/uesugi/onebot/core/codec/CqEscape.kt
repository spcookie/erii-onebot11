package uesugi.onebot.core.codec

/**
 * CQ 码转义工具。
 *
 * 纯文本转义：
 *   & → &amp;
 *   [ → &#91;
 *   ] → &#93;
 *
 * 参数值转义（额外）：
 *   , → &#44;
 */
object CqEscape {

    // ===== 纯文本转义 =====

    fun escapeText(text: String): String {
        return buildString(text.length) {
            for (c in text) {
                when (c) {
                    '&' -> append("&amp;")
                    '[' -> append("&#91;")
                    ']' -> append("&#93;")
                    else -> append(c)
                }
            }
        }
    }

    fun unescapeText(text: String): String {
        return text
            .replace("&#93;", "]")
            .replace("&#91;", "[")
            .replace("&amp;", "&")
    }

    // ===== 参数值转义 =====

    fun escapeParam(value: String): String {
        return buildString(value.length) {
            for (c in value) {
                when (c) {
                    '&' -> append("&amp;")
                    '[' -> append("&#91;")
                    ']' -> append("&#93;")
                    ',' -> append("&#44;")
                    else -> append(c)
                }
            }
        }
    }

    fun unescapeParam(value: String): String {
        return value
            .replace("&#44;", ",")
            .replace("&#93;", "]")
            .replace("&#91;", "[")
            .replace("&amp;", "&")
    }
}
