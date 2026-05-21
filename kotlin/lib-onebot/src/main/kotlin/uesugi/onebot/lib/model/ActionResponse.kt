package uesugi.onebot.lib.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * API 请求信封。
 * 传输层发送此结构，包含动作名、参数和可选的 echo 字段。
 */
@Serializable
data class ActionRequest(
    val action: String,
    val params: JsonObject = JsonObject(emptyMap()),
    val echo: String? = null,
)

/**
 * API 响应信封。
 * status: "ok" | "async" | "failed"
 * retcode: 0 表示成功，非 0 表示错误
 */
@Serializable
data class ActionResponse(
    val status: String,
    val retcode: Int,
    val data: JsonElement? = null,
    val echo: String? = null,
) {
    val isOk: Boolean get() = status == "ok" || status == "async"
    val isFailed: Boolean get() = status == "failed"

    /** 将响应转换为 JSON 字符串（用于 HTTP/WS 传输） */
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("status", status)
        put("retcode", retcode)
        if (data != null) put("data", data)
        if (echo != null) put("echo", echo)
    }

    companion object {
        fun ok(data: JsonElement? = null, echo: String? = null) =
            ActionResponse("ok", 0, data, echo)

        fun async(data: JsonElement? = null, echo: String? = null) =
            ActionResponse("async", 0, data, echo)

        fun failed(retcode: Int = 1, echo: String? = null) =
            ActionResponse("failed", retcode, echo = echo)
    }
}

/**
 * 事件信封——从传输层接收的原始事件数据。
 */
@Serializable
data class EventEnvelope(
    val time: Long,
    @SerialName("self_id") val selfId: Long,
    @SerialName("post_type") val postType: String,
)
