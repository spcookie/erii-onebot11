package uesugi.onebot.app.integration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.mock.MockBot
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockBotIntegrationTest {

    private val mockBotConfig = OneBotConfig(
        httpEnable = true, httpHost = "127.0.0.1", httpPort = 5710,
        httpPostEnable = true, httpPostUrl = "http://127.0.0.1:5711",
        selfId = 10001, appName = "mock-bot-test"
    )

    private val clientConfig = OneBotConfig(
        httpEnable = true, httpHost = "127.0.0.1", httpPort = 5710,
        httpPostEnable = true, httpPostHost = "127.0.0.1", httpPostPort = 5711,
        selfId = 10001
    )

    private lateinit var mockBot: MockBot
    private lateinit var client: OneBotClient

    @Before
    fun setup() {
        runBlocking {
            mockBot = MockBot(mockBotConfig)
            mockBot.addUser(10086, "Alice")
            mockBot.addUser(10087, "Bob")
            mockBot.addGroup(12345, "Test Group")
            mockBot.addGroupMember(12345, 10001, "Bot")
            mockBot.addGroupMember(12345, 10086, "Alice")
            mockBot.addGroupMember(12345, 10087, "Bob")
            mockBot.addFriend(10086, "Alice")
            mockBot.start()
            delay(500)

            client = OneBotClient(clientConfig)
            client.start()
            delay(500)
        }
    }

    @After
    fun teardown() {
        runBlocking {
            client.stop()
            mockBot.stop()
        }
    }

    // ===== 工具函数 =====

    private inline fun <reified T : OneBotEvent> captureEvent(
        crossinline predicate: (T) -> Boolean = { true }
    ): CompletableDeferred<T> {
        val deferred = CompletableDeferred<T>()
        client.onEvent("message") { event ->
            if (event is T && predicate(event) && !deferred.isCompleted) {
                deferred.complete(event)
            }
        }
        client.onEvent("notice") { event ->
            if (event is T && predicate(event) && !deferred.isCompleted) {
                deferred.complete(event)
            }
        }
        client.onEvent("request") { event ->
            if (event is T && predicate(event) && !deferred.isCompleted) {
                deferred.complete(event)
            }
        }
        client.onEvent("meta_event") { event ->
            if (event is T && predicate(event) && !deferred.isCompleted) {
                deferred.complete(event)
            }
        }
        return deferred
    }

    // ===== Message Action Tests =====

    @Test
    fun testSendPrivateMsg() {
        runBlocking {
            val msgId = client.sendPrivateMsg(10086, listOf(textSegment("hello")))
            assertTrue(msgId > 0, "message_id should be positive")

            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            assertEquals("private", msg.messageType)
        }
    }

    @Test
    fun testSendGroupMsg() {
        runBlocking {
            val msgId = client.sendGroupMsg(12345, listOf(textSegment("hello group")))
            assertTrue(msgId > 0)

            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            assertEquals("group", msg.messageType)
        }
    }

    @Test
    fun testSendMsg() {
        runBlocking {
            val msgId = client.sendMsg("private", 10086, null, listOf(textSegment("hello via send_msg")))
            assertTrue(msgId > 0)
        }
    }

    @Test
    fun testDeleteMsg() {
        runBlocking {
            val msgId = client.sendPrivateMsg(10086, listOf(textSegment("to delete")))
            client.deleteMsg(msgId)
        }
    }

    @Test
    fun testGetForwardMsg() {
        runBlocking {
            val result = client.getForwardMsg("test_id")
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun testSendLike() {
        runBlocking {
            client.sendLike(10086, 3)
        }
    }

    // ===== Group Action Tests =====

    @Test
    fun testSetGroupKick() {
        runBlocking {
            mockBot.addGroupMember(12345, 99999, "TempUser")
            client.setGroupKick(12345, 99999)
            val members = client.getGroupMemberList(12345)
            assertFalse(members.any { it.userId == 99999L })
        }
    }

    @Test
    fun testSetGroupBan() {
        runBlocking {
            val captured = captureEvent<GroupBanEvent>()
            client.setGroupBan(12345, 10087, 600)
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10087, event.userId)
            assertEquals(600, event.duration)
            assertEquals("ban", event.subType)
        }
    }

    @Test
    fun testSetGroupWholeBan() {
        runBlocking {
            client.setGroupWholeBan(12345, true)
        }
    }

    @Test
    fun testSetGroupAdmin() {
        runBlocking {
            val captured = captureEvent<GroupAdminEvent>()
            client.setGroupAdmin(12345, 10087, true)
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10087, event.userId)
            assertEquals("set", event.subType)
        }
    }

    @Test
    fun testSetGroupCard() {
        runBlocking {
            client.setGroupCard(12345, 10087, "NewCard")
        }
    }

    @Test
    fun testSetGroupName() {
        runBlocking {
            client.setGroupName(12345, "Renamed Group")
        }
    }

    @Test
    fun testSetGroupLeave() {
        runBlocking {
            val newGroupId = 99999L
            mockBot.addGroup(newGroupId, "Leave Test")
            mockBot.addGroupMember(newGroupId, 10001, "Bot")

            client.setGroupLeave(newGroupId, false)

            val members = mockBot.storage.getGroupMembers(newGroupId)
            assertFalse(members.any { it.userId == 10001L })
        }
    }

    @Test
    fun testSetGroupSpecialTitle() {
        runBlocking {
            client.setGroupSpecialTitle(12345, 10087, "Special")
        }
    }

    // ===== Info Action Tests =====

    @Test
    fun testGetLoginInfo() {
        runBlocking {
            val info = client.getLoginInfo()
            assertEquals(10001, info.userId)
        }
    }

    @Test
    fun testGetStrangerInfo() {
        runBlocking {
            val info = client.getStrangerInfo(10086)
            assertEquals(10086, info.userId)
            assertEquals("Alice", info.nickname)
        }
    }

    @Test
    fun testGetFriendList() {
        runBlocking {
            val friends = client.getFriendList()
            assertEquals(1, friends.size)
            assertEquals(10086, friends[0].userId)
        }
    }

    @Test
    fun testGetGroupList() {
        runBlocking {
            val groups = client.getGroupList()
            assertEquals(1, groups.size)
            assertEquals(12345, groups[0].groupId)
        }
    }

    @Test
    fun testGetGroupInfo() {
        runBlocking {
            val info = client.getGroupInfo(12345)
            assertEquals(12345, info.groupId)
            assertEquals("Test Group", info.groupName)
        }
    }

    @Test
    fun testGetGroupMemberInfo() {
        runBlocking {
            val info = client.getGroupMemberInfo(12345, 10086)
            assertEquals(10086, info.userId)
            assertEquals("Alice", info.nickname)
        }
    }

    @Test
    fun testGetGroupMemberList() {
        runBlocking {
            val members = client.getGroupMemberList(12345)
            assertEquals(3, members.size)
        }
    }

    @Test
    fun testGetGroupHonorInfo() {
        runBlocking {
            val info = client.getGroupHonorInfo(12345)
            assertEquals(12345, info.groupId)
        }
    }

    // ===== Request Action Tests =====

    @Test
    fun testSetFriendAddRequest() {
        runBlocking {
            client.setFriendAddRequest("10088", true, "NewFriend")
            val friend = mockBot.storage.getFriend(10088)
            assertNotNull(friend)
        }
    }

    @Test
    fun testSetGroupAddRequest() {
        runBlocking {
            val captured = captureEvent<GroupIncreaseEvent>()
            client.setGroupAddRequest("12345:10099", "add", true)
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10099, event.userId)

            val member = mockBot.storage.getGroupMember(12345, 10099)
            assertNotNull(member)
        }
    }

    // ===== System Action Tests =====

    @Test
    fun testGetCookies() {
        runBlocking {
            val info = client.getCookies()
            assertNotNull(info)
        }
    }

    @Test
    fun testGetCsrfToken() {
        runBlocking {
            val info = client.getCsrfToken()
            assertNotNull(info)
        }
    }

    @Test
    fun testGetCredentials() {
        runBlocking {
            val info = client.getCredentials()
            assertNotNull(info)
        }
    }

    @Test
    fun testGetRecord() {
        runBlocking {
            val info = client.getRecord("test.mp3")
            assertTrue(info.file.contains("mock_record_"))
        }
    }

    @Test
    fun testGetImage() {
        runBlocking {
            val info = client.getImage("test.jpg")
            assertTrue(info.file.contains("mock_image_"))
        }
    }

    @Test
    fun testCanSendImage() {
        runBlocking {
            assertTrue(client.canSendImage())
        }
    }

    @Test
    fun testCanSendRecord() {
        runBlocking {
            assertTrue(client.canSendRecord())
        }
    }

    @Test
    fun testGetStatus() {
        runBlocking {
            val status = client.getStatus()
            assertTrue(status.online)
            assertTrue(status.good)
        }
    }

    @Test
    fun testGetVersionInfo() {
        runBlocking {
            val info = client.getVersionInfo()
            assertEquals("mock-bot-server", info.appName)
            assertEquals("v11", info.protocolVersion)
        }
    }

    @Test
    fun testSetRestart() {
        runBlocking {
            val captured = captureEvent<LifecycleMetaEvent>()
            client.setRestart()
            val event = withTimeout(3000) { captured.await() }
            assertEquals("lifecycle", event.metaEventType)
            assertEquals("disable", event.subType)
        }
    }

    @Test
    fun testCleanCache() {
        runBlocking {
            client.cleanCache()
        }
    }

    // ===== Message Event Tests =====

    @Test
    fun testPrivateMessageEvent() {
        runBlocking {
            val captured = captureEvent<PrivateMessageEvent>()
            mockBot.simulatePrivateMessage(10086, listOf(textSegment("test private msg")))
            val event = withTimeout(3000) { captured.await() }
            assertEquals(10086, event.userId)
            assertEquals("private", event.messageType)
        }
    }

    @Test
    fun testGroupMessageEvent() {
        runBlocking {
            val captured = captureEvent<GroupMessageEvent>()
            mockBot.simulateGroupMessage(12345, 10086, listOf(textSegment("test group msg")))
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10086, event.userId)
        }
    }

    // ===== Notice Event Tests =====

    @Test
    fun testGroupUploadEvent() {
        runBlocking {
            val captured = captureEvent<GroupUploadEvent>()
            mockBot.simulateNotice(
                GroupUploadEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    groupId = 12345,
                    userId = 10086,
                    file = FileInfo("1", "test.txt", 1024, 0)
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10086, event.userId)
        }
    }

    @Test
    fun testGroupDecreaseEvent() {
        runBlocking {
            val captured = captureEvent<GroupDecreaseEvent>()
            mockBot.simulateNotice(
                GroupDecreaseEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    subType = "leave",
                    groupId = 12345,
                    userId = 10087
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10087, event.userId)
        }
    }

    @Test
    fun testFriendAddEvent() {
        runBlocking {
            val captured = captureEvent<FriendAddEvent>()
            mockBot.simulateNotice(
                FriendAddEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    userId = 10086
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(10086, event.userId)
        }
    }

    @Test
    fun testGroupRecallEvent() {
        runBlocking {
            val captured = captureEvent<GroupRecallEvent>()
            mockBot.simulateNotice(
                GroupRecallEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    groupId = 12345,
                    userId = 10086,
                    operatorId = 10086,
                    messageId = 42
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(42, event.messageId)
        }
    }

    @Test
    fun testFriendRecallEvent() {
        runBlocking {
            val captured = captureEvent<FriendRecallEvent>()
            mockBot.simulateNotice(
                FriendRecallEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    userId = 10086,
                    messageId = 42
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(10086, event.userId)
            assertEquals(42, event.messageId)
        }
    }

    @Test
    fun testPokeEvent() {
        runBlocking {
            val captured = captureEvent<PokeEvent>()
            mockBot.simulateNotice(
                PokeEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    subType = "poke",
                    groupId = 12345,
                    userId = 10086,
                    targetId = 10087
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10086, event.userId)
            assertEquals(10087, event.targetId)
        }
    }

    @Test
    fun testLuckyKingEvent() {
        runBlocking {
            val captured = captureEvent<LuckyKingEvent>()
            mockBot.simulateNotice(
                LuckyKingEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    subType = "lucky_king",
                    groupId = 12345,
                    userId = 10086,
                    targetId = 10087
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
        }
    }

    @Test
    fun testHonorEvent() {
        runBlocking {
            val captured = captureEvent<HonorEvent>()
            mockBot.simulateNotice(
                HonorEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    subType = "honor",
                    groupId = 12345,
                    userId = 10086,
                    honorType = "talkative"
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals("talkative", event.honorType)
        }
    }

    // ===== Request Event Tests =====

    @Test
    fun testFriendRequestEvent() {
        runBlocking {
            val captured = captureEvent<FriendRequestEvent>()
            mockBot.simulateRequest(
                FriendRequestEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    userId = 10088,
                    comment = "Hello",
                    flag = "request_123"
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(10088, event.userId)
            assertEquals("friend", event.requestType)
        }
    }

    @Test
    fun testGroupRequestEvent() {
        runBlocking {
            val captured = captureEvent<GroupRequestEvent>()
            mockBot.simulateRequest(
                GroupRequestEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    subType = "add",
                    groupId = 12345,
                    userId = 10088,
                    comment = "Please let me in",
                    flag = "request_456"
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(12345, event.groupId)
            assertEquals(10088, event.userId)
            assertEquals("group", event.requestType)
        }
    }

    // ===== Meta Event Tests =====

    @Test
    fun testLifecycleMetaEvent() {
        runBlocking {
            val captured = captureEvent<LifecycleMetaEvent>()
            mockBot.pushEvent(
                LifecycleMetaEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    subType = "connect"
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals("connect", event.subType)
        }
    }

    @Test
    fun testHeartbeatMetaEvent() {
        runBlocking {
            val captured = captureEvent<HeartbeatMetaEvent>()
            mockBot.pushEvent(
                HeartbeatMetaEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = 10001,
                    interval = 15000
                )
            )
            val event = withTimeout(3000) { captured.await() }
            assertEquals(15000, event.interval)
        }
    }

    // ===== Scenario Tests =====

    @Test
    fun testGroupChatScenario() {
        runBlocking {
            val groupId = 55555L
            mockBot.addGroup(groupId, "Scenario Group")
            mockBot.addGroupMember(groupId, 10001, "Bot")
            mockBot.addGroupMember(groupId, 20001, "User1")
            mockBot.addGroupMember(groupId, 20002, "User2")

            val msgId = client.sendGroupMsg(groupId, listOf(textSegment("Hello everyone")))
            assertTrue(msgId > 0)

            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            assertEquals("group", msg.messageType)

            val banCaptured = captureEvent<GroupBanEvent>()
            client.setGroupBan(groupId, 20002, 300)
            val banEvent = withTimeout(3000) { banCaptured.await() }
            assertEquals(groupId, banEvent.groupId)
            assertEquals(20002, banEvent.userId)

            val adminCaptured = captureEvent<GroupAdminEvent>()
            client.setGroupAdmin(groupId, 20002, true)
            val adminEvent = withTimeout(3000) { adminCaptured.await() }
            assertEquals(groupId, adminEvent.groupId)
            assertEquals(20002, adminEvent.userId)
            assertEquals("set", adminEvent.subType)

            client.setGroupKick(groupId, 20002)
            val members = client.getGroupMemberList(groupId)
            assertEquals(2, members.size)
        }
    }

    @Test
    fun testPrivateChatScenario() {
        runBlocking {
            val userId = 30001L
            mockBot.addUser(userId, "PrivateUser")
            mockBot.addFriend(userId, "PrivateUser")

            val msgId = client.sendPrivateMsg(userId, listOf(textSegment("Hey there")))
            assertTrue(msgId > 0)

            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            assertEquals("private", msg.messageType)

            val friends = client.getFriendList()
            assertTrue(friends.any { it.userId == userId })
        }
    }

    @Test
    fun testMemberJoinScenario() {
        runBlocking {
            val groupId = 66666L
            val userId = 40001L
            mockBot.addGroup(groupId, "Join Test")

            val captured = captureEvent<GroupIncreaseEvent>()
            client.setGroupAddRequest("$groupId:$userId", "add", true)
            val event = withTimeout(3000) { captured.await() }
            assertEquals(groupId, event.groupId)
            assertEquals(userId, event.userId)
            assertEquals("approve", event.subType)

            val members = client.getGroupMemberList(groupId)
            assertTrue(members.any { it.userId == userId })
        }
    }

    // ===== Supplementary Action Tests =====

    @Test
    fun testSetGroupAnonymousBan() {
        runBlocking {
            client.callWith(
                ActionName.SET_GROUP_ANONYMOUS_BAN, SetGroupAnonymousBanRequest(
                    groupId = 12345, anonymousFlag = "abc123", duration = 60
                )
            )
        }
    }

    @Test
    fun testSetGroupAnonymous() {
        runBlocking {
            client.callWith(
                ActionName.SET_GROUP_ANONYMOUS, SetGroupAnonymousRequest(
                    groupId = 12345, enable = false
                )
            )
        }
    }

    @Test
    fun testSetGroupBanLift() {
        runBlocking {
            val captured = captureEvent<GroupBanEvent>()
            client.setGroupBan(12345, 10087, 0)
            val event = withTimeout(3000) { captured.await() }
            assertEquals("lift_ban", event.subType)
            assertEquals(12345, event.groupId)
            assertEquals(10087, event.userId)
            assertEquals(0, event.duration)
        }
    }

    @Test
    fun testSetGroupAdminUnset() {
        runBlocking {
            val captured = captureEvent<GroupAdminEvent>()
            client.setGroupAdmin(12345, 10087, false)
            val event = withTimeout(3000) { captured.await() }
            assertEquals("unset", event.subType)
            assertEquals(12345, event.groupId)
            assertEquals(10087, event.userId)
        }
    }

    @Test
    fun testSetFriendAddRequestReject() {
        runBlocking {
            client.setFriendAddRequest("20001", false, "")
            val friend = mockBot.storage.getFriend(20001)
            assertEquals(null, friend)
        }
    }

    @Test
    fun testSetGroupAddRequestReject() {
        runBlocking {
            client.setGroupAddRequest("12345:20002", "add", false)
            val member = mockBot.storage.getGroupMember(12345, 20002)
            assertEquals(null, member)
        }
    }
}
