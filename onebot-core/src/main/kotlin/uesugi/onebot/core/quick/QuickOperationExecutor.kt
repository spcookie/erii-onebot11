package uesugi.onebot.core.transport

import org.slf4j.LoggerFactory
import uesugi.onebot.core.model.*

/**
 * 快速操作执行器。
 *
 * 将 [QuickOperation] 转换为对应的 API 调用。
 * 不同事件类型支持不同的快速操作，详见 OneBot 11 规范。
 */
class QuickOperationExecutor(
    private val actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult
) {
    private val logger = LoggerFactory.getLogger(QuickOperationExecutor::class.java)

    /**
     * 根据事件上下文执行快速操作。
     * 返回 true 表示至少执行了一个操作。
     */
    suspend fun execute(event: OneBotEvent, operation: QuickOperation): Boolean {
        var executed = false

        if (operation.reply.isNotEmpty()) {
            executeReply(event, operation)
            executed = true
        }

        if (operation.delete && event is GroupMessageEvent) {
            actionHandler(ActionName.DELETE_MSG, DeleteMsgRequest(event.messageId))
            executed = true
        }

        if (operation.kick && event is GroupMessageEvent) {
            actionHandler(ActionName.SET_GROUP_KICK, SetGroupKickRequest(event.groupId, event.userId))
            executed = true
        }

        if (operation.ban && event is GroupMessageEvent) {
            actionHandler(
                ActionName.SET_GROUP_BAN,
                SetGroupBanRequest(event.groupId, event.userId, operation.banDuration)
            )
            executed = true
        }

        if (operation.approve != null && event is RequestEvent) {
            executeRequestApprove(event, operation)
            executed = true
        }

        return executed
    }

    private suspend fun executeReply(event: OneBotEvent, operation: QuickOperation) {
        when (event) {
            is PrivateMessageEvent -> {
                actionHandler(
                    ActionName.SEND_PRIVATE_MSG,
                    SendPrivateMsgRequest(
                        userId = event.userId,
                        message = operation.reply,
                        autoEscape = operation.autoEscape
                    )
                )
            }

            is GroupMessageEvent -> {
                val finalMessage = if (operation.atSender) {
                    listOf(atSegment(event.userId)) + operation.reply
                } else {
                    operation.reply
                }
                actionHandler(
                    ActionName.SEND_GROUP_MSG,
                    SendGroupMsgRequest(
                        groupId = event.groupId,
                        message = finalMessage,
                        autoEscape = operation.autoEscape
                    )
                )
            }

            else -> logger.debug("Quick reply not supported for event type: {}", event::class.simpleName)
        }
    }

    private suspend fun executeRequestApprove(event: RequestEvent, operation: QuickOperation) {
        when (event) {
            is FriendRequestEvent -> {
                actionHandler(
                    ActionName.SET_FRIEND_ADD_REQUEST,
                    SetFriendAddRequestRequest(
                        flag = event.flag,
                        approve = operation.approve ?: true,
                        remark = operation.remark ?: ""
                    )
                )
            }

            is GroupRequestEvent -> {
                actionHandler(
                    ActionName.SET_GROUP_ADD_REQUEST,
                    SetGroupAddRequestRequest(
                        flag = event.flag,
                        subType = event.subType,
                        approve = operation.approve ?: true,
                        reason = operation.reason ?: ""
                    )
                )
            }
        }
    }
}
