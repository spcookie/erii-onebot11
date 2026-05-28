package uesugi.onebot.app.integration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

class WsForwardIntegrationTest {

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

    @Test
    fun testWsSendAndReceiveMessage() = runBlocking {
        val mockBotConfig = OneBotConfig(
            wsForwardServerEnable = true, wsForwardServerHost = "127.0.0.1", wsForwardServerPort = 6710,
            selfId = 10001
        )
        val mockBot = MockBot(mockBotConfig)
        mockBot.addUser(10086, "Alice")

        mockBot.start()
        delay(500)

        val clientConfig = OneBotConfig(
            wsForwardClientEnable = true, wsForwardClientUseUniversal = true,
            wsForwardClientUrl = "ws://127.0.0.1:6710",
            selfId = 10001
        )
        val client = OneBotClient(clientConfig)

        client.start()
        delay(1000)

        try {
            val msgId = client.sendPrivateMsg(10086, listOf(textSegment("hello via ws")))
            assertTrue(msgId > 0, "WS API call should return valid message_id")

            val eventCaptured = captureEvent<PrivateMessageEvent>(client)
            mockBot.simulatePrivateMessage(10086, "ws event test")
            val event = withTimeout(5000) { eventCaptured.await() }
            assertEquals(10086, event.userId)
            assertTrue(event.message.any { it.data["text"]?.jsonPrimitive?.content?.contains("ws event test") == true })
        } finally {
            client.stop()
            mockBot.stop()
        }
    }

    @Test
    fun testWsLifecycleConnectEvent() = runBlocking {
        val mockBotConfig = OneBotConfig(
            wsForwardServerEnable = true, wsForwardServerHost = "127.0.0.1", wsForwardServerPort = 6710,
            selfId = 10001
        )
        val mockBot = MockBot(mockBotConfig)

        mockBot.start()
        delay(500)

        val clientConfig = OneBotConfig(
            wsForwardClientEnable = true, wsForwardClientUseUniversal = true,
            wsForwardClientUrl = "ws://127.0.0.1:6710",
            selfId = 10001
        )
        val client = OneBotClient(clientConfig)

        val captured = captureEvent<LifecycleMetaEvent>(client) { it.subType == "connect" }
        client.start()
        delay(1000)

        try {
            val event = withTimeout(5000) { captured.await() }
            assertEquals("lifecycle", event.metaEventType)
            assertEquals("connect", event.subType)
        } finally {
            client.stop()
            mockBot.stop()
        }
    }

    @Test
    fun testWsHeartbeatEvent() = runBlocking {
        val mockBotConfig = OneBotConfig(
            wsForwardServerEnable = true, wsForwardServerHost = "127.0.0.1", wsForwardServerPort = 6710,
            heartbeatEnable = true, heartbeatInterval = 2000,
            selfId = 10001
        )
        val mockBot = MockBot(mockBotConfig)

        mockBot.start()
        delay(500)

        val clientConfig = OneBotConfig(
            wsForwardClientEnable = true, wsForwardClientUseUniversal = true,
            wsForwardClientUrl = "ws://127.0.0.1:6710",
            selfId = 10001
        )
        val client = OneBotClient(clientConfig)

        val captured = captureEvent<HeartbeatMetaEvent>(client)
        client.start()
        delay(1000)

        try {
            val event = withTimeout(8000) { captured.await() }
            assertEquals("heartbeat", event.metaEventType)
            assertEquals(2000, event.interval)
        } finally {
            client.stop()
            mockBot.stop()
        }
    }

    @Test
    fun testWsGroupMessageRoundTrip() = runBlocking {
        val mockBotConfig = OneBotConfig(
            wsForwardServerEnable = true, wsForwardServerHost = "127.0.0.1", wsForwardServerPort = 6710,
            selfId = 10001
        )
        val mockBot = MockBot(mockBotConfig)
        mockBot.addGroup(12345, "WS Test Group")
        mockBot.addGroupMember(12345, 10001, "Bot")
        mockBot.addGroupMember(12345, 10086, "Alice")

        mockBot.start()
        delay(500)

        val clientConfig = OneBotConfig(
            wsForwardClientEnable = true, wsForwardClientUseUniversal = true,
            wsForwardClientUrl = "ws://127.0.0.1:6710",
            selfId = 10001
        )
        val client = OneBotClient(clientConfig)

        client.start()
        delay(1000)

        try {
            val msgId = client.sendGroupMsg(
                12345, listOf(
                    textSegment("Hello "),
                    atSegment(10086),
                    textSegment(" via WebSocket")
                )
            )
            assertTrue(msgId > 0)

            val msg = client.getMsg(msgId)
            assertNotNull(msg)
            assertEquals("group", msg.messageType)
        } finally {
            client.stop()
            mockBot.stop()
        }
    }
}
