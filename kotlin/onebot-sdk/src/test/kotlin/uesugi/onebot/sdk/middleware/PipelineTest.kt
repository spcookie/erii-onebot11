package uesugi.onebot.sdk.middleware

import kotlinx.coroutines.test.runTest
import uesugi.onebot.lib.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineTest {

    // ============ action middleware ============

    @Test
    fun `action middleware execution order is FIFO`() = runTest {
        val pipeline = MiddlewarePipeline()
        val order = mutableListOf<String>()

        pipeline.use(object : Middleware {
            override suspend fun interceptAction(
                action: Action,
                next: suspend (Action) -> ActionResponse,
            ): ActionResponse {
                order.add("first-before")
                val result = next(action)
                order.add("first-after")
                return result
            }
        })

        pipeline.use(object : Middleware {
            override suspend fun interceptAction(
                action: Action,
                next: suspend (Action) -> ActionResponse,
            ): ActionResponse {
                order.add("second-before")
                val result = next(action)
                order.add("second-after")
                return result
            }
        })

        pipeline.executeAction(GetLoginInfo) { action ->
            order.add("core")
            ActionResponse.ok()
        }

        // First-registered middleware wraps outermost:
        // first-before -> second-before -> core -> second-after -> first-after
        assertEquals(
            listOf("first-before", "second-before", "core", "second-after", "first-after"),
            order,
        )
    }

    @Test
    fun `action middleware can modify the action`() = runTest {
        val pipeline = MiddlewarePipeline()
        var receivedByCore: Action? = null

        pipeline.use(object : Middleware {
            override suspend fun interceptAction(
                action: Action,
                next: suspend (Action) -> ActionResponse,
            ): ActionResponse {
                // Wrap the action with a modified version
                val modified = object : Action {
                    override val actionName = action.actionName + ".modified"
                    override fun toParams() = action.toParams()
                }
                return next(modified)
            }
        })

        pipeline.executeAction(GetLoginInfo) { action ->
            receivedByCore = action
            ActionResponse.ok()
        }

        assertEquals("get_login_info.modified", receivedByCore?.actionName)
    }

    @Test
    fun `action middleware can block and return early`() = runTest {
        val pipeline = MiddlewarePipeline()
        var coreCalled = false
        val blockedResponse = ActionResponse.failed(retcode = -1)

        pipeline.use(object : Middleware {
            override suspend fun interceptAction(
                action: Action,
                next: suspend (Action) -> ActionResponse,
            ): ActionResponse {
                // Block the action — do NOT call next()
                return blockedResponse
            }
        })

        val result = pipeline.executeAction(GetLoginInfo) {
            coreCalled = true
            ActionResponse.ok()
        }

        assertFalse(coreCalled, "Core should not be called when middleware blocks")
        assertEquals(-1, result.retcode)
    }

    @Test
    fun `action middleware can modify the response`() = runTest {
        val pipeline = MiddlewarePipeline()

        pipeline.use(object : Middleware {
            override suspend fun interceptAction(
                action: Action,
                next: suspend (Action) -> ActionResponse,
            ): ActionResponse {
                val response = next(action)
                // Modify the response
                return response.copy(status = "modified")
            }
        })

        val result = pipeline.executeAction(GetLoginInfo) {
            ActionResponse.ok()
        }

        assertEquals("modified", result.status)
    }

    @Test
    fun `empty pipeline passes through directly`() = runTest {
        val pipeline = MiddlewarePipeline()
        var coreCalled = false

        val result = pipeline.executeAction(DeleteMsg(42L)) {
            coreCalled = true
            ActionResponse.ok()
        }

        assertTrue(coreCalled)
        assertTrue(result.isOk)
    }

    @Test
    fun `multiple action middlewares chain correctly with three`() = runTest {
        val pipeline = MiddlewarePipeline()
        val trace = mutableListOf<String>()

        repeat(3) { i ->
            pipeline.use(object : Middleware {
                override suspend fun interceptAction(
                    action: Action,
                    next: suspend (Action) -> ActionResponse,
                ): ActionResponse {
                    trace.add("mw${i + 1}-enter")
                    val r = next(action)
                    trace.add("mw${i + 1}-exit")
                    return r
                }
            })
        }

        pipeline.executeAction(GetLoginInfo) {
            trace.add("core")
            ActionResponse.ok()
        }

        // Chain: mw1-enter -> mw2-enter -> mw3-enter -> core -> mw3-exit -> mw2-exit -> mw1-exit
        assertEquals(
            listOf("mw1-enter", "mw2-enter", "mw3-enter", "core", "mw3-exit", "mw2-exit", "mw1-exit"),
            trace,
        )
    }

    // ============ event middleware ============

    @Test
    fun `event middleware execution order is FIFO`() = runTest {
        val pipeline = MiddlewarePipeline()
        val order = mutableListOf<String>()

        pipeline.use(object : Middleware {
            override suspend fun interceptEvent(
                event: OneBotEvent,
                next: suspend (OneBotEvent) -> Unit,
            ) {
                order.add("first-before")
                next(event)
                order.add("first-after")
            }
        })

        pipeline.use(object : Middleware {
            override suspend fun interceptEvent(
                event: OneBotEvent,
                next: suspend (OneBotEvent) -> Unit,
            ) {
                order.add("second-before")
                next(event)
                order.add("second-after")
            }
        })

        pipeline.processEvent(GroupMessageEvent()) {
            order.add("core")
        }

        assertEquals(
            listOf("first-before", "second-before", "core", "second-after", "first-after"),
            order,
        )
    }

    @Test
    fun `event middleware can intercept and block the event`() = runTest {
        val pipeline = MiddlewarePipeline()
        var coreCalled = false

        pipeline.use(object : Middleware {
            override suspend fun interceptEvent(
                event: OneBotEvent,
                next: suspend (OneBotEvent) -> Unit,
            ) {
                // Block the event — do NOT call next()
            }
        })

        pipeline.processEvent(GroupMessageEvent()) {
            coreCalled = true
        }

        assertFalse(coreCalled, "Core should not be called when event middleware blocks")
    }

    @Test
    fun `event middleware can modify the event`() = runTest {
        val pipeline = MiddlewarePipeline()
        var receivedEvent: OneBotEvent? = null

        pipeline.use(object : Middleware {
            override suspend fun interceptEvent(
                event: OneBotEvent,
                next: suspend (OneBotEvent) -> Unit,
            ) {
                // Pass a modified event downstream
                val modified = GroupMessageEvent(groupId = 999L)
                next(modified)
            }
        })

        pipeline.processEvent(GroupMessageEvent(groupId = 123L)) { event ->
            receivedEvent = event
        }

        assertEquals(999L, (receivedEvent as? GroupMessageEvent)?.groupId)
    }

    @Test
    fun `empty pipeline passes event through directly`() = runTest {
        val pipeline = MiddlewarePipeline()
        var received = false

        pipeline.processEvent(GroupMessageEvent(groupId = 456L)) {
            received = true
        }

        assertTrue(received)
    }

    @Test
    fun `event middleware receives the correct event type`() = runTest {
        val pipeline = MiddlewarePipeline()
        var middlewareReceived: OneBotEvent? = null
        var coreReceived: OneBotEvent? = null

        pipeline.use(object : Middleware {
            override suspend fun interceptEvent(
                event: OneBotEvent,
                next: suspend (OneBotEvent) -> Unit,
            ) {
                middlewareReceived = event
                next(event)
            }
        })

        val original = GroupBanEvent(groupId = 777L, userId = 888L)
        pipeline.processEvent(original) { event ->
            coreReceived = event
        }

        assertTrue(middlewareReceived is GroupBanEvent)
        assertEquals(777L, (middlewareReceived as GroupBanEvent).groupId)
        assertTrue(coreReceived is GroupBanEvent)
    }

    // ============ remove ============

    @Test
    fun `remove middleware excludes it from the chain`() = runTest {
        val pipeline = MiddlewarePipeline()
        val trace = mutableListOf<String>()

        val toRemove = object : Middleware {
            override suspend fun interceptAction(
                action: Action,
                next: suspend (Action) -> ActionResponse,
            ): ActionResponse {
                trace.add("removed")
                return next(action)
            }
        }

        pipeline.use(object : Middleware {
            override suspend fun interceptAction(
                action: Action,
                next: suspend (Action) -> ActionResponse,
            ): ActionResponse {
                trace.add("keeper-before")
                val r = next(action)
                trace.add("keeper-after")
                return r
            }
        })

        pipeline.use(toRemove)
        pipeline.remove(toRemove)

        pipeline.executeAction(GetLoginInfo) {
            trace.add("core")
            ActionResponse.ok()
        }

        assertEquals(
            listOf("keeper-before", "core", "keeper-after"),
            trace,
        )
    }

    @Test
    fun `default Middleware methods pass through unchanged`() = runTest {
        val pipeline = MiddlewarePipeline()
        var coreCalled = false

        // Using Middleware directly without overriding — uses default pass-through impl
        pipeline.use(object : Middleware {
            // No overrides — default implementations call next() directly
        })

        val result = pipeline.executeAction(GetLoginInfo) {
            coreCalled = true
            ActionResponse.ok()
        }

        assertTrue(coreCalled)
        assertTrue(result.isOk)
    }
}
