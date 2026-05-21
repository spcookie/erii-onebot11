package uesugi.onebot.app.handlers

import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.GroupMessageEvent
import uesugi.onebot.lib.model.PrivateMessageEvent
import uesugi.onebot.sdk.OneBot
import uesugi.onebot.sdk.util.buildMessage

/**
 * 示例消息处理器。
 */
class MessageHandlers(private val bot: OneBot) {
    private val logger = LoggerFactory.getLogger(MessageHandlers::class.java)

    fun register() {
        // 私聊消息处理
        bot.onMessage { event ->
            when (event) {
                is PrivateMessageEvent -> handlePrivateMessage(event)
                is GroupMessageEvent -> handleGroupMessage(event)
            }
        }

        logger.info("Message handlers registered")
    }

    private suspend fun handlePrivateMessage(event: PrivateMessageEvent) {
        val raw = event.rawMessage.trim()

        when {
            // echo 回复
            raw.startsWith("/echo ") -> {
                val text = raw.removePrefix("/echo ")
                bot.sendPrivateMsg(event.userId, buildMessage { text("Echo: $text") })
            }

            // ping
            raw == "/ping" -> {
                bot.sendPrivateMsg(event.userId, buildMessage { text("pong!") })
            }

            // help
            raw == "/help" -> {
                val help = buildMessage {
                    text("Available commands:\n")
                    text("/echo <text> - Echo back\n")
                    text("/ping - Ping test\n")
                    text("/help - Show this help\n")
                    text("/status - Show bot status")
                }
                bot.sendPrivateMsg(event.userId, help)
            }

            // status
            raw == "/status" -> {
                val status = bot.getStatus()
                val info = buildMessage {
                    text("Status: ")
                    text(if (status.online) "online" else "offline")
                    text(", ")
                    text(if (status.good) "good" else "unhealthy")
                }
                bot.sendPrivateMsg(event.userId, info)
            }

            else -> {
                logger.debug("Private message from {}: {}", event.userId, raw)
            }
        }
    }

    private suspend fun handleGroupMessage(event: GroupMessageEvent) {
        val raw = event.rawMessage.trim()

        when {
            raw.startsWith("/echo ") -> {
                val text = raw.removePrefix("/echo ")
                bot.sendGroupMsg(event.groupId, buildMessage {
                    at(event.userId)
                    text(" Echo: $text")
                })
            }

            raw == "/ping" -> {
                bot.sendGroupMsg(event.groupId, buildMessage { text("pong!") })
            }
        }
    }
}
