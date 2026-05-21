package uesugi.onebot.lib.transport.ws

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

/**
 * WebSocket 消息信封格式。
 * 与 HTTP API 调用格式一致：{ "action": "...", "params": {...}, "echo": "..." }
 */

@Serializable
data class WsApiRequest(
    val action: String,
    val params: JsonObject? = null,
    val echo: String? = null,
)

@Serializable
data class WsApiResponse(
    val status: String,
    val retcode: Int,
    val data: JsonElement? = null,
    val echo: String? = null,
)

/**
 * WS 客户端角色
 */
enum class WsRole(val headerValue: String) {
    API("API"),
    EVENT("Event"),
    UNIVERSAL("Universal"),
}

/**
 * WS 端点路径
 */
object WsPaths {
    const val API = "/api"
    const val EVENT = "/event"
    const val ALL = "/"
}
