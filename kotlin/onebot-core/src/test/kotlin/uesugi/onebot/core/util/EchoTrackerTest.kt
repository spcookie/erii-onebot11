package uesugi.onebot.core.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import uesugi.onebot.core.model.ActionResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EchoTrackerTest {

    @Test
    fun `generateEcho produces unique IDs`() = runTest {
        val tracker = EchoTracker()
        tracker.setScope(this)

        val echoes = (1..100).map { tracker.generateEcho() }
        assertEquals(100, echoes.distinct().size)
    }

    @Test
    fun `register and resolve`() = runTest {
        val tracker = EchoTracker()
        tracker.setScope(this)

        val echo = tracker.generateEcho()
        val deferred = tracker.register(echo, timeoutMs = 5000)

        val response = ActionResponse.ok(JsonNull, echo = echo)
        val found = tracker.resolve(echo, response)

        assertTrue(found)
        assertEquals(ActionResponse.STATUS_OK, deferred.await().status)
    }

    @Test
    fun `resolve returns false for unknown echo`() = runTest {
        val tracker = EchoTracker()
        tracker.setScope(this)

        val found = tracker.resolve("non-existent", ActionResponse.ok(JsonNull))
        assertFalse(found)
    }

    @Test
    fun `timeout completes with failed response`() = runTest {
        val tracker = EchoTracker()
        tracker.setScope(this)

        val echo = tracker.generateEcho()
        val deferred = tracker.register(echo, timeoutMs = 100)

        // Wait for timeout
        delay(200)

        val response = deferred.await()
        assertEquals(ActionResponse.STATUS_FAILED, response.status)
        assertEquals(-1, response.retcode)
        assertEquals(echo, response.echo)
        assertEquals(0, tracker.pendingCount())
    }

    @Test
    fun `resolve before timeout returns success`() = runTest {
        val tracker = EchoTracker()
        tracker.setScope(this)

        val echo = tracker.generateEcho()
        val deferred = tracker.register(echo, timeoutMs = 5000)

        val response = ActionResponse.ok(JsonNull, echo = echo)
        tracker.resolve(echo, response)

        assertEquals(ActionResponse.STATUS_OK, deferred.await().status)
    }

    @Test
    fun `cancelAll clears all pending`() = runTest {
        val tracker = EchoTracker()
        tracker.setScope(this)

        val echo1 = tracker.generateEcho()
        val echo2 = tracker.generateEcho()

        tracker.register(echo1, timeoutMs = 5000)
        tracker.register(echo2, timeoutMs = 5000)

        assertEquals(2, tracker.pendingCount())

        tracker.cancelAll()

        assertEquals(0, tracker.pendingCount())
    }

    @Test
    fun `multiple concurrent register and resolve`() = runTest {
        val tracker = EchoTracker()
        tracker.setScope(this)

        val echoes = (1..50).map { tracker.generateEcho() }

        // Register all
        val deferreds = echoes.map { echo ->
            tracker.register(echo, timeoutMs = 5000)
        }

        assertEquals(50, tracker.pendingCount())

        // Resolve all
        echoes.forEachIndexed { i, echo ->
            tracker.resolve(echo, ActionResponse.ok(JsonNull, echo = echo))
        }

        // All should complete successfully
        deferreds.forEach { deferred ->
            assertEquals(ActionResponse.STATUS_OK, deferred.await().status)
        }

        assertEquals(0, tracker.pendingCount())
    }
}
