package uesugi.onebot.app.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.MiddlewareException
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.Connection
import uesugi.onebot.lib.transport.ServerTransportBuilder
import uesugi.onebot.sdk.transport.SdkTransportBuilder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Connection 集成测试。
 *
 * 验证 Connection facade 的 Server/Client 模式 HTTP 传输集成。
 * 每个测试使用独立端口避免 BindException。
 */
class ConnectionIntegrationTest {

    private val portAllocator = AtomicInteger(26001)

    private suspend fun withServer(
        actionHandler: suspend (String, OneBotActionParams) -> OneBotActionResult,
        block: suspend (server: Connection, client: Connection) -> Unit
    ) {
        var lastError: Throwable? = null
        repeat(5) { attempt ->
            val port = portAllocator.incrementAndGet()
            val serverConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = port)
            val server = ServerTransportBuilder(serverConfig, actionHandler).build()
            try {
                server.start()
                delay(200)
                val clientConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = port)
                val client = SdkTransportBuilder(clientConfig).build()
                client.start()
                try {
                    block(server, client)
                    return
                } finally {
                    try { client.stop() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < 4) delay(100)
            } finally {
                try { server.stop() } catch (_: Exception) {}
                delay(200)
            }
        }
        throw lastError!!
    }

    // ===== Server Mode =====

    @Test
    fun `server responds to API calls`() = runBlocking {
        withServer({ action, _ ->
            when (action) {
                "get_login_info" -> LoginInfo(userId = 10001L, nickname = "TestBot")
                else -> throw MiddlewareException(404, "unknown")
            }
        }) { _, client ->
            val result = client.call("get_login_info", RawActionParams(buildJsonObject {}))
            assertTrue(result is LoginInfo)
            assertEquals(10001L, (result as LoginInfo).userId)
            assertEquals("TestBot", result.nickname)
        }
    }

    @Test
    fun `server dispatches multiple actions`() = runBlocking {
        withServer({ action, _ ->
            when (action) {
                "action_a" -> RawActionResult(buildJsonObject { put("id", 1) })
                "action_b" -> RawActionResult(buildJsonObject { put("id", 2) })
                else -> throw MiddlewareException(404, "unknown")
            }
        }) { _, client ->
            val resp1 = client.call("action_a", RawActionParams(buildJsonObject {}))
            assertEquals(1, (resp1 as RawActionResult).raw.jsonObject["id"]?.jsonPrimitive?.int)

            val resp2 = client.call("action_b", RawActionParams(buildJsonObject {}))
            assertEquals(2, (resp2 as RawActionResult).raw.jsonObject["id"]?.jsonPrimitive?.int)
        }
    }

    @Test
    fun `server forwards params to handler`() = runBlocking {
        var captured: OneBotActionParams? = null
        withServer({ _, params ->
            captured = params
            RawActionResult()
        }) { _, client ->
            client.call("set_group_ban", RawActionParams(buildJsonObject {
                put("group_id", 555L)
                put("user_id", 111L)
                put("duration", 600L)
            }))

            assertTrue(captured is SetGroupBanRequest)
            val req = captured as SetGroupBanRequest
            assertEquals(555L, req.groupId)
            assertEquals(111L, req.userId)
            assertEquals(600L, req.duration)
        }
    }

    @Test
    fun `server returns failed for unknown action`() = runBlocking {
        withServer({ _, _ -> throw MiddlewareException(404, "unknown") }) { _, client ->
            val result = client.call("unknown", RawActionParams(buildJsonObject {}))
            assertTrue(result is RawActionResult)
            assertEquals(JsonNull, (result as RawActionResult).raw)
        }
    }

    // ===== Client Mode =====

    @Test
    fun `client calls remote server`() = runBlocking {
        val serverPort = portAllocator.incrementAndGet()
        val serverConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = serverPort)

        val server = ServerTransportBuilder(serverConfig) { action, params ->
            when (action) {
                "get_group_info" -> {
                    val p = params as GetGroupInfoRequest
                    GroupInfo(groupId = p.groupId, groupName = "TestGroup")
                }
                else -> throw MiddlewareException(404, "unknown")
            }
        }.build()
        server.start()
        delay(300)
        try {
            val clientConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = serverPort)
            val client = SdkTransportBuilder(clientConfig).build()
            client.start()

            val result = client.call("get_group_info", RawActionParams(buildJsonObject { put("group_id", 123L) }))
            assertTrue(result is GroupInfo)
            assertEquals(123L, (result as GroupInfo).groupId)
            assertEquals("TestGroup", result.groupName)
        } finally {
            server.stop()
            delay(500)
        }
    }

    // ===== Auth =====

    @Test
    fun `server with token rejects wrong token`() {
        runBlocking {
            val port = portAllocator.incrementAndGet()
            val serverConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = port, accessToken = "correct-token")
            val server = ServerTransportBuilder(serverConfig) { _, _ -> RawActionResult() }.build()
            server.start()
            delay(300)
            try {
                val clientConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = port, accessToken = "wrong-token")
                val client = SdkTransportBuilder(clientConfig).build()
                client.start()

                val result = client.call("test", RawActionParams(buildJsonObject {}))
                assertTrue(result is RawActionResult, "Expected RawActionResult for auth failure")
            } finally {
                server.stop()
                delay(500)
            }
        }
    }

    // ===== Lifecycle =====

    @Test
    fun `start call and stop lifecycle`() = runBlocking {
        val port = portAllocator.incrementAndGet()
        val serverConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = port)
        val server = ServerTransportBuilder(serverConfig) { _, _ -> RawActionResult() }.build()
        server.start()
        delay(300)

        val clientConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = port)
        val client = SdkTransportBuilder(clientConfig).build()
        client.start()
        val result = client.call("test", RawActionParams(buildJsonObject {}))
        assertTrue(result is RawActionResult)
        client.stop()
        server.stop()
        delay(500)

        // After stop, calls fail
        val threw = try {
            client.call("test", RawActionParams(buildJsonObject {}))
            false
        } catch (e: Exception) {
            true
        }
        assertTrue(threw, "Expected exception after stop")
    }

    @Test
    fun `call without action channel throws error`() {
        val config = OneBotConfig(httpPort = portAllocator.incrementAndGet())
        val connection = Connection(config)

        assertFailsWith<IllegalStateException> {
            runBlocking { connection.call("test", RawActionParams(buildJsonObject {})) }
        }
    }

    @Test
    fun `pushEvent without event push channel throws error`() {
        val config = OneBotConfig(httpPort = portAllocator.incrementAndGet())
        val connection = Connection(config)

        assertFailsWith<IllegalStateException> {
            runBlocking { connection.pushEvent(PrivateMessageEvent(0, 0)) }
        }
    }
}
