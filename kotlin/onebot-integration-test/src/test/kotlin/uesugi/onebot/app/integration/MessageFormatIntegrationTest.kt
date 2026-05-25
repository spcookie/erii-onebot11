package uesugi.onebot.app.integration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.GroupMessageEvent
import uesugi.onebot.core.model.OneBotEvent
import uesugi.onebot.core.model.faceSegment
import uesugi.onebot.core.model.textSegment
import uesugi.onebot.mock.MockBot
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.api.getMsg
import uesugi.onebot.sdk.client.api.sendGroupMsg
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessageFormatIntegrationTest {

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

    private suspend fun setupWith(
        messageFormat: String
    ): Triple<MockBot, OneBotClient, suspend (String) -> Unit> {
        val mockBotConfig = OneBotConfig(
            httpEnable = true, httpHost = "127.0.0.1", httpPort = 5740,
            httpPostEnable = true, httpPostUrl = "http://127.0.0.1:5741",
            selfId = 10001, messageFormat = messageFormat
        )
        val mockBot = MockBot(mockBotConfig)
        mockBot.addUser(10086, "Alice")
        mockBot.addGroup(12345, "Test Group")
        mockBot.addGroupMember(12345, 10001, "Bot")
        mockBot.addGroupMember(12345, 10086, "Alice")

        mockBot.start()
        delay(500)

        val clientConfig = OneBotConfig(
            httpEnable = true, httpHost = "127.0.0.1", httpPort = 5740,
            httpPostEnable = true, httpPostHost = "127.0.0.1", httpPostPort = 5741,
            selfId = 10001
        )
        val client = OneBotClient(clientConfig)
        client.start()
        delay(500)

        val cleanup: suspend (String) -> Unit = { label ->
            try {
                client.stop()
            } catch (_: Exception) {
            }
            try {
                mockBot.stop()
            } catch (_: Exception) {
            }
        }
        return Triple(mockBot, client, cleanup)
    }

    @Test
    fun testReceiveMessageAsArray() = runBlocking {
        val (mockBot, client, cleanup) = setupWith("array")
        try {
            val captured = captureEvent<GroupMessageEvent>(client)
            mockBot.simulateGroupMessage(12345, 10086, "hello array")
            val event = withTimeout(3000) { captured.await() }
            assertTrue(event.message.isNotEmpty(), "message array should not be empty")
            assertEquals("text", event.message[0].type)
        } finally {
            cleanup("array")
        }
    }

    @Test
    fun testReceiveMessageAsString() = runBlocking {
        val (mockBot, client, cleanup) = setupWith("string")
        try {
            val captured = captureEvent<GroupMessageEvent>(client)
            mockBot.simulateGroupMessage(12345, 10086, "hello string")
            val event = withTimeout(3000) { captured.await() }
            assertTrue(event.message.isNotEmpty(), "message should be parsed to List by SDK")
            assertEquals("text", event.message[0].type)
        } finally {
            cleanup("string")
        }
    }

    @Test
    fun testRawMessageAlwaysCqCodeString() = runBlocking {
        val (mockBot, client, cleanup) = setupWith("array")
        try {
            val captured = captureEvent<GroupMessageEvent>(client)
            mockBot.simulateGroupMessage(12345, 10086, "hello world")
            val event = withTimeout(3000) { captured.await() }
            assertTrue(event.rawMessage.isNotEmpty())
            assertTrue(event.rawMessage.contains("hello world"))
        } finally {
            cleanup("rawMessage")
        }
    }

    @Test
    fun testApiCallWithMultipleSegments() = runBlocking {
        val (mockBot, client, cleanup) = setupWith("array")
        try {
            val msgId = client.sendGroupMsg(
                12345, listOf(
                    textSegment("mixed "),
                    faceSegment("178"),
                    textSegment(" content")
                )
            )
            assertTrue(msgId > 0)
            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            assertTrue(msg.message.any { it.type == "face" })
            assertTrue(msg.message.any { it.type == "text" })
        } finally {
            cleanup("multi")
        }
    }
}
