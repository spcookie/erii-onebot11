package uesugi.onebot.mock

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.lib.server.OneBotServer
import uesugi.onebot.mock.handler.MockActionDispatcher
import uesugi.onebot.mock.storage.InMemoryStorage
import uesugi.onebot.mock.storage.MockStorage

class MockBot(
    private val config: OneBotConfig = OneBotConfig(
        httpEnable = true, httpHost = "127.0.0.1", httpPort = 5700,
        wsForwardServerEnable = true, wsForwardServerHost = "127.0.0.1", wsForwardServerPort = 6700,
        selfId = 10001, appName = "mock-bot-server"
    ),
    val storage: MockStorage = InMemoryStorage(selfId = 10001)
) {
    private val server = OneBotServer(config)
    private val eventScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dispatcher = MockActionDispatcher(storage) { event ->
        eventScope.launch {
            try {
                server.pushEvent(event)
            } catch (_: CancellationException) {
                // 正常关闭流程，忽略
            }
        }
    }

    // ===== Lifecycle =====

    suspend fun start() {
        dispatcher.registerTo(server)
        server.start()
    }

    suspend fun stop() {
        server.stop()
        eventScope.cancel()
    }

    suspend fun pushEvent(event: OneBotEvent) = server.pushEvent(event)

    // ===== Management APIs =====

    fun addGroup(groupId: Long, name: String, memberCount: Int = 0): GroupInfo {
        val group = GroupInfo(groupId = groupId, groupName = name, memberCount = memberCount)
        storage.addGroup(group)
        return group
    }

    fun addUser(userId: Long, nickname: String): Sender {
        val user = Sender(userId = userId, nickname = nickname)
        storage.addUser(user)
        return user
    }

    fun addGroupMember(groupId: Long, userId: Long, nickname: String, role: String = "member"): GroupMemberInfo {
        val member = GroupMemberInfo(
            groupId = groupId, userId = userId,
            nickname = nickname, role = role,
            joinTime = System.currentTimeMillis() / 1000
        )
        storage.addGroupMember(groupId, member)
        return member
    }

    fun addFriend(userId: Long, nickname: String, remark: String = ""): FriendInfo {
        val friend = FriendInfo(userId = userId, nickname = nickname, remark = remark)
        storage.addFriend(friend)
        return friend
    }

    fun setupGroupWithMembers(groupId: Long, name: String, vararg memberIds: Long): GroupInfo {
        val group = addGroup(groupId, name, memberIds.size)
        memberIds.forEachIndexed { i, uid -> addGroupMember(groupId, uid, "User$uid") }
        return group
    }

    // ===== Simulation APIs — typed overload =====

    suspend inline fun <reified T> simulateAction(
        userId: Long,
        action: String,
        request: T
    ) {
        val params = JsonFactory.base.encodeToJsonElement(request).jsonObject
        simulateAction(userId, action, params)
    }

    // ===== Simulation APIs — raw overload =====

    suspend fun simulateAction(
        userId: Long,
        action: String,
        params: JsonObject
    ): ActionResponse {
        dispatcher.actorUserId = userId
        try {
            return server.dispatchAction(action, params)
        } finally {
            dispatcher.actorUserId = 0
        }
    }

    // ===== Convenience Simulation Methods =====

    suspend fun simulatePrivateMessage(fromUserId: Long, text: String) {
        simulateAction(
            userId = fromUserId,
            action = ActionName.SEND_PRIVATE_MSG,
            request = SendPrivateMsgRequest(
                userId = config.selfId,
                message = listOf(textSegment(text))
            )
        )
    }

    suspend fun simulateGroupMessage(groupId: Long, fromUserId: Long, text: String) {
        simulateAction(
            userId = fromUserId,
            action = ActionName.SEND_GROUP_MSG,
            request = SendGroupMsgRequest(
                groupId = groupId,
                message = listOf(textSegment(text))
            )
        )
    }

    suspend fun simulateNotice(event: NoticeEvent) {
        server.pushEvent(event)
    }

    suspend fun simulateRequest(event: RequestEvent) {
        server.pushEvent(event)
    }
}
