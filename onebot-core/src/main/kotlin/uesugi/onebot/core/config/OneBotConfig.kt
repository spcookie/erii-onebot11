package uesugi.onebot.core.config

/**
 * OneBot 统一配置。
 *
 * 覆盖 OneBot 11 规范定义的所有配置项，包含合理的默认值。
 * 不同传输模式下的使用方式详见 transport 包文档。
 */
data class OneBotConfig(
    // ===== HTTP 服务器模式 =====
    // OneBot 作为 HTTP 服务端，接收 API 调用
    val httpEnable: Boolean = false,
    val httpHost: String = "0.0.0.0",
    val httpPort: Int = 5700,

    // ===== HTTP POST 模式 =====
    // OneBot 作为 HTTP 客户端，向配置的 URL 推送事件
    val httpPostEnable: Boolean = false,
    val httpPostUrl: String? = null,
    val httpPostHost: String = "0.0.0.0",
    val httpPostPort: Int = 8080,
    val httpPostTimeout: Long = 30_000L,

    // ===== 正向 WebSocket 模式 =====
    // OneBot 作为 WebSocket 服务端，等待客户端连接
    val wsEnable: Boolean = false,
    val wsHost: String = "0.0.0.0",
    val wsPort: Int = 6700,

    // ===== 反向 WebSocket 模式 =====
    // OneBot 作为 WebSocket 客户端，主动连接配置的 URL
    val wsReverseEnable: Boolean = false,
    val wsReverseUrl: String? = null,
    val wsReverseApiUrl: String? = null,
    val wsReverseEventUrl: String? = null,
    val wsReverseUseUniversal: Boolean = false,
    val wsReverseReconnectInterval: Long = 3_000L,

    // ===== 授权 =====
    val accessToken: String? = null,
    val secret: String? = null,

    // ===== 应用信息 =====
    val selfId: Long = 0,
    val appName: String = "erii-onebot11",
    val appVersion: String = "1.0.0",

    // ===== 运行时 =====
    val timeout: Long = 30_000L,
    val heartbeatEnable: Boolean = false,
    val heartbeatInterval: Long = 15_000L,
    val rateLimitInterval: Long = 0L,
    val messageFormat: String = "array"  // "string" 或 "array"
) {
    val selfIdStr: String get() = selfId.toString()
    val authHeader: String? get() = accessToken?.let { "Bearer $it" }

    companion object {
        const val MESSAGE_FORMAT_ARRAY = "array"
        const val MESSAGE_FORMAT_STRING = "string"
    }
}
