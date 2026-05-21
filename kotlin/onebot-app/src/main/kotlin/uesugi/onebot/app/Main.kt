package uesugi.onebot.app

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import uesugi.onebot.app.handlers.MessageHandlers
import uesugi.onebot.app.handlers.NoticeHandlers
import uesugi.onebot.lib.connection.OneBotConfig
import uesugi.onebot.sdk.OneBot

/**
 * OneBot 示例应用入口。
 *
 * 默认使用反向 WebSocket 模式连接到 OneBot 实现端（如 NapCat、go-cqhttp）。
 */
fun main() {
    val logger = LoggerFactory.getLogger("onebot-app")

    // 配置：反向 WS 模式
    val config = OneBotConfig(
        wsReverseEnable = true,
        wsReverseUrl = "ws://127.0.0.1:8080/ws",
        wsReverseUseUniversal = true,
        accessToken = null,
        selfId = 0,
        appName = "onebot-app",
        appVersion = "1.0.0",
    )

    val bot = OneBot(config)

    // 注册消息处理器
    MessageHandlers(bot).register()

    // 注册通知处理器
    NoticeHandlers(bot).register()

    // 启动
    runBlocking {
        bot.start()
        logger.info("Bot is running, press Ctrl+C to stop")

        // 等待停止信号
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                logger.info("Shutting down...")
                bot.stop()
            }
        })

        // 保持运行
        while (true) {
            delay(1000)
        }
    }
}
