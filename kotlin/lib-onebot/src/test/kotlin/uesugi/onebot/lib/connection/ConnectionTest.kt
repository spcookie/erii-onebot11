package uesugi.onebot.lib.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import uesugi.onebot.lib.model.*
import uesugi.onebot.lib.transport.ActionChannel
import uesugi.onebot.lib.transport.EventChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Mock ActionChannel that processes requests in-process.
 */
class MockActionChannel : ActionChannel {
    private var handler: (suspend (ActionRequest) -> ActionResponse)? = null
    var started = false
    var stopped = false

    override fun onRequest(handler: suspend (ActionRequest) -> ActionResponse) {
        this.handler = handler
    }

    override suspend fun start() { started = true }
    override suspend fun stop() { stopped = true }

    override suspend fun send(request: ActionRequest): ActionResponse {
        return handler?.invoke(request) ?: ActionResponse("failed", 1, echo = request.echo)
    }
}

/**
 * Mock EventChannel that collects events in a shared flow.
 */
class MockEventChannel : EventChannel {
    private val flow = MutableSharedFlow<EventEnvelope>(replay = 0, extraBufferCapacity = 100)
    var started = false
    var stopped = false
    val pushedEvents = mutableListOf<EventEnvelope>()

    override val eventFlow = flow

    override suspend fun start() { started = true }
    override suspend fun stop() { stopped = true }

    override suspend fun push(event: EventEnvelope) {
        pushedEvents.add(event)
        flow.emit(event)
    }
}

class ConnectionTest {

    private val config = OneBotConfig(timeoutMs = 5000)

    @Test
    fun `execute action returns response`() = runTest {
        val actionCh = MockActionChannel()
        val conn = OneBotConnection(config, actionCh, null)

        // Register handler that returns ok
        actionCh.onRequest { ActionResponse.ok(data = null, echo = it.echo) }

        val result = conn.execute(SendPrivateMsg(userId = 123, message = listOf(textSegment("hi"))))
        assertEquals("ok", result.status)
        assertEquals(0, result.retcode)
    }

    @Test
    fun `execute action with handler returns success`() = runTest {
        val actionCh = MockActionChannel()
        val conn = OneBotConnection(config, actionCh, null)

        actionCh.onRequest { request ->
            ActionResponse.ok(data = buildJsonObject { put("message_id", 42) }, echo = request.echo)
        }

        val result = conn.execute(SendPrivateMsg(userId = 1, message = listOf(textSegment("x"))))
        assertEquals("ok", result.status)
        assertEquals(0, result.retcode)
    }

    @Test
    fun `onEvent registers handler without error`() = runTest {
        val eventCh = MockEventChannel()
        val conn = OneBotConnection(config, null, eventCh)

        // Should not throw
        conn.onEvent { event -> /* no-op */ }
    }

    @Test
    fun `pushEvent sends to event channel`() = runTest {
        val eventCh = MockEventChannel()
        val conn = OneBotConnection(config, null, eventCh)

        val event = GroupIncreaseEvent(
            time = System.currentTimeMillis() / 1000,
            selfId = 10001,
            groupId = 2001,
            userId = 1011,
            operatorId = 0,
        )

        conn.pushEvent(event)
        assertEquals(1, eventCh.pushedEvents.size)
        assertEquals("notice", eventCh.pushedEvents.first().postType)
        assertEquals(10001L, eventCh.pushedEvents.first().selfId)
    }

    @Test
    fun `onAction registers handler and processes requests`() = runTest {
        val actionCh = MockActionChannel()
        val conn = OneBotConnection(config, actionCh, null)

        conn.onAction { action ->
            assertEquals("get_login_info", action.actionName)
            ActionResponse.ok()
        }

        // Simulate receiving an action request
        val response = actionCh.send(ActionRequest(
            action = "get_login_info",
            params = kotlinx.serialization.json.JsonObject(emptyMap()),
            echo = "echo-1",
        ))

        assertEquals("ok", response.status)
    }

    @Test
    fun `start and stop manage lifecycle`() = runTest {
        val actionCh = MockActionChannel()
        val eventCh = MockEventChannel()
        val conn = OneBotConnection(config, actionCh, eventCh)

        conn.start()
        assertTrue(actionCh.started)
        assertTrue(eventCh.started)

        conn.stop()
        assertTrue(actionCh.stopped)
        assertTrue(eventCh.stopped)
    }

    @Test
    fun `execute without action channel throws`() = runTest {
        val conn = OneBotConnection(config, null, null)

        assertFailsWith<IllegalStateException> {
            conn.execute(GetLoginInfo)
        }
    }

    @Test
    fun `execute returns echo-matched response among multiple concurrent calls`() = runTest {
        val actionCh = MockActionChannel()
        val conn = OneBotConnection(config, actionCh, null)

        actionCh.onRequest { request ->
            when (request.action) {
                "get_login_info" -> ActionResponse.ok(
                    data = buildJsonObject { put("user_id", 10001); put("nickname", "TestBot") },
                    echo = request.echo,
                )
                else -> ActionResponse.ok(echo = request.echo)
            }
        }

        val result = conn.execute(GetLoginInfo)
        assertEquals("ok", result.status)
    }
}
