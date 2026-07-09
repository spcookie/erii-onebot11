package uesugi.onebot.app.integration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.mock.MockBot
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.api.getMsg
import uesugi.onebot.sdk.client.api.sendGroupMsg
import uesugi.onebot.sdk.client.api.sendPrivateMsg
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessageSegmentIntegrationTest {

    private inline fun <reified T : OneBotEvent> captureEvent(
        client: OneBotClient,
        crossinline predicate: (T) -> Boolean = { true }
    ): CompletableDeferred<T> {
        val deferred = CompletableDeferred<T>()
        listOf("message", "notice", "request", "meta_event").forEach { postType ->
            client.onEvent(postType) { event ->
                if (event is T && predicate(event) && !deferred.isCompleted) {
                    deferred.complete(event)
                }
            }
        }
        return deferred
    }

    private suspend fun setup(): Triple<MockBot, OneBotClient, () -> Unit> {
        val mockBotConfig = OneBotConfig(
            httpEnable = true, httpHost = "127.0.0.1", httpPort = 5750,
            httpPostEnable = true, httpPostUrl = "http://127.0.0.1:5751",
            selfId = 10001
        )
        val mockBot = MockBot(mockBotConfig)
        mockBot.addUser(10086, "Alice")
        mockBot.addUser(10087, "Bob")
        mockBot.addGroup(12345, "Test Group")
        mockBot.addGroupMember(12345, 10001, "Bot")
        mockBot.addGroupMember(12345, 10086, "Alice")
        mockBot.addGroupMember(12345, 10087, "Bob")
        mockBot.addFriend(10086, "Alice")

        mockBot.start()
        delay(500)

        val clientConfig = OneBotConfig(
            httpEnable = true, httpHost = "127.0.0.1", httpPort = 5750,
            httpPostEnable = true, httpPostHost = "127.0.0.1", httpPostPort = 5751,
            selfId = 10001
        )
        val client = OneBotClient(clientConfig)
        client.start()
        delay(500)

        val cleanup = {
            runBlocking {
                try {
                    client.stop()
                } catch (_: Exception) {
                }
                try {
                    mockBot.stop()
                } catch (_: Exception) {
                }
            }
        }
        return Triple(mockBot, client, cleanup)
    }

    @Test
    fun testTextSegment() = runBlocking {
        val (mockBot, client, cleanup) = setup()
        try {
            val captured = captureEvent<PrivateMessageEvent>(client)
            mockBot.simulatePrivateMessage(10086, listOf(textSegment("plain text message")))
            val event = withTimeout(3000) { captured.await() }
            assertEquals(1, event.message.size)
            assertEquals("text", event.message[0].type)
            assertEquals("plain text message", event.message[0].data["text"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testFaceSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(10086, listOf(faceSegment("178")))
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            val face = msg.message.find { it.type == "face" }
            assertNotNull(face)
            assertEquals("178", face.data["id"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testAtSegment() = runBlocking {
        val (mockBot, client, cleanup) = setup()
        try {
            val captured = captureEvent<GroupMessageEvent>(client)
            mockBot.simulateGroupMessage(12345, 10086, listOf(textSegment("@someone hello")))
            val event = withTimeout(3000) { captured.await() }
            assertTrue(event.message.any { it.type == "text" })
        } finally {
            cleanup()
        }
    }

    @Test
    fun testAtAllSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendGroupMsg(
                12345, listOf(
                    MessageSegment("at", mapOf("qq" to JsonPrimitive("all"))),
                    textSegment(" everyone")
                )
            )
            assertTrue(msgId > 0)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testImageSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    imageSegment("https://example.com/img.jpg")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            val img = msg.message.find { it.type == "image" }
            assertNotNull(img)
            assertEquals("https://example.com/img.jpg", img.data["file"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testRecordSegment() {
        runBlocking {
            val (_, client, cleanup) = setup()
            try {
                val msgId = client.sendPrivateMsg(
                    10086, listOf(
                        recordSegment("https://example.com/audio.mp3")
                    )
                )
                assertTrue(msgId > 0)
                val msg = client.getMsg(msgId)
                assertNotNull(msg)
                val record = msg.message.find { it.type == "record" }
                assertNotNull(record)
            } finally {
                cleanup()
            }
        }
    }

    @Test
    fun testVideoSegment() {
        runBlocking {
            val (_, client, cleanup) = setup()
            try {
                val msgId = client.sendPrivateMsg(
                    10086, listOf(
                        videoSegment("https://example.com/video.mp4")
                    )
                )
                assertTrue(msgId > 0)
                val msg = client.getMsg(msgId)
                assertNotNull(msg)
                val video = msg.message.find { it.type == "video" }
                assertNotNull(video)
            } finally {
                cleanup()
            }
        }
    }

    @Test
    fun testRpsSegment() {
        runBlocking {
            val (_, client, cleanup) = setup()
            try {
                val msgId = client.sendPrivateMsg(10086, listOf(rpsSegment()))
                assertTrue(msgId > 0)
                val msg = client.getMsg(msgId)
                val rps = msg?.message?.find { it.type == "rps" }
                assertNotNull(rps)
            } finally {
                cleanup()
            }
        }
    }

    @Test
    fun testDiceSegment() {
        runBlocking {
            val (_, client, cleanup) = setup()
            try {
                val msgId = client.sendPrivateMsg(10086, listOf(diceSegment()))
                assertTrue(msgId > 0)
                val msg = client.getMsg(msgId)
                val dice = msg?.message?.find { it.type == "dice" }
                assertNotNull(dice)
            } finally {
                cleanup()
            }
        }
    }

    @Test
    fun testShakeSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(10086, listOf(shakeSegment()))
            assertTrue(msgId > 0)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testPokeSegment() {
        runBlocking {
            val (_, client, cleanup) = setup()
            try {
                val msgId = client.sendPrivateMsg(10086, listOf(pokeSegment("1", "-1")))
                assertTrue(msgId > 0)
                val msg = client.getMsg(msgId)
                val poke = msg?.message?.find { it.type == "poke" }
                assertNotNull(poke)
            } finally {
                cleanup()
            }
        }
    }

    @Test
    fun testShareSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    shareSegment("https://example.com", "Title", "Content", "https://example.com/img.jpg")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            val share = msg?.message?.find { it.type == "share" }
            assertNotNull(share)
            assertEquals("https://example.com", share.data["url"]?.jsonPrimitive?.content)
            assertEquals("Title", share.data["title"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testContactSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(10086, listOf(contactSegment("qq", 10087)))
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            val contact = msg?.message?.find { it.type == "contact" }
            assertNotNull(contact)
            assertEquals("qq", contact.data["type"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testLocationSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    locationSegment(39.9042, 116.4074, "Beijing", "Capital of China")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            val loc = msg?.message?.find { it.type == "location" }
            assertNotNull(loc)
            assertEquals("39.9042", loc.data["lat"]?.jsonPrimitive?.content)
            assertEquals("116.4074", loc.data["lon"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testMusicSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    musicSegment("163", "123456")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            val music = msg?.message?.find { it.type == "music" }
            assertNotNull(music)
            assertEquals("163", music.data["type"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testMusicCustomSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    musicSegment(
                        "custom", url = "https://example.com/music.mp3",
                        audio = "https://example.com/music.mp3",
                        title = "Custom Song", content = "Artist"
                    )
                )
            )
            assertTrue(msgId > 0)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testReplySegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    replySegment(42),
                    textSegment("reply text")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            val reply = msg?.message?.find { it.type == "reply" }
            assertNotNull(reply)
            assertEquals("42", reply.data["id"]?.jsonPrimitive?.content)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testForwardSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(10086, listOf(forwardSegment("merge_123")))
            assertTrue(msgId > 0)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testNodeSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(10086, listOf(nodeSegment("12345")))
            assertTrue(msgId > 0)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testXmlSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    xmlSegment("<msg><text>hello</text></msg>")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            val xml = msg?.message?.find { it.type == "xml" }
            assertNotNull(xml)
            assertTrue(xml.data["data"]?.jsonPrimitive?.content?.contains("hello") == true)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testJsonSegment() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val msgId = client.sendPrivateMsg(
                10086, listOf(
                    jsonSegment("""{"app":"com.example"}""")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            val json = msg?.message?.find { it.type == "json" }
            assertNotNull(json)
            assertTrue(json.data["data"]?.jsonPrimitive?.content?.contains("com.example") == true)
        } finally {
            cleanup()
        }
    }

    @Test
    fun testMultipleSegments() = runBlocking {
        val (_, client, cleanup) = setup()
        try {
            val segments = listOf(
                textSegment("Hello "),
                faceSegment("178"),
                textSegment(" check this: "),
                imageSegment("https://example.com/img.jpg"),
                textSegment(" and reply to "),
                atSegment(10087)
            )
            val msgId = client.sendGroupMsg(12345, segments)
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            val types = msg.message.map { it.type }
            assertTrue(types.contains("text"))
            assertTrue(types.contains("face"))
            assertTrue(types.contains("image"))
            assertTrue(types.contains("at"))
        } finally {
            cleanup()
        }
    }
}
