package uesugi.onebot.core.pipeline

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import uesugi.onebot.core.dispatch.MiddlewareException
import uesugi.onebot.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PipelineTest {

    private val dummyParams: OneBotActionParams = RawActionParams(JsonObject(emptyMap()))

    // ===== Action Pipeline =====

    @Test
    fun `wrapAction without middleware calls handler directly`() = runTest {
        val pipeline = Pipeline()
        val handler: ActionHandler = { _, _ -> RawActionResult() }

        val wrapped = pipeline.wrapAction(handler)
        val result = wrapped("test", dummyParams)

        assertTrue(result is RawActionResult)
    }

    @Test
    fun `wrapAction with single middleware in onion order`() = runTest {
        val pipeline = Pipeline()
        val order = mutableListOf<String>()

        pipeline.useAction { _, params, next ->
            order.add("before")
            val result = next("test", params)
            order.add("after")
            result
        }

        val handler: ActionHandler = { _, _ ->
            order.add("handler")
            RawActionResult()
        }

        val wrapped = pipeline.wrapAction(handler)
        wrapped("test", dummyParams)

        assertEquals(listOf("before", "handler", "after"), order)
    }

    @Test
    fun `wrapAction onion order with multiple middlewares`() = runTest {
        val pipeline = Pipeline()
        val order = mutableListOf<String>()

        pipeline.useAction { _, params, next ->
            order.add("mw1-in");
            val r = next("x", params); order.add("mw1-out"); r
        }
        pipeline.useAction { _, params, next ->
            order.add("mw2-in");
            val r = next("x", params); order.add("mw2-out"); r
        }
        pipeline.useAction { _, params, next ->
            order.add("mw3-in");
            val r = next("x", params); order.add("mw3-out"); r
        }

        val handler: ActionHandler = { _, _ -> order.add("handler"); RawActionResult() }

        val wrapped = pipeline.wrapAction(handler)
        wrapped("x", dummyParams)

        assertEquals(
            listOf("mw1-in", "mw2-in", "mw3-in", "handler", "mw3-out", "mw2-out", "mw1-out"),
            order
        )
    }

    @Test
    fun `middleware can short-circuit action`() = runTest {
        val pipeline = Pipeline()
        var handlerCalled = false

        pipeline.useAction { _, _, _ ->
            throw MiddlewareException(403, "short-circuit")
        }

        val handler: ActionHandler = { _, _ ->
            handlerCalled = true
            RawActionResult()
        }

        val wrapped = pipeline.wrapAction(handler)
        assertFailsWith<MiddlewareException> { wrapped("test", dummyParams) }
        assertEquals(false, handlerCalled)
    }

    // ===== Event Pipeline =====

    @Test
    fun `wrapEvent without middleware calls handler directly`() = runTest {
        val pipeline = Pipeline()
        var called = false

        val handler: EventHandler = { called = true }
        val wrapped = pipeline.wrapEvent(handler)

        wrapped(PrivateMessageEvent(time = 0, selfId = 0))
        assertEquals(true, called)
    }

    @Test
    fun `wrapEvent with single middleware in onion order`() = runTest {
        val pipeline = Pipeline()
        val order = mutableListOf<String>()

        pipeline.useEvent { event, next ->
            order.add("before")
            next(event)
            order.add("after")
        }

        val handler: EventHandler = { order.add("handler") }

        val wrapped = pipeline.wrapEvent(handler)
        wrapped(PrivateMessageEvent(time = 0, selfId = 0))

        assertEquals(listOf("before", "handler", "after"), order)
    }

    @Test
    fun `wrapEvent onion order with multiple middlewares`() = runTest {
        val pipeline = Pipeline()
        val order = mutableListOf<String>()

        pipeline.useEvent { _, next -> order.add("mw1-in"); next(PrivateMessageEvent(0, 0)); order.add("mw1-out") }
        pipeline.useEvent { _, next -> order.add("mw2-in"); next(PrivateMessageEvent(0, 0)); order.add("mw2-out") }

        val handler: EventHandler = { order.add("handler") }

        val wrapped = pipeline.wrapEvent(handler)
        wrapped(PrivateMessageEvent(0, 0))

        assertEquals(listOf("mw1-in", "mw2-in", "handler", "mw2-out", "mw1-out"), order)
    }

    @Test
    fun `middleware can short-circuit event`() = runTest {
        val pipeline = Pipeline()
        var handlerCalled = false

        pipeline.useEvent { _, _ ->
            // Short-circuit, never call next
        }

        val handler: EventHandler = { handlerCalled = true }

        val wrapped = pipeline.wrapEvent(handler)
        wrapped(PrivateMessageEvent(time = 0, selfId = 0))

        assertEquals(false, handlerCalled)
    }

    // ===== Middleware interface =====

    @Test
    fun `Middleware interface works with Pipeline use`() = runTest {
        val pipeline = Pipeline()
        var intercepted = false

        val mw = object : Middleware {
            override suspend fun interceptAction(
                action: String,
                params: OneBotActionParams,
                next: ActionHandler
            ): OneBotActionResult {
                intercepted = true
                return next(action, params)
            }
        }

        pipeline.use(mw)

        val handler: ActionHandler = { _, _ -> RawActionResult() }
        val wrapped = pipeline.wrapAction(handler)
        wrapped("x", dummyParams)

        assertEquals(true, intercepted)
    }
}
