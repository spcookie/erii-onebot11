package uesugi.onebot.core.model

import kotlinx.serialization.json.*
import uesugi.onebot.core.transport.JsonFactory
import java.awt.SystemColor.text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SerializationTest {

    private val json = JsonFactory.base

    // ===== MessageSegment =====

    @Test
    fun `serialize MessageSegment with snakeCase naming`() {
        val segment = text("Hello World")
        val encoded = json.encodeToString(MessageSegment.serializer(), segment)
        val decoded: MessageSegment = json.decodeFromString(MessageSegment.serializer(), encoded)

        assertEquals("text", decoded.type)
        assertEquals("Hello World", decoded.data["text"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serialize at segment`() {
        val segment = at(123456789L)
        val encoded = json.encodeToString(MessageSegment.serializer(), segment)
        assertNotNull(encoded)
        val decoded: MessageSegment = json.decodeFromString(MessageSegment.serializer(), encoded)

        assertEquals("at", decoded.type)
        assertEquals("123456789", decoded.data["qq"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serialize reply segment`() {
        val segment = reply(42L)
        val encoded = json.encodeToString(MessageSegment.serializer(), segment)

        val decoded: MessageSegment = json.decodeFromString(MessageSegment.serializer(), encoded)
        assertEquals("reply", decoded.type)
        assertEquals("42", decoded.data["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serialize image segment with optional url`() {
        val withUrl = image("file.jpg", url = "http://example.com/img.jpg")
        val withoutUrl = image("file.jpg")

        val encodedWith = json.encodeToString(MessageSegment.serializer(), withUrl)
        val encodedWithout = json.encodeToString(MessageSegment.serializer(), withoutUrl)

        val decodedWith: MessageSegment = json.decodeFromString(MessageSegment.serializer(), encodedWith)
        val decodedWithout: MessageSegment = json.decodeFromString(MessageSegment.serializer(), encodedWithout)

        assertEquals("http://example.com/img.jpg", decodedWith.data["url"]?.jsonPrimitive?.content)
        assertEquals(null, decodedWithout.data["url"])
    }

    // ===== PrivateMessageEvent =====

    @Test
    fun `deserialize PrivateMessageEvent from snakeCase JSON`() {
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
            "sender": {"user_id": 987654321, "nickname": "TestUser", "sex": "male", "age": 18}
        }
        """.trimIndent()

        val event = json.decodeFromString(PrivateMessageEvent.serializer(), raw)

        assertEquals(1700000000L, event.time)
        assertEquals(10001L, event.selfId)
        assertEquals("private", event.messageType)
        assertEquals("friend", event.subType)
        assertEquals(12345, event.messageId)
        assertEquals(987654321L, event.userId)
        assertEquals("hi", event.rawMessage)
        assertEquals("TestUser", event.sender.nickname)
        assertEquals("male", event.sender.sex)
    }

    @Test
    fun `serialize PrivateMessageEvent round-trip`() {
        val event = PrivateMessageEvent(
            time = 1700000000L,
            selfId = 10001L,
            subType = "friend",
            messageId = 12345,
            userId = 987654321L,
            message = listOf(text("hello")),
            rawMessage = "hello",
            sender = Sender(987654321L, "User")
        )

        val encoded = json.encodeToString(PrivateMessageEvent.serializer(), event)
        val decoded = json.decodeFromString(PrivateMessageEvent.serializer(), encoded)

        assertEquals(event.time, decoded.time)
        assertEquals(event.selfId, decoded.selfId)
        assertEquals(event.messageId, decoded.messageId)
        assertEquals(event.userId, decoded.userId)
        assertEquals(event.rawMessage, decoded.rawMessage)
    }

    // ===== GroupMessageEvent =====

    @Test
    fun `deserialize GroupMessageEvent from snakeCase JSON`() {
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
            "message": [{"type": "at", "data": {"qq": "10001"}}, {"type": "text", "data": {"text": " hello"}}],
            "raw_message": "[CQ:at,qq=10001] hello",
            "font": 0,
            "sender": {"user_id": 111222333, "nickname": "GroupUser", "sex": "unknown", "age": 0, "card": "CardName", "area": "Area", "level": "5", "role": "member", "title": "VIP"}
        }
        """.trimIndent()

        val event = json.decodeFromString(GroupMessageEvent.serializer(), raw)

        assertEquals(1700000000L, event.time)
        assertEquals(10001L, event.selfId)
        assertEquals("group", event.messageType)
        assertEquals("normal", event.subType)
        assertEquals(67890, event.messageId)
        assertEquals(555666777L, event.groupId)
        assertEquals(111222333L, event.userId)
        assertEquals("GroupUser", event.sender.nickname)
        assertEquals("CardName", event.sender.card)
        assertEquals("member", event.sender.role)
    }

    @Test
    fun `deserialize GroupMessageEvent with anonymous`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "message",
            "message_type": "group",
            "sub_type": "anonymous",
            "group_id": 555666777,
            "user_id": 0,
            "anonymous": {"id": 12345, "name": "AnonUser", "flag": "abc123"},
            "message": [],
            "raw_message": "",
            "sender": {"user_id": 0, "nickname": "", "card": ""}
        }
        """.trimIndent()

        val event = json.decodeFromString(GroupMessageEvent.serializer(), raw)

        assertEquals("anonymous", event.subType)
        assertNotNull(event.anonymous)
        assertEquals(12345L, event.anonymous!!.id)
        assertEquals("AnonUser", event.anonymous!!.name)
        assertEquals("abc123", event.anonymous!!.flag)
    }

    // ===== Notice Events =====

    @Test
    fun `deserialize GroupUploadEvent`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_upload",
            "group_id": 555666777,
            "user_id": 111222333,
            "file": {"id": "file-id-1", "name": "test.jpg", "size": 1024000, "busid": 123}
        }
        """.trimIndent()

        val event = json.decodeFromString(GroupUploadEvent.serializer(), raw)

        assertEquals("group_upload", event.noticeType)
        assertEquals(555666777L, event.groupId)
        assertEquals("test.jpg", event.file.name)
        assertEquals(1024000L, event.file.size)
    }

    @Test
    fun `deserialize GroupAdminEvent`() {
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

        val event = json.decodeFromString(GroupAdminEvent.serializer(), raw)
        assertEquals("group_admin", event.noticeType)
        assertEquals("set", event.subType)
        assertEquals(111222333L, event.userId)
    }

    @Test
    fun `deserialize GroupDecreaseEvent`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "notice",
            "notice_type": "group_decrease",
            "sub_type": "kick",
            "group_id": 555666777,
            "operator_id": 10001,
            "user_id": 111222333
        }
        """.trimIndent()

        val event = json.decodeFromString(GroupDecreaseEvent.serializer(), raw)
        assertEquals("group_decrease", event.noticeType)
        assertEquals("kick", event.subType)
        assertEquals(10001L, event.operatorId)
    }

    @Test
    fun `deserialize PokeEvent`() {
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

        val event = json.decodeFromString(PokeEvent.serializer(), raw)
        assertEquals("notify", event.noticeType)
        assertEquals("poke", event.subType)
        assertEquals(10001L, event.targetId)
    }

    @Test
    fun `deserialize LuckyKingEvent`() {
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

        val event = json.decodeFromString(LuckyKingEvent.serializer(), raw)
        assertEquals("lucky_king", event.subType)
    }

    @Test
    fun `deserialize HonorEvent`() {
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

        val event = json.decodeFromString(HonorEvent.serializer(), raw)
        assertEquals("honor", event.subType)
        assertEquals("talkative", event.honorType)
    }

    // ===== Request Events =====

    @Test
    fun `deserialize FriendRequestEvent`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "request",
            "request_type": "friend",
            "user_id": 111222333,
            "comment": "Hello, I am a fan!",
            "flag": "abc123flag"
        }
        """.trimIndent()

        val event = json.decodeFromString(FriendRequestEvent.serializer(), raw)
        assertEquals("friend", event.requestType)
        assertEquals(111222333L, event.userId)
        assertEquals("Hello, I am a fan!", event.comment)
        assertEquals("abc123flag", event.flag)
    }

    @Test
    fun `deserialize GroupRequestEvent`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "request",
            "request_type": "group",
            "sub_type": "invite",
            "group_id": 555666777,
            "user_id": 111222333,
            "comment": "",
            "flag": "inviteFlag"
        }
        """.trimIndent()

        val event = json.decodeFromString(GroupRequestEvent.serializer(), raw)
        assertEquals("group", event.requestType)
        assertEquals("invite", event.subType)
        assertEquals(555666777L, event.groupId)
    }

    // ===== Meta Events =====

    @Test
    fun `deserialize LifecycleMetaEvent`() {
        val raw = """
        {
            "time": 1700000000,
            "self_id": 10001,
            "post_type": "meta_event",
            "meta_event_type": "lifecycle",
            "sub_type": "connect"
        }
        """.trimIndent()

        val event = json.decodeFromString(LifecycleMetaEvent.serializer(), raw)
        assertEquals("lifecycle", event.metaEventType)
        assertEquals("connect", event.subType)
    }

    @Test
    fun `deserialize HeartbeatMetaEvent`() {
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

        val event = json.decodeFromString(HeartbeatMetaEvent.serializer(), raw)
        assertEquals("heartbeat", event.metaEventType)
        assertEquals(5000L, event.interval)
        assertEquals(true, event.status.online)
    }

    // ===== ActionRequest / ActionResponse =====

    @Test
    fun `serialize ActionRequest with snakeCase`() {
        val params = buildJsonObject { put("user_id", 123L); put("message", "hello") }
        val request = ActionRequest(action = "send_private_msg", params = params, echo = "echo-1")

        val encoded = json.encodeToString(ActionRequest.serializer(), request)
        val decoded = json.decodeFromString(ActionRequest.serializer(), encoded)

        assertEquals("send_private_msg", decoded.action)
        assertEquals("echo-1", decoded.echo)
        assertEquals("hello", decoded.params["message"]?.jsonPrimitive?.content)
    }

    @Test
    fun `ActionResponse ok factory`() {
        val data = buildJsonObject { put("message_id", 12345) }
        val response = ActionResponse.ok(data = data, echo = "echo-1")

        assertEquals(ActionResponse.STATUS_OK, response.status)
        assertEquals(0, response.retcode)
        assertEquals("echo-1", response.echo)
        assertEquals(12345, response.data.jsonObject["message_id"]?.jsonPrimitive?.int)
    }

    @Test
    fun `ActionResponse async factory`() {
        val response = ActionResponse.async("echo-2")

        assertEquals(ActionResponse.STATUS_ASYNC, response.status)
        assertEquals(1, response.retcode)
        assertEquals(JsonNull, response.data)
    }

    @Test
    fun `ActionResponse failed factory`() {
        val response = ActionResponse.failed(retcode = 1404, echo = "echo-3")

        assertEquals("failed", response.status)
        assertEquals(1404, response.retcode)
        assertEquals(JsonNull, response.data)
    }

    // ===== detailType extension =====

    @Test
    fun `detailType for message events`() {
        val event = PrivateMessageEvent(
            time = 0, selfId = 0, subType = "group"
        )
        assertEquals("group", event.detailType)
    }

    @Test
    fun `detailType for notice events`() {
        val event = GroupBanEvent(
            time = 0, selfId = 0, noticeType = "group_ban"
        )
        assertEquals("group_ban", event.detailType)
    }

    @Test
    fun `detailType for request events`() {
        val event = FriendRequestEvent(
            time = 0, selfId = 0, requestType = "friend"
        )
        assertEquals("friend", event.detailType)
    }

    @Test
    fun `detailType for meta events`() {
        val event = HeartbeatMetaEvent(
            time = 0, selfId = 0, metaEventType = "heartbeat"
        )
        assertEquals("heartbeat", event.detailType)
    }

    // ===== factory functions =====

    @Test
    fun `textSegment factory`() {
        val seg = text("hello")
        assertEquals("text", seg.type)
        assertEquals("hello", seg.data["text"]?.jsonPrimitive?.content)
    }

    @Test
    fun `atSegment factory`() {
        val seg = at(12345L)
        assertEquals("at", seg.type)
        assertEquals("12345", seg.data["qq"]?.jsonPrimitive?.content)
    }

    @Test
    fun `imageSegment factory with optional url`() {
        val seg = image("img.jpg")
        assertEquals("image", seg.type)
        assertEquals("img.jpg", seg.data["file"]?.jsonPrimitive?.content)
        assertEquals(null, seg.data["url"])
    }

    @Test
    fun `faceSegment factory`() {
        val seg = face("1")
        assertEquals("face", seg.type)
        assertEquals("1", seg.data["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `recordSegment factory`() {
        val seg = record("audio.mp3")
        assertEquals("record", seg.type)
        assertEquals("audio.mp3", seg.data["file"]?.jsonPrimitive?.content)
    }

    @Test
    fun `videoSegment factory`() {
        val seg = video("video.mp4")
        assertEquals("video", seg.type)
        assertEquals("video.mp4", seg.data["file"]?.jsonPrimitive?.content)
    }
}
