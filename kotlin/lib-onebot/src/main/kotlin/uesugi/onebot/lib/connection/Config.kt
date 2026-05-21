package uesugi.onebot.lib.connection

/**
 * OneBot 连接配置。
 */
data class OneBotConfig(
    // --- 通用配置 ---
    val accessToken: String? = null,
    val secret: String? = null,
    val selfId: Long = 0,

    // --- HTTP 模式 ---
    val httpEnable: Boolean = false,
    val httpHost: String = "0.0.0.0",
    val httpPort: Int = 5700,

    // --- HTTP POST 模式 ---
    val httpPostEnable: Boolean = false,
    val httpPostUrl: String = "",
    val httpPostHost: String = "127.0.0.1",
    val httpPostPort: Int = 8080,

    // --- 正向 WS 模式 ---
    val wsEnable: Boolean = false,
    val wsHost: String = "0.0.0.0",
    val wsPort: Int = 6700,

    // --- 反向 WS 模式 ---
    val wsReverseEnable: Boolean = false,
    val wsReverseUrl: String = "",
    val wsReverseApiUrl: String = "",
    val wsReverseEventUrl: String = "",
    val wsReverseUseUniversal: Boolean = true,

    // --- 其他配置 ---
    val messageFormat: String = "string",
    val heartbeatEnable: Boolean = false,
    val heartbeatInterval: Long = 15000,
    val rateLimitInterval: Long = 500,
    val reconnectInterval: Long = 3000,
    val timeoutMs: Long = 30000,

    // --- 版本信息 ---
    val appName: String = "onebot-kotlin",
    val appVersion: String = "1.0.0",
    val protocolVersion: String = "v11",
)
