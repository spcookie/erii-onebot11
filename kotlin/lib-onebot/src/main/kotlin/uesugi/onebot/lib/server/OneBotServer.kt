package uesugi.onebot.lib.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.dispatch.ActionDispatcher
import uesugi.onebot.core.dispatch.ActionNotFoundException
import uesugi.onebot.core.dispatch.MiddlewareException
import uesugi.onebot.core.model.*
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.pipeline.Middleware
import uesugi.onebot.core.pipeline.Pipeline
import uesugi.onebot.core.transport.Connection

class OneBotServer(private val config: OneBotConfig) {
    private val logger = LoggerFactory.getLogger(OneBotServer::class.java)
    private val connection = Connection(config)
    private val pipeline = Pipeline()
    @PublishedApi
    internal val dispatcher = ActionDispatcher(config.rateLimitInterval)
    private val resultParser = ActionResultParser()
    private val paramParser = ActionParamParser(config.messageFormat)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 注册 raw handler（接收 JsonObject，返回 OneBotActionResult） */
    @JvmName("onActionRaw")
    fun onAction(action: String, handler: suspend (JsonObject) -> OneBotActionResult) {
        dispatcher.onAction(action, handler)
    }

    /** 注册类型安全的 handler（接收反序列化后的请求对象，返回 OneBotActionResult） */
    @JvmName("onActionTyped")
    inline fun <reified T : OneBotActionParams> onAction(
        action: String,
        noinline handler: suspend (T) -> OneBotActionResult
    ) {
        dispatcher.onAction(action, handler)
    }

    suspend fun pushEvent(event: OneBotEvent) {
        connection.pushEvent(event)
    }

    /** 直接分发 Action（用于 mock/testing 场景，绕过传输层直接调用 handler） */
    suspend fun dispatchAction(action: String, params: JsonObject): ActionResponse {
        return try {
            val deserializedParams = paramParser.deserialize(action, params)
            when (val result = dispatcher.dispatch(action, deserializedParams)) {
                is AsyncActionResult -> ActionResponse.async()
                else -> {
                    val data = resultParser.serialize(action, result)
                    ActionResponse.ok(data)
                }
            }
        } catch (e: ActionNotFoundException) {
            ActionResponse.notFound()
        } catch (e: MiddlewareException) {
            ActionResponse.failed(e.retcode)
        } catch (e: Exception) {
            logger.warn("Action dispatch failed: $action", e)
            ActionResponse.badRequest()
        }
    }

    fun use(mw: Middleware) = pipeline.use(mw)

    suspend fun start() {
        dispatcher.setScope(scope)
        val handler = pipeline.wrapAction { action, params ->
            dispatcher.dispatch(action, params)
        }
        connection.buildServer { action, params ->
            try {
                handler(action, params)
            } catch (e: ActionNotFoundException) {
                throw e
            } catch (e: MiddlewareException) {
                throw e
            }
        }
        connection.start()
        logger.info("OneBotServer started")
    }

    suspend fun stop() {
        connection.stop()
        dispatcher.stop()
        scope.cancel()
        logger.info("OneBotServer stopped")
    }
}
