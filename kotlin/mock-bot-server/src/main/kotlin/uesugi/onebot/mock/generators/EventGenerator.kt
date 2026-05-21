package uesugi.onebot.mock.generators

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.connection.OneBotConnection
import uesugi.onebot.lib.model.*
import kotlin.random.Random

/**
 * 定时生成模拟事件。
 */
class EventGenerator(
    private val connection: OneBotConnection,
    private val data: MockData,
) {
    private val logger = LoggerFactory.getLogger(EventGenerator::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            while (isActive) {
                delay(15000L) // 每 15 秒发送一个模拟事件
                try {
                    sendRandomEvent()
                } catch (e: Exception) {
                    logger.debug("Failed to send event: {}", e.message)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private suspend fun sendRandomEvent() {
        val groups = data.groups
        if (groups.isEmpty()) return

        val group = groups.random()
        val members = data.groupMembers[group.groupId] ?: return
        val member = members.filter { it.userId != data.botUserId }.randomOrNull() ?: return

        val event = when (Random.nextInt(3)) {
            0 -> GroupMessageEvent(
                time = System.currentTimeMillis() / 1000,
                selfId = data.botUserId,
                subType = "normal",
                messageId = data.nextMsgId(),
                groupId = group.groupId,
                userId = member.userId,
                message = listOf(textSegment("这是一条模拟消息 [${Random.nextInt(100)}]")),
                rawMessage = "这是一条模拟消息",
                font = 0,
                sender = GroupSender(userId = member.userId, nickname = member.nickname, card = member.card),
            )
            1 -> GroupIncreaseEvent(
                time = System.currentTimeMillis() / 1000,
                selfId = data.botUserId,
                subType = "approve",
                groupId = group.groupId,
                userId = member.userId,
                operatorId = member.userId,
            )
            else -> HeartbeatMetaEvent(
                time = System.currentTimeMillis() / 1000,
                selfId = data.botUserId,
                status = StatusInfo(online = true, good = true),
                interval = 15000,
            )
        }

        connection.pushEvent(event)
        logger.debug("Mock event sent: {}", event::class.simpleName)
    }

    /** 手动触发事件（测试用） */
    suspend fun triggerEvent(event: OneBotEvent) {
        connection.pushEvent(event)
    }
}
