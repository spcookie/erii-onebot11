package uesugi.onebot.app

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.pipeline.LoggingMiddleware
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.api.sendGroupMsg
import uesugi.onebot.sdk.client.api.sendPrivateMsg
import uesugi.onebot.sdk.client.onGroupMessage
import uesugi.onebot.sdk.client.onNotice
import uesugi.onebot.sdk.client.onPrivateMessage
import uesugi.onebot.sdk.message.buildMessage

/**
 * OneBot SDK Demo 应用。
 *
 * 演示如何使用 SDK 连接 OneBot 实现，注册事件处理器，调用 API。
 */
object App {
    private val logger = LoggerFactory.getLogger(App::class.java)

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val config = OneBotConfig(
            wsReverseEnable = true,
            wsReverseUrl = System.getenv("ONEBOT_WS_URL") ?: "ws://127.0.0.1:6700",
            wsReverseUseUniversal = true,
            accessToken = System.getenv("ONEBOT_TOKEN"),
            selfId = System.getenv("ONEBOT_SELF_ID")?.toLongOrNull() ?: 0
        )

        val client = OneBotClient(config)

        client.use(LoggingMiddleware("onebot-app"))

        client.onPrivateMessage { event ->
            logger.info("收到私聊: user={}, message={}", event.userId, event.rawMessage)

            if (event.rawMessage == "/ping") {
                client.sendPrivateMsg(event.userId, buildMessage { text("pong!") })
            }
        }

        client.onGroupMessage { event ->
            logger.info(
                "收到群消息: group={}, user={}, message={}",
                event.groupId, event.userId, event.rawMessage
            )

            when {
                event.rawMessage == "/ping" -> {
                    client.sendGroupMsg(event.groupId, buildMessage {
                        reply(event.messageId.toLong())
                        text("pong!")
                    })
                }

                event.rawMessage.startsWith("/echo ") -> {
                    val content = event.rawMessage.removePrefix("/echo ")
                    client.sendGroupMsg(event.groupId, buildMessage { text(content) })
                }
            }
        }

        client.onNotice { event ->
            logger.info("收到通知: noticeType={}", event.noticeType)
        }

        client.start()
        logger.info("OneBot App 已启动，按 Ctrl+C 退出")

        // 等待退出信号
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                logger.info("正在关闭...")
                client.stop()
            }
        })

        // 保持运行
        while (isActive) {
            delay(1000)
        }
    }
}
