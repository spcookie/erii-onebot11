package uesugi.onebot.lib.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import uesugi.onebot.lib.model.ActionResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class EchoManagerTest {

    @Test
    fun `generateEcho returns unique ids`() = runTest {
        val manager = EchoManager(30000L)
        val echoes = (1..100).map { manager.generateEcho() }
        assertEquals(100, echoes.distinct().size)
    }

    @Test
    fun `generateEcho starts with uuid pattern`() = runTest {
        val manager = EchoManager(30000L)
        val echo = manager.generateEcho()
        assertTrue(echo.contains("-"), "Echo should contain dashes: $echo")
    }

    @Test
    fun `register and resolve completes deferred`() = runTest {
        val manager = EchoManager(5000L)
        val echo = manager.generateEcho()
        val deferred = manager.register(echo)

        val response = ActionResponse.ok(echo = echo)
        manager.resolve(echo, response)

        val result = deferred.await()
        assertEquals("ok", result.status)
        assertEquals(echo, result.echo)
    }

    @Test
    fun `resolve unknown echo is no-op`() = runTest {
        val manager = EchoManager(5000L)
        manager.resolve("unknown-echo", ActionResponse.ok())
    }

    @Test
    fun `timeout cancels deferred`() = runTest {
        val manager = EchoManager(100L)
        val echo = manager.generateEcho()
        val deferred = manager.register(echo)

        val error = assertFailsWith<CancellationException> {
            deferred.await()
        }
        assertNotNull(error)
    }

    @Test
    fun `multiple concurrent registers`() = runTest {
        val manager = EchoManager(5000L)
        val pairs = (1..50).map {
            val echo = manager.generateEcho()
            echo to manager.register(echo)
        }

        // Resolve all out of order
        pairs.shuffled().forEach { (echo, _) ->
            manager.resolve(echo, ActionResponse.ok(echo = echo))
        }

        // All should complete
        pairs.forEach { (echo, deferred) ->
            val result = deferred.await()
            assertEquals(echo, result.echo)
            assertEquals("ok", result.status)
        }
    }

    @Test
    fun `cancelAll stops all pending`() = runTest {
        val manager = EchoManager(50000L)
        val pairs = (1..10).map {
            val echo = manager.generateEcho()
            echo to manager.register(echo)
        }

        manager.stop()

        pairs.forEach { (_, deferred) ->
            assertFailsWith<CancellationException> {
                deferred.await()
            }
        }
    }

    @Test
    fun `resolve after cancel is ignored`() = runTest {
        val manager = EchoManager(5000L)
        val echo = manager.generateEcho()
        val deferred = manager.register(echo)

        manager.stop()

        manager.resolve(echo, ActionResponse.ok())

        assertFailsWith<CancellationException> {
            deferred.await()
        }
    }
}
