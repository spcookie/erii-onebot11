package uesugi.onebot.mock.handler

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import uesugi.onebot.core.codec.CqCodeSerializer
import uesugi.onebot.core.dispatch.MiddlewareException
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.lib.server.OneBotServer
import uesugi.onebot.lib.server.api.*
import uesugi.onebot.mock.storage.MockStorage

class MockActionDispatcher(
    private val storage: MockStorage,
    private val pushEvent: suspend (OneBotEvent) -> Unit
) {
    private fun now() = System.currentTimeMillis() / 1000

    /** 当 simulateAction 调用时设置为模拟用户的 ID（0 = 使用 bot selfId） */
    var actorUserId: Long = 0

    /** 当 bot 自己（非模拟用户）发送群消息时回调，用于 ChatBridge 捕获响应 */
    var onBotSendGroupMsg: (suspend (GroupMessageEvent) -> Unit)? = null

    /** 当前 actor 的 ID，如果 simulateAction 未设置则 fallback 到 bot selfId */
    private val actorId: Long get() = if (actorUserId > 0) actorUserId else storage.selfId

    private fun actorSender(): Sender =
        storage.getUser(actorId) ?: Sender(actorId, "User$actorId")

    private fun actorGroupSender(groupId: Long): GroupSender {
        val member = storage.getGroupMember(groupId, actorId)
        return if (member != null) GroupSender(
            member.userId, member.nickname, member.card,
            sex = member.sex, role = member.role, title = member.title
        ) else GroupSender(actorId, "User$actorId")
    }

    fun registerTo(server: OneBotServer) {
        registerMessageHandlers(server)
        registerGroupHandlers(server)
        registerInfoHandlers(server)
        registerRequestHandlers(server)
        registerSystemHandlers(server)
    }

    // ===== Message Handlers =====

    private fun registerMessageHandlers(server: OneBotServer) {
        server.onSendPrivateMsg { req ->
            val msgId = storage.nextMessageId()
            val event = PrivateMessageEvent(
                time = now(), selfId = storage.selfId, messageId = msgId,
                userId = actorId, message = req.message,
                rawMessage = CqCodeSerializer.serialize(req.message), sender = actorSender()
            )
            storage.saveMessage(event)
            pushEvent(event)
            MessageIdResult(msgId)
        }

        server.onSendGroupMsg { req ->
            storage.getGroupMember(req.groupId, actorId)
                ?: throw MiddlewareException(100, "Not in group")
            val msgId = storage.nextMessageId()
            val event = GroupMessageEvent(
                time = now(), selfId = storage.selfId, messageId = msgId,
                groupId = req.groupId, userId = actorId,
                message = req.message, rawMessage = CqCodeSerializer.serialize(req.message),
                sender = actorGroupSender(req.groupId)
            )
            storage.saveMessage(event)
            pushEvent(event)

            if (actorUserId == 0L) {
                onBotSendGroupMsg?.invoke(event)
            }

            MessageIdResult(msgId)
        }

        server.onSendMsg { req ->
            val msgId = storage.nextMessageId()
            val event = PrivateMessageEvent(
                time = now(), selfId = storage.selfId, messageId = msgId,
                userId = req.userId ?: 0, message = req.message,
                rawMessage = CqCodeSerializer.serialize(req.message), sender = actorSender()
            )
            storage.saveMessage(event)
            pushEvent(event)
            MessageIdResult(msgId)
        }

        server.onDeleteMsg { req ->
            storage.getMessage(req.messageId) ?: throw MiddlewareException(100, "Message not found")
            RawActionResult()
        }

        server.onGetMsg { req ->
            val msg = storage.getMessage(req.messageId)
                ?: throw MiddlewareException(100, "Message not found")
            msg
        }

        server.onGetForwardMsg {
            RawActionResult(buildJsonObject { put("messages", JsonArray(emptyList())) })
        }

        server.onSendLike { RawActionResult() }
    }

    // ===== Group Handlers =====

    private fun registerGroupHandlers(server: OneBotServer) {
        server.onSetGroupKick { req ->
            storage.getGroupMember(req.groupId, req.userId)
                ?: throw MiddlewareException(100, "Member not found")
            storage.removeGroupMember(req.groupId, req.userId)
            RawActionResult()
        }

        server.onSetGroupBan { req ->
            storage.getGroupMember(req.groupId, req.userId)
                ?: throw MiddlewareException(100, "Member not found")
            pushEvent(
                GroupBanEvent(
                    time = now(), selfId = storage.selfId,
                    subType = if (req.duration > 0) "ban" else "lift_ban",
                    groupId = req.groupId, operatorId = storage.selfId,
                    userId = req.userId, duration = req.duration
                )
            )
            RawActionResult()
        }

        server.onSetGroupAnonymousBan { RawActionResult() }

        server.onSetGroupWholeBan { req ->
            storage.getGroup(req.groupId)
                ?: throw MiddlewareException(100, "Group not found")
            RawActionResult()
        }

        server.onSetGroupAdmin { req ->
            storage.getGroupMember(req.groupId, req.userId)
                ?: throw MiddlewareException(100, "Member not found")
            pushEvent(
                GroupAdminEvent(
                    time = now(), selfId = storage.selfId,
                    subType = if (req.enable) "set" else "unset",
                    groupId = req.groupId, userId = req.userId
                )
            )
            RawActionResult()
        }

        server.onSetGroupAnonymous { RawActionResult() }

        server.onSetGroupCard { req ->
            storage.getGroupMember(req.groupId, req.userId)
                ?: throw MiddlewareException(100, "Member not found")
            RawActionResult()
        }

        server.onSetGroupName { req ->
            storage.getGroup(req.groupId)
                ?: throw MiddlewareException(100, "Group not found")
            RawActionResult()
        }

        server.onSetGroupLeave { req ->
            storage.getGroup(req.groupId)
                ?: throw MiddlewareException(100, "Group not found")
            if (req.isDismiss) storage.removeGroup(req.groupId)
            else storage.removeGroupMember(req.groupId, storage.selfId)
            RawActionResult()
        }

        server.onSetGroupSpecialTitle { req ->
            storage.getGroupMember(req.groupId, req.userId)
                ?: throw MiddlewareException(100, "Member not found")
            RawActionResult()
        }
    }

    // ===== Info Handlers =====

    private fun registerInfoHandlers(server: OneBotServer) {
        server.onGetLoginInfo {
            LoginInfo(userId = storage.selfId, nickname = "MockBot")
        }

        server.onGetStrangerInfo { req ->
            val user = storage.getUser(req.userId)
                ?: throw MiddlewareException(100, "User not found")
            StrangerInfo(userId = user.userId, nickname = user.nickname, sex = user.sex, age = user.age)
        }

        server.onGetFriendList {
            RawActionResult(JsonFactory.base.encodeToJsonElement(storage.getAllFriends()))
        }

        server.onGetGroupList {
            RawActionResult(JsonFactory.base.encodeToJsonElement(storage.getAllGroups()))
        }

        server.onGetGroupInfo { req ->
            storage.getGroup(req.groupId)
                ?: throw MiddlewareException(100, "Group not found")
        }

        server.onGetGroupMemberInfo { req ->
            storage.getGroupMember(req.groupId, req.userId)
                ?: throw MiddlewareException(100, "Member not found")
        }

        server.onGetGroupMemberList { req ->
            val members = storage.getGroupMembers(req.groupId)
            RawActionResult(JsonFactory.base.encodeToJsonElement(members))
        }

        server.onGetGroupHonorInfo { req ->
            if (storage.getGroup(req.groupId) == null)
                throw MiddlewareException(100, "Group not found")
            GroupHonorInfo(groupId = req.groupId)
        }
    }

    // ===== Request Handlers =====

    private fun registerRequestHandlers(server: OneBotServer) {
        server.onSetFriendAddRequest { req ->
            if (req.approve) {
                val userId = req.flag.toLongOrNull() ?: 0
                val user = Sender(userId, req.remark)
                storage.addUser(user)
                storage.addFriend(FriendInfo(user.userId, user.nickname, req.remark))
            }
            RawActionResult()
        }

        server.onSetGroupAddRequest { req ->
            if (req.approve && req.subType == "add") {
                val parts = req.flag.split(":")
                val groupId = parts.getOrNull(0)?.toLongOrNull()
                    ?: throw MiddlewareException(100, "Invalid flag format")
                val userId = parts.getOrNull(1)?.toLongOrNull()
                    ?: throw MiddlewareException(100, "Invalid flag format")
                val group = storage.getGroup(groupId)
                    ?: throw MiddlewareException(100, "Group not found")
                storage.addGroupMember(
                    group.groupId,
                    GroupMemberInfo(groupId = group.groupId, userId = userId, role = "member")
                )
                pushEvent(
                    GroupIncreaseEvent(
                        time = now(), selfId = storage.selfId,
                        subType = "approve", groupId = group.groupId,
                        operatorId = storage.selfId, userId = userId
                    )
                )
            }
            RawActionResult()
        }
    }

    // ===== System Handlers =====

    private fun registerSystemHandlers(server: OneBotServer) {
        server.onGetCookies {
            CookiesInfo()
        }

        server.onGetCsrfToken {
            CsrfTokenInfo()
        }

        server.onGetCredentials {
            CredentialsInfo()
        }

        server.onGetRecord { req ->
            FilePathInfo(file = "mock_record_${req.file}")
        }

        server.onGetImage { req ->
            FilePathInfo(file = "mock_image_${req.file}")
        }

        server.onCanSendImage { CanSendResult(true) }

        server.onCanSendRecord { CanSendResult(true) }

        server.onGetStatus {
            StatusInfo(online = true, good = true)
        }

        server.onGetVersionInfo {
            VersionInfo(appName = "mock-bot-server", appVersion = "1.0.0", protocolVersion = "v11")
        }

        server.onSetRestart {
            pushEvent(
                LifecycleMetaEvent(
                    time = now(), selfId = storage.selfId, subType = "disable"
                )
            )
            RawActionResult()
        }

        server.onCleanCache { RawActionResult() }
    }
}
