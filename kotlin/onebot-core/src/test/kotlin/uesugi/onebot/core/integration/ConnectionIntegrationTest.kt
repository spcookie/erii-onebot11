package uesugi.onebot.core.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.MiddlewareException
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.Connection
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
        block: suspend (Connection) -> Unit
    ) {
        var lastError: Throwable? = null
        repeat(5) { attempt ->
            val port = portAllocator.incrementAndGet()
            val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = port)
            val connection = Connection(config)
            connection.buildServer(actionHandler)
            try {
                connection.start()
                delay(200)
                block(connection)
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < 4) delay(100)
            } finally {
                try {
                    connection.stop()
                } catch (_: Exception) {
                }
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
        }) { connection ->
            val result = connection.call("get_login_info", RawActionParams(buildJsonObject {}))
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
        }) { connection ->
            val resp1 = connection.call("action_a", RawActionParams(buildJsonObject {}))
            assertEquals(1, (resp1 as RawActionResult).raw.jsonObject["id"]?.jsonPrimitive?.int)

            val resp2 = connection.call("action_b", RawActionParams(buildJsonObject {}))
            assertEquals(2, (resp2 as RawActionResult).raw.jsonObject["id"]?.jsonPrimitive?.int)
        }
    }

    @Test
    fun `server forwards params to handler`() = runBlocking {
        var captured: OneBotActionParams? = null
        withServer({ _, params ->
            captured = params
            RawActionResult()
        }) { connection ->
            connection.call("set_group_ban", RawActionParams(buildJsonObject {
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
        withServer({ _, _ -> throw MiddlewareException(404, "unknown") }) { connection ->
            val result = connection.call("unknown", RawActionParams(buildJsonObject {}))
            assertTrue(result is RawActionResult)
            assertEquals(JsonNull, (result as RawActionResult).raw)
        }
    }

    // ===== Client Mode =====

    @Test
    fun `client calls remote server`() = runBlocking {
        val serverPort = portAllocator.incrementAndGet()
        val serverConfig = OneBotConfig(httpHost = "127.0.0.1", httpPort = serverPort)

        val server = Connection(serverConfig)
        server.buildServer { action, params ->
            when (action) {
                "get_group_info" -> {
                    val p = params as GetGroupInfoRequest
                    GroupInfo(groupId = p.groupId, groupName = "TestGroup")
                }

                else -> throw MiddlewareException(404, "unknown")
            }
        }
        server.start()
        delay(300)
        try {
            val clientConfig = OneBotConfig(httpEnable = true, httpHost = "127.0.0.1", httpPort = serverPort)
            val client = Connection(clientConfig)
            client.buildClient()
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
            val serverConfig = OneBotConfig(httpHost = "127.0.0.1", httpPort = port, accessToken = "correct-token")
            val server = Connection(serverConfig)
            server.buildServer { _, _ -> RawActionResult() }
            server.start()
            delay(300)
            try {
                val clientConfig = OneBotConfig(httpHost = "127.0.0.1", httpPort = port, accessToken = "wrong-token")
                val client = Connection(clientConfig)
                client.buildClient()
                client.start()

                assertFailsWith<Exception> {
                    client.call("test", RawActionParams(buildJsonObject {}))
                }
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
        val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = port)
        val connection = Connection(config)
        connection.buildServer { _, _ -> RawActionResult() }

        assertTrue(connection.isInitialized)

        connection.start()
        delay(300)
        val result = connection.call("test", RawActionParams(buildJsonObject {}))
        assertTrue(result is RawActionResult)
        connection.stop()
        delay(500)

        // After stop, calls fail
        val threw = try {
            connection.call("test", RawActionParams(buildJsonObject {}))
            false
        } catch (e: Exception) {
            true
        }
        assertTrue(threw, "Expected exception after stop")
    }

    @Test
    fun `buildServer cannot be called twice`() {
        val config = OneBotConfig(httpPort = portAllocator.incrementAndGet())
        val connection = Connection(config)
        connection.buildServer { _, _ -> RawActionResult() }

        assertFailsWith<IllegalArgumentException> {
            connection.buildServer { _, _ -> RawActionResult() }
        }
    }

    @Test
    fun `call without build throws error`() {
        val config = OneBotConfig(httpPort = portAllocator.incrementAndGet())
        val connection = Connection(config)

        assertFailsWith<IllegalStateException> {
            runBlocking { connection.call("test", RawActionParams(buildJsonObject {})) }
        }
    }

    @Test
    fun `pushEvent without build throws error`() {
        val config = OneBotConfig(httpPort = portAllocator.incrementAndGet())
        val connection = Connection(config)

        assertFailsWith<IllegalStateException> {
            runBlocking { connection.pushEvent(PrivateMessageEvent(0, 0)) }
        }
    }
}
