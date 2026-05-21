package uesugi.onebot.app.handlers

import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.*
import uesugi.onebot.sdk.OneBot
import uesugi.onebot.sdk.util.buildMessage

/**
 * 示例通知事件处理器。
 */
class NoticeHandlers(private val bot: OneBot) {
    private val logger = LoggerFactory.getLogger(NoticeHandlers::class.java)

    fun register() {
        bot.onNotice { event ->
            when (event) {
                is GroupIncreaseEvent -> handleMemberJoin(event)
                is GroupDecreaseEvent -> handleMemberLeave(event)
                is GroupBanEvent -> handleGroupBan(event)
                is FriendAddEvent -> handleFriendAdd(event)
                else -> logger.debug("Notice: {}", event::class.simpleName)
            }
        }
    }

    private suspend fun handleMemberJoin(event: GroupIncreaseEvent) {
        logger.info("Member joined group {}: user_id={}", event.groupId, event.userId)
        try {
            bot.sendGroupMsg(event.groupId, buildMessage {
                at(event.userId)
                text(" 欢迎加入群聊！")
            })
        } catch (e: Exception) {
            logger.error("Failed to send welcome: {}", e.message)
        }
    }

    private suspend fun handleMemberLeave(event: GroupDecreaseEvent) {
        logger.info("Member left group {}: user_id={}, sub_type={}", event.groupId, event.userId, event.subType)
    }

    private suspend fun handleGroupBan(event: GroupBanEvent) {
        val action = if (event.subType == "ban") "被禁言" else "被解除禁言"
        logger.info("Group {} ban: user_id={}, duration={}", event.groupId, event.userId, event.duration)
    }

    private suspend fun handleFriendAdd(event: FriendAddEvent) {
        logger.info("New friend added: user_id={}", event.userId)
    }
}
