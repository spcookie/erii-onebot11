package uesugi.onebot.sdk.event

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import uesugi.onebot.lib.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventBusTest {

    @Test
    fun `register handler for specific event type`(): Unit = runBlocking {
        val bus = EventBus()
        val deferred = CompletableDeferred<GroupMessageEvent>()

        bus.on(GroupMessageEvent::class) { event ->
            deferred.complete(event)
        }

        val event = GroupMessageEvent(
            time = 1000L,
            selfId = 123L,
            messageId = 1L,
            groupId = 456L,
            userId = 789L,
            rawMessage = "hello",
        )

        bus.dispatch(event)

        val received = deferred.await()
        assertEquals(456L, received.groupId)
        assertEquals(789L, received.userId)

        bus.shutdown()
    }

    @Test
    fun `multiple handlers for same type`(): Unit = runBlocking {
        val bus = EventBus()
        val deferred1 = CompletableDeferred<String>()
        val deferred2 = CompletableDeferred<String>()

        bus.on(GroupMessageEvent::class) { deferred1.complete("handler1") }
        bus.on(GroupMessageEvent::class) { deferred2.complete("handler2") }

        bus.dispatch(GroupMessageEvent(groupId = 1L))

        assertEquals("handler1", deferred1.await())
        assertEquals("handler2", deferred2.await())

        bus.shutdown()
    }

    @Test
    fun `parent type handler receives child events`(): Unit = runBlocking {
        val bus = EventBus()
        val messageDeferred = CompletableDeferred<MessageEvent>()

        // Register handler for the sealed parent type MessageEvent
        bus.on(MessageEvent::class) { event ->
            messageDeferred.complete(event)
        }

        // Dispatch a child type - GroupMessageEvent extends MessageEvent
        val event = GroupMessageEvent(groupId = 999L, userId = 111L)
        bus.dispatch(event)

        val received = messageDeferred.await()
        assertTrue(received is GroupMessageEvent, "Parent handler should receive child event")
        assertEquals(999L, (received as GroupMessageEvent).groupId)

        bus.shutdown()
    }

    @Test
    fun `NoticeEvent handler receives all notice subtypes`(): Unit = runBlocking {
        val bus = EventBus()
        val deferred = CompletableDeferred<NoticeEvent>()

        bus.on(NoticeEvent::class) { event ->
            deferred.complete(event)
        }

        // GroupBanEvent extends NoticeEvent
        val event = GroupBanEvent(groupId = 123L, userId = 456L, duration = 600L)
        bus.dispatch(event)

        val received = deferred.await()
        assertTrue(received is GroupBanEvent)

        bus.shutdown()
    }

    @Test
    fun `handler exception isolation - second handler still executes`(): Unit = runBlocking {
        val bus = EventBus()
        val successDeferred = CompletableDeferred<Boolean>()

        bus.on(GroupMessageEvent::class) {
            throw RuntimeException("handler error")
        }
        bus.on(GroupMessageEvent::class) {
            successDeferred.complete(true)
        }

        bus.dispatch(GroupMessageEvent(groupId = 1L))

        // Second handler should still execute despite first handler exception
        assertEquals(true, successDeferred.await())

        bus.shutdown()
    }

    @Test
    fun `handler not called for unrelated event type`(): Unit = runBlocking {
        val bus = EventBus()
        val deferred = CompletableDeferred<Unit>()

        bus.on(GroupMessageEvent::class) {
            deferred.complete(Unit)
        }

        // Dispatch a NoticeEvent — should NOT trigger GroupMessageEvent handler
        bus.dispatch(GroupBanEvent())

        val result = withTimeoutOrNull(200) {
            deferred.await()
        }
        assertEquals(null, result, "Handler should not be called for unrelated event type")

        bus.shutdown()
    }

    @Test
    fun `off removes all handlers for type`(): Unit = runBlocking {
        val bus = EventBus()
        val deferred = CompletableDeferred<Unit>()

        bus.on(GroupMessageEvent::class) {
            deferred.complete(Unit)
        }

        // Remove handlers before dispatching
        bus.off(GroupMessageEvent::class)

        bus.dispatch(GroupMessageEvent())

        val result = withTimeoutOrNull(200) {
            deferred.await()
        }
        assertEquals(null, result, "Handler should not be called after off()")

        bus.shutdown()
    }

    @Test
    fun `exact match and parent match both fire for child event`(): Unit = runBlocking {
        val bus = EventBus()
        val exactDeferred = CompletableDeferred<String>()
        val parentDeferred = CompletableDeferred<String>()

        bus.on(GroupMessageEvent::class) { exactDeferred.complete("exact") }
        bus.on(MessageEvent::class) { parentDeferred.complete("parent") }

        bus.dispatch(GroupMessageEvent(groupId = 1L))

        assertEquals("exact", exactDeferred.await())
        assertEquals("parent", parentDeferred.await())

        bus.shutdown()
    }

    @Test
    fun `handler receives event with correct fields`(): Unit = runBlocking {
        val bus = EventBus()
        val deferred = CompletableDeferred<PrivateMessageEvent>()

        bus.on(PrivateMessageEvent::class) { event ->
            deferred.complete(event)
        }

        val event = PrivateMessageEvent(
            time = 2000L,
            selfId = 100L,
            messageId = 42L,
            userId = 777L,
            rawMessage = "test message",
            subType = "friend",
        )

        bus.dispatch(event)

        val received = deferred.await()
        assertEquals(2000L, received.time)
        assertEquals(100L, received.selfId)
        assertEquals(42L, received.messageId)
        assertEquals(777L, received.userId)
        assertEquals("test message", received.rawMessage)
        assertEquals("friend", received.subType)

        bus.shutdown()
    }

    @Test
    fun `dispatch to OneBotEvent base handler receives all event types`(): Unit = runBlocking {
        val bus = EventBus()
        val msgDeferred = CompletableDeferred<OneBotEvent>()
        val noticeDeferred = CompletableDeferred<OneBotEvent>()

        bus.on(OneBotEvent::class) { event ->
            if (msgDeferred.isCompleted) {
                noticeDeferred.complete(event)
            } else {
                msgDeferred.complete(event)
            }
        }

        bus.dispatch(GroupMessageEvent(groupId = 10L))
        val msgReceived = msgDeferred.await()
        assertTrue(msgReceived is MessageEvent)

        bus.dispatch(GroupBanEvent(groupId = 20L))
        val noticeReceived = noticeDeferred.await()
        assertTrue(noticeReceived is NoticeEvent)

        bus.shutdown()
    }

    @Test
    fun `shutdown prevents further dispatch`(): Unit = runBlocking {
        val bus = EventBus()
        val deferred = CompletableDeferred<Unit>()

        bus.on(GroupMessageEvent::class) { deferred.complete(Unit) }

        bus.shutdown()

        // Dispatch after shutdown should not reach handler
        bus.dispatch(GroupMessageEvent())

        val result = withTimeoutOrNull(200) {
            deferred.await()
        }
        assertEquals(null, result, "Handler should not be called after shutdown")

        bus.shutdown()
    }
}
