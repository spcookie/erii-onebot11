package uesugi.onebot.core.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.MiddlewareException
import uesugi.onebot.core.model.*
import uesugi.onebot.sdk.transport.impl.http.HttpActionClient
import uesugi.onebot.lib.transport.impl.http.HttpActionServer
import kotlin.test.*

class HttpActionIntegrationTest {

    private val serverPort = 25001

    private var server: HttpActionServer? = null

    // Returns Unit explicitly to avoid JUnit void-check issues
    private fun doTest(block: suspend (HttpActionClient) -> Unit) {
        runBlocking {
            val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort)
            server = HttpActionServer(config) { action, _ ->
                when (action) {
                    "send_private_msg" -> MessageIdResult(42)
                    "get_login_info" -> LoginInfo(userId = 10001L, nickname = "TestBot")
                    "get_status" -> StatusInfo(online = true, good = true)
                    else -> throw MiddlewareException(404, "unknown")
                }
            }
            server?.start()
            delay(300)
            try {
                val client = HttpActionClient(OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort))
                block(client)
            } finally {
                server?.stop()
                delay(300)
            }
        }
    }

    @Test
    fun `client calls server and receives response`() = doTest { client ->
        val result = client.call("send_private_msg", RawActionParams(buildJsonObject {
            put("user_id", 123L)
            put("message", buildJsonArray {
                add(buildJsonObject {
                    put("type", "text")
                    put("data", buildJsonObject { put("text", "hello") })
                })
            })
        }))
        assertTrue(result is MessageIdResult)
        assertEquals(42, (result as MessageIdResult).messageId)
    }

    @Test
    fun `client receives failed response for unregistered action`() = doTest { client ->
        val result = client.call("unknown_action", RawActionParams(buildJsonObject {}))
        assertTrue(result is RawActionResult)
        assertEquals(JsonNull, (result as RawActionResult).raw)
    }

    @Test
    fun `client sends JSON body params`() = doTest { client ->
        val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort)
        server?.stop()
        delay(300)

        runBlocking {
            var receivedParams: OneBotActionParams? = null
            val s = HttpActionServer(config) { _, params ->
                receivedParams = params
                RawActionResult()
            }
            s.start()
            delay(300)
            try {
                val c = HttpActionClient(OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort))
                c.call("set_group_ban", RawActionParams(buildJsonObject {
                    put("group_id", 555666777L)
                    put("user_id", 111222333L)
                    put("duration", 600L)
                }))
                assertTrue(receivedParams is SetGroupBanRequest)
                val req = receivedParams as SetGroupBanRequest
                assertEquals(555666777L, req.groupId)
                assertEquals(111222333L, req.userId)
                assertEquals(600L, req.duration)
            } finally {
                s.stop()
                delay(300)
            }
        }
    }

    @Test
    fun `server accepts request with correct access token`() {
        runBlocking {
            val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort, accessToken = "test-token")
            val s = HttpActionServer(config) { _, _ -> LoginInfo(userId = 10001L, nickname = "TestBot") }
            s.start()
            delay(300)
            try {
                val c = HttpActionClient(
                    OneBotConfig(
                        httpHost = "127.0.0.1",
                        httpPort = serverPort,
                        accessToken = "test-token"
                    )
                )
                val result = c.call("get_login_info", RawActionParams(buildJsonObject {}))
                assertTrue(result is LoginInfo)
            } finally {
                s.stop()
                delay(300)
            }
        }
    }

    @Test
    fun `server rejects request with wrong access token`() {
        runBlocking {
            val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort, accessToken = "correct-token")
            val s = HttpActionServer(config) { _, _ -> RawActionResult() }
            s.start()
            delay(300)
            try {
                val c = HttpActionClient(
                    OneBotConfig(
                        httpHost = "127.0.0.1",
                        httpPort = serverPort,
                        accessToken = "wrong-token"
                    )
                )
                val result = c.call("get_login_info", RawActionParams(buildJsonObject {}))
                assertTrue(result is RawActionResult, "Expected RawActionResult for auth failure")
            } finally {
                s.stop()
                delay(300)
            }
        }
    }

    @Test
    fun `server handles complex nested params`() {
        runBlocking {
            val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort)
            var capturedAction = ""
            var capturedParams: SendMsgRequest? = null
            val s = HttpActionServer(config) { action, params ->
                capturedAction = action
                capturedParams = params as SendMsgRequest
                RawActionResult(buildJsonObject { put("message_id", 1) })
            }
            s.start()
            delay(300)
            try {
                val c = HttpActionClient(OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort))
                c.call("send_msg", RawActionParams(buildJsonObject {
                    put("message_type", "group")
                    put("group_id", 123L)
                    put("message", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("data", buildJsonObject { put("text", "hello") })
                        })
                        add(buildJsonObject {
                            put("type", "at")
                            put("data", buildJsonObject { put("qq", "10001") })
                        })
                    })
                }))
                assertEquals("send_msg", capturedAction)
                assertEquals("group", capturedParams?.messageType)
                assertNotNull(capturedParams?.message)
                assertEquals(2, capturedParams?.message?.size)
                assertEquals("text", capturedParams?.message?.get(0)?.type)
            } finally {
                s.stop()
                delay(300)
            }
        }
    }
}
