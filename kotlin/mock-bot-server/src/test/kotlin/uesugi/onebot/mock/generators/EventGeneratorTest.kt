package uesugi.onebot.mock.generators

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import uesugi.onebot.lib.connection.OneBotConfig
import uesugi.onebot.lib.connection.OneBotConnection
import uesugi.onebot.lib.model.EventEnvelope
import uesugi.onebot.lib.model.GroupIncreaseEvent
import uesugi.onebot.lib.model.GroupMessageEvent
import uesugi.onebot.lib.model.HeartbeatMetaEvent
import uesugi.onebot.lib.model.LifecycleMetaEvent
import uesugi.onebot.lib.model.StatusInfo
import uesugi.onebot.lib.model.textSegment
import uesugi.onebot.lib.transport.EventChannel

/**
 * Minimal mock EventChannel that collects pushed events in a list.
 */
class MockEventChannel : EventChannel {
    private val flow = MutableSharedFlow<EventEnvelope>(replay = 0, extraBufferCapacity = 100)
    var started = false
    var stopped = false
    val pushedEvents = mutableListOf<EventEnvelope>()

    override val eventFlow: Flow<EventEnvelope> = flow

    override suspend fun start() { started = true }
    override suspend fun stop() { stopped = true }

    override suspend fun push(event: EventEnvelope) {
        pushedEvents.add(event)
        flow.emit(event)
    }
}

class EventGeneratorTest {

    private val data = MockData()

    private fun createConnection(eventCh: MockEventChannel): OneBotConnection {
        return OneBotConnection(
            config = OneBotConfig(),
            actionChannel = null,
            eventChannel = eventCh,
        )
    }

    @Test
    fun `triggerEvent pushes GroupMessageEvent to connection`() = runTest {
        val eventCh = MockEventChannel()
        val conn = createConnection(eventCh)
        val generator = EventGenerator(conn, data)

        val event = GroupMessageEvent(
            time = 1234567890,
            selfId = data.botUserId,
            subType = "normal",
            messageId = data.nextMsgId(),
            groupId = 2001,
            userId = 1011,
            message = listOf(textSegment("test message")),
            rawMessage = "test message",
            font = 0,
        )

        generator.triggerEvent(event)

        assertEquals(1, eventCh.pushedEvents.size)
        val pushed = eventCh.pushedEvents.first()
        assertEquals("message", pushed.postType)
        assertEquals(data.botUserId, pushed.selfId)
        assertEquals(1234567890L, pushed.time)
    }

    @Test
    fun `triggerEvent pushes GroupIncreaseEvent to connection`() = runTest {
        val eventCh = MockEventChannel()
        val conn = createConnection(eventCh)
        val generator = EventGenerator(conn, data)

        val event = GroupIncreaseEvent(
            time = 1234567890,
            selfId = data.botUserId,
            subType = "approve",
            groupId = 2001,
            userId = 1011,
            operatorId = 1011,
        )

        generator.triggerEvent(event)

        assertEquals(1, eventCh.pushedEvents.size)
        val pushed = eventCh.pushedEvents.first()
        assertEquals("notice", pushed.postType)
        assertEquals(data.botUserId, pushed.selfId)
    }

    @Test
    fun `triggerEvent pushes HeartbeatMetaEvent to connection`() = runTest {
        val eventCh = MockEventChannel()
        val conn = createConnection(eventCh)
        val generator = EventGenerator(conn, data)

        val event = HeartbeatMetaEvent(
            time = 1234567890,
            selfId = data.botUserId,
            status = StatusInfo(online = true, good = true),
            interval = 15000,
        )

        generator.triggerEvent(event)

        assertEquals(1, eventCh.pushedEvents.size)
        val pushed = eventCh.pushedEvents.first()
        assertEquals("meta_event", pushed.postType)
        assertEquals(data.botUserId, pushed.selfId)
    }

    @Test
    fun `triggerEvent multiple events each produce an envelope`() = runTest {
        val eventCh = MockEventChannel()
        val conn = createConnection(eventCh)
        val generator = EventGenerator(conn, data)

        val event1 = GroupMessageEvent(
            time = 1000,
            selfId = data.botUserId,
            subType = "normal",
            messageId = data.nextMsgId(),
            groupId = 2001,
            userId = 1011,
            message = listOf(textSegment("msg 1")),
            rawMessage = "msg 1",
            font = 0,
        )

        val event2 = GroupIncreaseEvent(
            time = 2000,
            selfId = data.botUserId,
            subType = "approve",
            groupId = 2001,
            userId = 1012,
            operatorId = 0,
        )

        generator.triggerEvent(event1)
        generator.triggerEvent(event2)

        assertEquals(2, eventCh.pushedEvents.size)
    }

    @Test
    fun `triggerEvent LifecycleMetaEvent produces meta_event postType`() = runTest {
        val eventCh = MockEventChannel()
        val conn = createConnection(eventCh)
        val generator = EventGenerator(conn, data)

        val event = LifecycleMetaEvent(
            time = 1000,
            selfId = data.botUserId,
            subType = "enable",
        )

        generator.triggerEvent(event)

        assertEquals(1, eventCh.pushedEvents.size)
        val pushed = eventCh.pushedEvents.first()
        assertEquals("meta_event", pushed.postType)
    }
}
