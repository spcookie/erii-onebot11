package uesugi.onebot.mock

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.connection.OneBotConfig
import uesugi.onebot.lib.connection.OneBotConnection
import uesugi.onebot.lib.model.*
import uesugi.onebot.mock.generators.MockData
import uesugi.onebot.mock.generators.EventGenerator
import uesugi.onebot.mock.handlers.MockActionHandlers

/**
 * Mock OneBot 实现端（模拟 QQ 后端）。
 * 启动 HTTP + WebSocket 服务，返回模拟数据并可定时发送模拟事件。
 */
class MockServer(private val config: OneBotConfig) {
    private val logger = LoggerFactory.getLogger(MockServer::class.java)
    private val connection: OneBotConnection = OneBotConnection.createForImplementation(config)
    private val data = MockData()
    private val actionHandlers = MockActionHandlers(data)
    private val eventGen = EventGenerator(connection, data)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var started = false

    suspend fun start() {
        // 注册动作处理器
        connection.onAction { action ->
            logger.debug("Action received: {}", action.actionName)
            actionHandlers.handle(action)
        }

        connection.start()

        // 启动事件生成器
        eventGen.start(scope)

        started = true
        logger.info("Mock server started")
    }

    suspend fun stop() {
        eventGen.stop()
        connection.stop()
        scope.cancel()
        started = false
        logger.info("Mock server stopped")
    }
}
