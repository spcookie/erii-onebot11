package uesugi.onebot.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Action 请求。
 *
 * 通过 HTTP 或 WebSocket 发送给 OneBot 实现。
 * - action: API 名称（如 send_private_msg）
 * - params: 请求参数
 * - echo: 请求标识（WS 模式下用于请求/响应匹配）
 */
@Serializable
data class ActionRequest(
    val action: String,
    val params: JsonObject = JsonObject(emptyMap()),
    val echo: String? = null
)

/**
 * Action 响应。
 *
 * - status: "ok" / "async" / "failed"
 * - retcode: 返回码，0 表示成功，1 表示异步
 * - data: 响应数据
 * - echo: 回显请求标识
 */
@Serializable
data class ActionResponse(
    val status: String = "ok",
    val retcode: Int = 0,
    val data: JsonElement = JsonNull,
    val echo: String? = null
) {
    companion object {
        const val STATUS_OK = "ok"
        const val STATUS_ASYNC = "async"
        const val STATUS_FAILED = "failed"

        // retcode 与 HTTP 状态码对照（OneBot 11 规范）
        const val RETCODE_BAD_REQUEST = 1400    // 400
        const val RETCODE_UNAUTHORIZED = 1401   // 401
        const val RETCODE_FORBIDDEN = 1403      // 403
        const val RETCODE_NOT_FOUND = 1404      // 404

        fun ok(data: JsonElement, echo: String? = null) =
            ActionResponse(status = STATUS_OK, retcode = 0, data = data, echo = echo)

        fun async(echo: String? = null) =
            ActionResponse(status = STATUS_ASYNC, retcode = 1, data = JsonNull, echo = echo)

        fun failed(retcode: Int, echo: String? = null) =
            ActionResponse(status = STATUS_FAILED, retcode = retcode, data = JsonNull, echo = echo)

        fun badRequest(echo: String? = null) = failed(RETCODE_BAD_REQUEST, echo)
        fun unauthorized(echo: String? = null) = failed(RETCODE_UNAUTHORIZED, echo)
        fun forbidden(echo: String? = null) = failed(RETCODE_FORBIDDEN, echo)
        fun notFound(echo: String? = null) = failed(RETCODE_NOT_FOUND, echo)
    }
}
