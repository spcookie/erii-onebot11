package uesugi.onebot.core.dispatch

import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.EventParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventParserTest {

    private val parser = EventParser()

    // ===== Message Events =====

    @Test
    fun `parse private message event`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "message",
            "message_type": "private",
            "sub_type": "friend",
            "message_id": 12345,
            "user_id": 987654321,
            "message": [{"type": "text", "data": {"text": "hi"}}],
            "raw_message": "hi",
            "font": 0,
            "sender": {"user_id": 987654321, "nickname": "TestUser"}
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is PrivateMessageEvent)
        assertEquals(12345, event.messageId)
        assertEquals(987654321L, event.userId)
    }

    @Test
    fun `parse group message event`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "message",
            "message_type": "group",
            "sub_type": "normal",
            "message_id": 67890,
            "group_id": 555666777,
            "user_id": 111222333,
            "message": [{"type": "text", "data": {"text": "hello"}}],
            "raw_message": "hello",
            "font": 0,
            "sender": {"user_id": 111222333, "nickname": "GroupUser"}
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupMessageEvent)
        assertEquals(67890, event.messageId)
        assertEquals(555666777L, event.groupId)
    }

    // ===== Notice Events =====

    @Test
    fun `parse group upload notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_upload",
            "group_id": 555666777,
            "user_id": 111222333,
            "file": {"id": "f1", "name": "pic.jpg", "size": 1024, "busid": 1}
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupUploadEvent)
        assertEquals("pic.jpg", event.file.name)
    }

    @Test
    fun `parse group admin notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_admin",
            "sub_type": "set",
            "group_id": 555666777,
            "user_id": 111222333
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupAdminEvent)
    }

    @Test
    fun `parse group decrease notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_decrease",
            "sub_type": "leave",
            "group_id": 555666777,
            "operator_id": 0,
            "user_id": 111222333
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupDecreaseEvent)
    }

    @Test
    fun `parse group increase notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_increase",
            "sub_type": "approve",
            "group_id": 555666777,
            "operator_id": 10001,
            "user_id": 111222333
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupIncreaseEvent)
    }

    @Test
    fun `parse group ban notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_ban",
            "sub_type": "ban",
            "group_id": 555666777,
            "operator_id": 10001,
            "user_id": 111222333,
            "duration": 600
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupBanEvent)
        assertEquals(600L, event.duration)
    }

    @Test
    fun `parse friend add notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "friend_add",
            "user_id": 111222333
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is FriendAddEvent)
    }

    @Test
    fun `parse group recall notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_recall",
            "group_id": 555666777,
            "user_id": 111222333,
            "operator_id": 10001,
            "message_id": 99999
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupRecallEvent)
    }

    @Test
    fun `parse friend recall notice`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "friend_recall",
            "user_id": 111222333,
            "message_id": 88888
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is FriendRecallEvent)
    }

    @Test
    fun `parse poke event from notify`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "notify",
            "sub_type": "poke",
            "group_id": 555666777,
            "user_id": 111222333,
            "target_id": 10001
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is PokeEvent)
        assertEquals(10001L, event.targetId)
    }

    @Test
    fun `parse lucky king event from notify`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "notify",
            "sub_type": "lucky_king",
            "group_id": 555666777,
            "user_id": 111222333,
            "target_id": 10001
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is LuckyKingEvent)
    }

    @Test
    fun `parse honor event from notify`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "notify",
            "sub_type": "honor",
            "group_id": 555666777,
            "user_id": 111222333,
            "honor_type": "talkative"
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is HonorEvent)
        assertEquals("talkative", event.honorType)
    }

    // ===== Request Events =====

    @Test
    fun `parse friend request event`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "request",
            "request_type": "friend",
            "user_id": 111222333,
            "comment": "Hello!",
            "flag": "abc123"
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is FriendRequestEvent)
        assertEquals("Hello!", event.comment)
    }

    @Test
    fun `parse group request event`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "request",
            "request_type": "group",
            "sub_type": "add",
            "group_id": 555666777,
            "user_id": 111222333,
            "comment": "",
            "flag": "groupFlag"
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is GroupRequestEvent)
        assertEquals("add", event.subType)
    }

    // ===== Meta Events =====

    @Test
    fun `parse lifecycle meta event`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "meta_event",
            "meta_event_type": "lifecycle",
            "sub_type": "connect"
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is LifecycleMetaEvent)
        assertEquals("connect", event.subType)
    }

    @Test
    fun `parse heartbeat meta event`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "meta_event",
            "meta_event_type": "heartbeat",
            "status": {"online": true, "good": true},
            "interval": 5000
        }
        """.trimIndent()

        val event = parser.deserialize(raw)
        assertTrue(event is HeartbeatMetaEvent)
        assertEquals(5000L, event.interval)
    }

    // ===== Fallback Cases =====

    @Test
    fun `parse returns RawEvent on missing post_type`() {
        val raw = """{"time": 1700000000, "self_id": 10001}"""
        val event = parser.deserialize(raw)
        assertTrue(event is RawEvent)
        assertEquals("", event.postType)
        assertEquals(1700000000L, event.time)
        assertEquals(10001L, event.selfId)
    }

    @Test
    fun `parse returns RawEvent on unknown post_type`() {
        val raw = """{"time": 1700000000, "self_id": 10001, "post_type": "invalid"}"""
        val event = parser.deserialize(raw)
        assertTrue(event is RawEvent)
        assertEquals("invalid", event.postType)
    }
}
