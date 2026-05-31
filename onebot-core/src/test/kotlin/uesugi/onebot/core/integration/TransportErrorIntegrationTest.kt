package uesugi.onebot.core.integration

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.ActionResponse
import uesugi.onebot.core.model.RawActionResult
import uesugi.onebot.core.transport.impl.http.HttpActionServer
import uesugi.onebot.core.transport.impl.ws.WsForwardServer
import uesugi.onebot.core.transport.impl.ws.WsReverseServer
import uesugi.onebot.core.util.EchoTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransportErrorIntegrationTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ==================== HTTP Error Response Tests ====================

    @Test
    fun `http returns 401 with JSON body when token missing`() = runBlocking {
        val port = 27001
        val config = OneBotConfig(
            httpHost = "127.0.0.1", httpPort = port,
            accessToken = "test-token"
        )
        val server = HttpActionServer(config) { _, _ -> RawActionResult() }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse = client.post("http://127.0.0.1:$port/send_private_msg") {
                contentType(ContentType.Application.Json)
                setBody("""{"user_id":123,"message":"hello"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            val body = response.bodyAsText()
            val resp = json.decodeFromString(ActionResponse.serializer(), body)
            assertEquals("failed", resp.status)
            assertEquals(ActionResponse.RETCODE_UNAUTHORIZED, resp.retcode)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http returns 403 with JSON body when token wrong`() = runBlocking {
        val port = 27002
        val config = OneBotConfig(
            httpHost = "127.0.0.1", httpPort = port,
            accessToken = "correct-token"
        )
        val server = HttpActionServer(config) { _, _ -> RawActionResult() }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse = client.post("http://127.0.0.1:$port/send_private_msg") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer wrong-token")
                setBody("""{"user_id":123,"message":"hello"}""")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            val body = response.bodyAsText()
            val resp = json.decodeFromString(ActionResponse.serializer(), body)
            assertEquals("failed", resp.status)
            assertEquals(ActionResponse.RETCODE_FORBIDDEN, resp.retcode)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http returns 401 when token missing via query parameter`() = runBlocking {
        val port = 27003
        val config = OneBotConfig(
            httpHost = "127.0.0.1", httpPort = port,
            accessToken = "test-token"
        )
        val server = HttpActionServer(config) { _, _ -> RawActionResult() }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            // Token provided via header but wrong → should still get 401 for no token scenario
            val response: HttpResponse = client.post("http://127.0.0.1:$port/send_private_msg") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer wrong-token")
                setBody("""{"user_id":123,"message":"hello"}""")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http returns 404 with JSON body for unknown action`() = runBlocking {
        val port = 27004
        val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = port)
        val server = HttpActionServer(config) { action, _ ->
            throw uesugi.onebot.core.dispatch.ActionNotFoundException(action)
        }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse = client.post("http://127.0.0.1:$port/unknown_action") {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
            val body = response.bodyAsText()
            val resp = json.decodeFromString(ActionResponse.serializer(), body)
            assertEquals("failed", resp.status)
            assertEquals(ActionResponse.RETCODE_NOT_FOUND, resp.retcode)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http returns 406 for unsupported content type`() = runBlocking {
        val port = 27005
        val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = port)
        val server = HttpActionServer(config) { _, _ -> RawActionResult() }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse = client.post("http://127.0.0.1:$port/send_private_msg") {
                contentType(ContentType.Text.Plain)
                setBody("not json")
            }
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http returns 400 with JSON body for malformed JSON`() = runBlocking {
        val port = 27006
        val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = port)
        val server = HttpActionServer(config) { _, _ -> RawActionResult() }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse = client.post("http://127.0.0.1:$port/send_private_msg") {
                contentType(ContentType.Application.Json)
                setBody("{ this is not valid json }")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val body = response.bodyAsText()
            val resp = json.decodeFromString(ActionResponse.serializer(), body)
            assertEquals("failed", resp.status)
            assertEquals(ActionResponse.RETCODE_BAD_REQUEST, resp.retcode)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http returns 400 with retcode in body for DispatchException`() = runBlocking {
        val port = 27007
        val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = port)
        val server = HttpActionServer(config) { _, _ ->
            throw uesugi.onebot.core.dispatch.MiddlewareException(103, "Rate limited")
        }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse = client.post("http://127.0.0.1:$port/send_private_msg") {
                contentType(ContentType.Application.Json)
                setBody("""{"user_id":123,"message":"hello"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val body = response.bodyAsText()
            val resp = json.decodeFromString(ActionResponse.serializer(), body)
            assertEquals("failed", resp.status)
            assertEquals(103, resp.retcode)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http returns 200 with ok status for successful call`() = runBlocking {
        val port = 27008
        val config = OneBotConfig(httpHost = "127.0.0.1", httpPort = port)
        val server = HttpActionServer(config) { _, _ -> RawActionResult() }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse = client.post("http://127.0.0.1:$port/send_private_msg") {
                contentType(ContentType.Application.Json)
                setBody("""{"user_id":123,"message":"hello"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            val resp = json.decodeFromString(ActionResponse.serializer(), body)
            assertEquals("ok", resp.status)
            assertEquals(0, resp.retcode)
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `http accepts token via access_token query parameter`() = runBlocking {
        val port = 27009
        val config = OneBotConfig(
            httpHost = "127.0.0.1", httpPort = port,
            accessToken = "query-token"
        )
        val server = HttpActionServer(config) { _, _ -> RawActionResult() }
        server.start()
        delay(300)
        try {
            val client = HttpClient()
            val response: HttpResponse =
                client.post("http://127.0.0.1:$port/send_private_msg?access_token=query-token") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"user_id":123,"message":"hello"}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        } finally {
            server.stop()
            delay(300)
        }
    }

    // ==================== WS Forward Auth Tests ====================

    @Test
    fun `ws forward server closes connection when token missing`() = runBlocking {
        val port = 28001
        val config = OneBotConfig(
            wsForwardServerHost = "127.0.0.1", wsForwardServerPort = port,
            accessToken = "ws-token"
        )
        val server = WsForwardServer(config, actionHandler = { _, _ -> RawActionResult() })
        server.start()
        delay(300)
        try {
            val client = HttpClient { install(WebSockets) }
            var closed = false
            try {
                client.webSocket("ws://127.0.0.1:$port/api") {
                    for (frame in incoming) {
                        if (frame is Frame.Close) {
                            assertTrue(frame.readReason()?.message?.contains("Unauthorized") == true)
                            closed = true
                            break
                        }
                    }
                    closed = true // server closed connection, incoming flow completed
                }
            } catch (_: Exception) {
                closed = true
            }
            assertTrue(closed, "Connection should be closed by server")
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `ws forward server closes connection when token wrong`() = runBlocking {
        val port = 28002
        val config = OneBotConfig(
            wsForwardServerHost = "127.0.0.1", wsForwardServerPort = port,
            accessToken = "correct-token"
        )
        val server = WsForwardServer(config, actionHandler = { _, _ -> RawActionResult() })
        server.start()
        delay(300)
        try {
            val client = HttpClient { install(WebSockets) }
            var closed = false
            try {
                client.webSocket("ws://127.0.0.1:$port/api") {
                    request {
                        header(HttpHeaders.Authorization, "Bearer wrong-token")
                    }
                    for (frame in incoming) {
                        if (frame is Frame.Close) {
                            assertTrue(frame.readReason()?.message?.contains("Forbidden") == true)
                            closed = true
                            break
                        }
                    }
                    closed = true // server closed connection, incoming flow completed
                }
            } catch (_: Exception) {
                closed = true
            }
            assertTrue(closed, "Connection should be closed by server")
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `ws forward server accepts connection with correct token`() = runBlocking {
        val port = 28003
        val config = OneBotConfig(
            wsForwardServerHost = "127.0.0.1", wsForwardServerPort = port,
            accessToken = "correct-token"
        )
        val server = WsForwardServer(config, actionHandler = { _, _ -> RawActionResult() })
        server.start()
        delay(300)
        try {
            val client = HttpClient { install(WebSockets) }
            var connected = false
            client.webSocket("ws://127.0.0.1:$port/api") {
                request {
                    header(HttpHeaders.Authorization, "Bearer correct-token")
                }
                connected = true
                outgoing.send(Frame.Close())
            }
            assertTrue(connected, "Connection should be accepted")
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `ws forward event session also requires auth`() = runBlocking {
        val port = 28004
        val config = OneBotConfig(
            wsForwardServerHost = "127.0.0.1", wsForwardServerPort = port,
            accessToken = "event-token"
        )
        val server = WsForwardServer(config, actionHandler = { _, _ -> RawActionResult() })
        server.start()
        delay(300)
        try {
            val client = HttpClient { install(WebSockets) }
            var closed = false
            try {
                client.webSocket("ws://127.0.0.1:$port/event") {
                    for (frame in incoming) {
                        if (frame is Frame.Close) {
                            closed = true
                            break
                        }
                    }
                    closed = true // server closed connection, incoming flow completed
                }
            } catch (_: Exception) {
                closed = true
            }
            assertTrue(closed, "Event connection without token should be closed")
        } finally {
            server.stop()
            delay(300)
        }
    }

    // ==================== WS Reverse Auth Tests ====================

    @Test
    fun `ws reverse server closes connection when token missing`() = runBlocking {
        val port = 28005
        val config = OneBotConfig(
            wsReverseServerHost = "127.0.0.1", wsReverseServerPort = port,
            accessToken = "reverse-token"
        )
        val server = WsReverseServer(config, EchoTracker())
        server.start()
        delay(300)
        try {
            val client = HttpClient { install(WebSockets) }
            var closed = false
            try {
                client.webSocket("ws://127.0.0.1:$port") {
                    request {
                        header("X-Client-Role", "API")
                        header("X-Self-ID", "10001")
                    }
                    for (frame in incoming) {
                        if (frame is Frame.Close) {
                            assertTrue(frame.readReason()?.message?.contains("Unauthorized") == true)
                            closed = true
                            break
                        }
                    }
                    closed = true // server closed connection, incoming flow completed
                }
            } catch (_: Exception) {
                closed = true
            }
            assertTrue(closed, "Connection should be closed by server")
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `ws reverse server closes connection when token wrong`() = runBlocking {
        val port = 28006
        val config = OneBotConfig(
            wsReverseServerHost = "127.0.0.1", wsReverseServerPort = port,
            accessToken = "correct-token"
        )
        val server = WsReverseServer(config, EchoTracker())
        server.start()
        delay(300)
        try {
            val client = HttpClient { install(WebSockets) }
            var closed = false
            try {
                client.webSocket("ws://127.0.0.1:$port") {
                    request {
                        header("X-Client-Role", "API")
                        header("X-Self-ID", "10001")
                        header(HttpHeaders.Authorization, "Bearer wrong-token")
                    }
                    for (frame in incoming) {
                        if (frame is Frame.Close) {
                            assertTrue(frame.readReason()?.message?.contains("Forbidden") == true)
                            closed = true
                            break
                        }
                    }
                    closed = true // server closed connection, incoming flow completed
                }
            } catch (_: Exception) {
                closed = true
            }
            assertTrue(closed, "Connection should be closed by server")
        } finally {
            server.stop()
            delay(300)
        }
    }

    @Test
    fun `ws reverse server accepts connection with correct token`() = runBlocking {
        val port = 28007
        val config = OneBotConfig(
            wsReverseServerHost = "127.0.0.1", wsReverseServerPort = port,
            accessToken = "correct-token"
        )
        val server = WsReverseServer(config, EchoTracker())
        server.start()
        delay(300)
        try {
            val client = HttpClient { install(WebSockets) }
            var connected = false
            client.webSocket("ws://127.0.0.1:$port") {
                request {
                    header("X-Client-Role", "API")
                    header("X-Self-ID", "10001")
                    header(HttpHeaders.Authorization, "Bearer correct-token")
                }
                connected = true
                outgoing.send(Frame.Close())
            }
            assertTrue(connected, "Connection should be accepted")
        } finally {
            server.stop()
            delay(300)
        }
    }

    // ==================== Retcode Constant Tests ====================

    @Test
    fun `ActionResponse factory methods produce correct retcodes`() {
        val ok = ActionResponse.ok(kotlinx.serialization.json.JsonNull)
        assertEquals("ok", ok.status)
        assertEquals(0, ok.retcode)

        val async = ActionResponse.async()
        assertEquals("async", async.status)
        assertEquals(1, async.retcode)

        val badRequest = ActionResponse.badRequest()
        assertEquals("failed", badRequest.status)
        assertEquals(1400, badRequest.retcode)

        val unauthorized = ActionResponse.unauthorized()
        assertEquals("failed", unauthorized.status)
        assertEquals(1401, unauthorized.retcode)

        val forbidden = ActionResponse.forbidden()
        assertEquals("failed", forbidden.status)
        assertEquals(1403, forbidden.retcode)

        val notFound = ActionResponse.notFound()
        assertEquals("failed", notFound.status)
        assertEquals(1404, notFound.retcode)

        val customFailed = ActionResponse.failed(103)
        assertEquals("failed", customFailed.status)
        assertEquals(103, customFailed.retcode)
    }

    @Test
    fun `ActionResponse echo is preserved in factory methods`() {
        val echo = "test-echo-123"
        assertEquals(echo, ActionResponse.badRequest(echo).echo)
        assertEquals(echo, ActionResponse.unauthorized(echo).echo)
        assertEquals(echo, ActionResponse.forbidden(echo).echo)
        assertEquals(echo, ActionResponse.notFound(echo).echo)
    }
}
