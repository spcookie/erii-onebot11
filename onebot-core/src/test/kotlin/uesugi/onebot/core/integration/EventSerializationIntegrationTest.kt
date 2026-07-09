package uesugi.onebot.core.integration

import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.EventParser
import uesugi.onebot.core.transport.JsonFactory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 事件序列化 ↔ EventParser 反序列化 round-trip 集成测试。
 *
 * 不涉及网络 I/O，仅验证序列化格式与解析器的兼容性。
 */
class EventSerializationIntegrationTest {

    private val json = JsonFactory.base
    private val parser = EventParser()

    @Test
    fun `PrivateMessageEvent round-trip`() {
        val event = PrivateMessageEvent(
            time = 1700000000L, selfId = 10001L,
            subType = "friend", messageId = 12345,
            userId = 987654321L,
            message = listOf(textSegment("hello")),
            rawMessage = "hello",
            sender = Sender(987654321L, "TestUser", sex = "male", age = 18)
        )
        val serialized = json.encodeToString(PrivateMessageEvent.serializer(), event)
        val parsed = parser.deserialize(serialized)

        assertTrue(parsed is PrivateMessageEvent)
        val result = parsed as PrivateMessageEvent
        assertEquals(12345, result.messageId)
        assertEquals(987654321L, result.userId)
        assertEquals("hello", result.rawMessage)
        assertEquals("TestUser", result.sender.nickname)
    }

    @Test
    fun `GroupMessageEvent round-trip with sender`() {
        val event = GroupMessageEvent(
            time = 1700000000L, selfId = 10001L,
            subType = "normal", messageId = 67890,
            groupId = 555666777L, userId = 111222333L,
            message = listOf(atSegment(10001L), textSegment(" hello")),
            rawMessage = "[CQ:at,qq=10001] hello",
            sender = GroupSender(111222333L, "GroupUser", card = "CardName", role = "admin", title = "VIP")
        )
        val serialized = json.encodeToString(GroupMessageEvent.serializer(), event)
        val parsed = parser.deserialize(serialized)

        assertTrue(parsed is GroupMessageEvent)
        val result = parsed as GroupMessageEvent
        assertEquals(67890, result.messageId)
        assertEquals(555666777L, result.groupId)
        assertEquals("admin", result.sender.role)
        assertEquals("VIP", result.sender.title)
    }

    @Test
    fun `GroupBanEvent round-trip`() {
        val event = GroupBanEvent(
            time = 1700000000L, selfId = 10001L,
            subType = "ban", groupId = 555666777L,
            operatorId = 10001L, userId = 111222333L,
            duration = 600L
        )
        val serialized = json.encodeToString(GroupBanEvent.serializer(), event)
        val parsed = parser.deserialize(serialized)

        assertTrue(parsed is GroupBanEvent)
        assertEquals(600L, (parsed as GroupBanEvent).duration)
    }

    @Test
    fun `PokeEvent round-trip`() {
        val event = PokeEvent(
            time = 1700000000L, selfId = 10001L,
            groupId = 555666777L, userId = 111222333L,
            targetId = 10001L
        )
        val serialized = json.encodeToString(PokeEvent.serializer(), event)
        val parsed = parser.deserialize(serialized)

        assertTrue(parsed is PokeEvent)
        assertEquals(10001L, (parsed as PokeEvent).targetId)
    }

    @Test
    fun `FriendRequestEvent round-trip`() {
        val event = FriendRequestEvent(
            time = 1700000000L, selfId = 10001L,
            userId = 111222333L,
            comment = "Hello, I am a fan!",
            flag = "abc123flag"
        )
        val serialized = json.encodeToString(FriendRequestEvent.serializer(), event)
        val parsed = parser.deserialize(serialized)

        assertTrue(parsed is FriendRequestEvent)
        assertEquals("Hello, I am a fan!", (parsed as FriendRequestEvent).comment)
        assertEquals("abc123flag", parsed.flag)
    }

    @Test
    fun `LifecycleMetaEvent round-trip`() {
        val event = LifecycleMetaEvent(
            time = 1700000000L, selfId = 10001L,
            subType = "connect"
        )
        val serialized = json.encodeToString(LifecycleMetaEvent.serializer(), event)
        val parsed = parser.deserialize(serialized)

        assertTrue(parsed is LifecycleMetaEvent)
        assertEquals("connect", (parsed as LifecycleMetaEvent).subType)
    }

    @Test
    fun `all notice event types serialize and parse correctly`() {
        data class TestCase(val event: NoticeEvent, val typeName: String)

        val cases = listOf(
            TestCase(
                GroupUploadEvent(
                    1700000000L,
                    10001L,
                    groupId = 1L,
                    userId = 2L,
                    file = FileInfo("id", "f.jpg", 100, 1)
                ), "group_upload"
            ),
            TestCase(GroupAdminEvent(1700000000L, 10001L, subType = "set", groupId = 1L, userId = 2L), "group_admin"),
            TestCase(
                GroupDecreaseEvent(
                    1700000000L,
                    10001L,
                    subType = "kick",
                    groupId = 1L,
                    operatorId = 3L,
                    userId = 2L
                ), "group_decrease"
            ),
            TestCase(
                GroupIncreaseEvent(
                    1700000000L,
                    10001L,
                    subType = "approve",
                    groupId = 1L,
                    operatorId = 3L,
                    userId = 2L
                ), "group_increase"
            ),
            TestCase(
                GroupBanEvent(
                    1700000000L,
                    10001L,
                    subType = "ban",
                    groupId = 1L,
                    operatorId = 3L,
                    userId = 2L,
                    duration = 60L
                ), "group_ban"
            ),
            TestCase(FriendAddEvent(1700000000L, 10001L, userId = 2L), "friend_add"),
            TestCase(
                GroupRecallEvent(1700000000L, 10001L, groupId = 1L, userId = 2L, operatorId = 3L, messageId = 99),
                "group_recall"
            ),
            TestCase(FriendRecallEvent(1700000000L, 10001L, userId = 2L, messageId = 88), "friend_recall"),
            TestCase(PokeEvent(1700000000L, 10001L, groupId = 1L, userId = 2L, targetId = 3L), "notify"),
            TestCase(LuckyKingEvent(1700000000L, 10001L, groupId = 1L, userId = 2L, targetId = 3L), "notify"),
            TestCase(HonorEvent(1700000000L, 10001L, groupId = 1L, userId = 2L, honorType = "talkative"), "notify")
        )

        cases.forEach { (event, _) ->
            val serialized = serializeNoticeEvent(event)
            val parsed = parser.deserialize(serialized)
            assertTrue(parsed is NoticeEvent, "Expected NoticeEvent for ${event::class.simpleName}")
            assertEquals(event.noticeType, (parsed as NoticeEvent).noticeType)
        }
    }

    private fun serializeNoticeEvent(event: NoticeEvent): String = when (event) {
        is GroupUploadEvent -> json.encodeToString(GroupUploadEvent.serializer(), event)
        is GroupAdminEvent -> json.encodeToString(GroupAdminEvent.serializer(), event)
        is GroupDecreaseEvent -> json.encodeToString(GroupDecreaseEvent.serializer(), event)
        is GroupIncreaseEvent -> json.encodeToString(GroupIncreaseEvent.serializer(), event)
        is GroupBanEvent -> json.encodeToString(GroupBanEvent.serializer(), event)
        is FriendAddEvent -> json.encodeToString(FriendAddEvent.serializer(), event)
        is GroupRecallEvent -> json.encodeToString(GroupRecallEvent.serializer(), event)
        is FriendRecallEvent -> json.encodeToString(FriendRecallEvent.serializer(), event)
        is PokeEvent -> json.encodeToString(PokeEvent.serializer(), event)
        is LuckyKingEvent -> json.encodeToString(LuckyKingEvent.serializer(), event)
        is HonorEvent -> json.encodeToString(HonorEvent.serializer(), event)
    }

    @Test
    fun `serialized JSON has no class discriminator`() {
        val event = PrivateMessageEvent(
            time = 1700000000L, selfId = 10001L,
            subType = "friend", messageId = 1,
            userId = 1L,
            message = listOf(textSegment("test")),
            rawMessage = "test"
        )
        val serialized = json.encodeToString(PrivateMessageEvent.serializer(), event)

        assertTrue(serialized.contains("\"post_type\""), "Should contain post_type")
        assertTrue(serialized.contains("\"message_type\""), "Should contain message_type")
        // No class discriminator in wire format
        assertFalse(serialized.contains("uesugi.onebot"), "Should NOT contain qualified class name")
    }
}
