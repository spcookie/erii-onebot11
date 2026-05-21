package uesugi.onebot.lib.model

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class EventSerializationTest {

    // Use the custom polymorphic serializer for event parsing
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "post_type"
    }

    // ==================== Message Events ====================

    @Test
    fun `parse PrivateMessageEvent from JSON`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"message","message_type":"private","sub_type":"friend","message_id":1,"user_id":1002,"message":[{"type":"text","data":{"text":"hello"}}],"raw_message":"hello","font":0,"sender":{"user_id":1002,"nickname":"Alice","sex":"female","age":20}}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<PrivateMessageEvent>(event)
        assertEquals(1002L, event.userId)
        assertEquals("hello", event.message.first().text)
        assertEquals("Alice", event.sender.nickname)
    }

    @Test
    fun `parse GroupMessageEvent from JSON`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"message","message_type":"group","sub_type":"normal","message_id":2,"group_id":2001,"user_id":1011,"message":[{"type":"text","data":{"text":"hi"}}],"raw_message":"hi","font":0,"sender":{"user_id":1011,"nickname":"Bob","card":"B","role":"member"}}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupMessageEvent>(event)
        assertEquals(2001L, event.groupId)
        assertEquals("Bob", event.sender.nickname)
        assertEquals("B", event.sender.card)
    }

    @Test
    fun `parse GroupMessageEvent with anonymous`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"message","message_type":"group","sub_type":"anonymous","message_id":3,"group_id":2001,"user_id":0,"anonymous":{"id":1,"name":"匿名","flag":"abc"},"message":[],"raw_message":"","font":0,"sender":{"user_id":0,"nickname":"","card":"","role":"member"}}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupMessageEvent>(event)
        assertEquals("anonymous", event.subType)
        assertEquals("匿名", event.anonymous?.name)
    }

    // ==================== Notice Events ====================

    @Test
    fun `parse GroupUploadEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"group_upload","group_id":2001,"user_id":1011,"file":{"id":"f1","name":"test.txt","size":1024,"busid":1}}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupUploadEvent>(event)
        assertEquals("test.txt", event.file.name)
        assertEquals(1024L, event.file.size)
    }

    @Test
    fun `parse GroupAdminEvent set`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"group_admin","sub_type":"set","group_id":2001,"user_id":1011}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupAdminEvent>(event)
        assertEquals("set", event.subType)
    }

    @Test
    fun `parse GroupDecreaseEvent kick`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"group_decrease","sub_type":"kick","group_id":2001,"user_id":1011,"operator_id":10001}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupDecreaseEvent>(event)
        assertEquals("kick", event.subType)
        assertEquals(10001L, event.operatorId)
    }

    @Test
    fun `parse GroupIncreaseEvent approve`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"group_increase","sub_type":"approve","group_id":2001,"user_id":1012,"operator_id":0}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupIncreaseEvent>(event)
        assertEquals("approve", event.subType)
        assertEquals(1012L, event.userId)
    }

    @Test
    fun `parse GroupBanEvent ban`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"group_ban","sub_type":"ban","group_id":2001,"user_id":1011,"operator_id":10001,"duration":600}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupBanEvent>(event)
        assertEquals("ban", event.subType)
        assertEquals(600L, event.duration)
    }

    @Test
    fun `parse FriendAddEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"friend_add","user_id":1002}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<FriendAddEvent>(event)
        assertEquals(1002L, event.userId)
    }

    @Test
    fun `parse GroupRecallEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"group_recall","group_id":2001,"user_id":1011,"operator_id":10001,"message_id":5}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupRecallEvent>(event)
        assertEquals(5L, event.messageId)
    }

    @Test
    fun `parse FriendRecallEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"friend_recall","user_id":1002,"message_id":10}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<FriendRecallEvent>(event)
        assertEquals(10L, event.messageId)
    }

    @Test
    fun `parse PokeEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"notify","sub_type":"poke","group_id":2001,"user_id":1011,"target_id":10001}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<PokeEvent>(event)
        assertEquals("poke", event.subType)
        assertEquals(10001L, event.targetId)
    }

    @Test
    fun `parse LuckyKingEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"notify","sub_type":"lucky_king","group_id":2001,"user_id":1011,"target_id":1012}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<LuckyKingEvent>(event)
    }

    @Test
    fun `parse HonorEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"notify","sub_type":"honor","group_id":2001,"honor_type":"talkative","user_id":1011}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<HonorEvent>(event)
        assertEquals("talkative", event.honorType)
    }

    // ==================== Request Events ====================

    @Test
    fun `parse FriendRequestEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"request","request_type":"friend","comment":"hello","flag":"abc123","user_id":1002}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<FriendRequestEvent>(event)
        assertEquals("hello", event.comment)
        assertEquals("abc123", event.flag)
    }

    @Test
    fun `parse GroupRequestEvent add`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"request","request_type":"group","sub_type":"add","comment":"","flag":"xyz","group_id":2001,"user_id":1002}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<GroupRequestEvent>(event)
        assertEquals("add", event.subType)
        assertEquals(2001L, event.groupId)
    }

    // ==================== Meta Events ====================

    @Test
    fun `parse LifecycleMetaEvent enable`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"meta_event","meta_event_type":"lifecycle","sub_type":"enable"}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<LifecycleMetaEvent>(event)
        assertEquals("enable", event.subType)
    }

    @Test
    fun `parse HeartbeatMetaEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"meta_event","meta_event_type":"heartbeat","status":{"online":true,"good":true},"interval":5000}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<HeartbeatMetaEvent>(event)
        assertEquals(true, event.status.online)
        assertEquals(5000L, event.interval)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `unknown post_type returns PolymorphicEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"unknown_type"}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<PolymorphicEvent>(event)
        assertEquals("unknown_type", event.postType)
    }

    @Test
    fun `unknown notice_type returns PolymorphicEvent`() {
        val input = """{"time":1710000000,"self_id":10001,"post_type":"notice","notice_type":"unknown_notice"}"""
        val event = json.decodeFromString(OneBotEventSerializer, input)
        assertIs<PolymorphicEvent>(event)
    }
}
