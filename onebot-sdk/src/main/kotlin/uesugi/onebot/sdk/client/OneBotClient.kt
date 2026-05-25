package uesugi.onebot.sdk.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.EventDispatcher
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.model.OneBotEvent
import uesugi.onebot.core.model.RawActionParams
import uesugi.onebot.core.pipeline.ActionHandler
import uesugi.onebot.core.pipeline.EventHandler
import uesugi.onebot.core.pipeline.Middleware
import uesugi.onebot.core.pipeline.Pipeline
import uesugi.onebot.core.transport.Connection

/**
 * OneBot 客户端统一入口。
 *
 * 供 OneBot 用户使用：调用 API，监听事件，消息 DSL。
 */
class OneBotClient(config: OneBotConfig) {
    private val logger = LoggerFactory.getLogger(OneBotClient::class.java)
    private val connection = Connection(config)
    private val pipeline = Pipeline()
    private val registry = EventDispatcher()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var eventJob: Job? = null
    private var wrappedAction: ActionHandler = { _, _ -> error("OneBotClient not started") }
    private var wrappedEvent: EventHandler = { error("OneBotClient not started") }

    // ===== Middleware =====

    fun use(mw: Middleware) = pipeline.use(mw)

    // ===== Event 监听 =====

    fun onEvent(postType: String, handler: suspend (OneBotEvent) -> Unit) {
        registry.on(postType, handler)
    }

    fun eventFlow(): Flow<OneBotEvent> = connection.events

    // ===== Lifecycle =====

    suspend fun start() {
        connection.buildClient()
        wrappedAction = pipeline.wrapAction { action, params ->
            connection.call(action, params)
        }
        wrappedEvent = pipeline.wrapEvent { event ->
            registry.dispatch(event, scope)
        }
        connection.start()

        eventJob = scope.launch {
            connection.events.collect { event -> wrappedEvent(event) }
        }
        logger.info("OneBotClient started")
    }

    suspend fun stop() {
        eventJob?.cancel()
        connection.stop()
        scope.cancel()
        logger.info("OneBotClient stopped")
    }

    // ===== API 方法 =====

    suspend fun call(action: String, params: JsonObject): OneBotActionResult {
        return wrappedAction(action, RawActionParams(params))
    }

    suspend fun <T : OneBotActionParams> callWith(action: String, request: T): OneBotActionResult {
        return wrappedAction(action, request)
    }
}
