package uesugi.onebot.lib.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.model.*
import uesugi.onebot.lib.transport.*
import uesugi.onebot.lib.transport.http.*
import uesugi.onebot.lib.transport.ws.*

/**
 * OneBot 连接——统一管理动作通道和事件通道。
 * 同时支持 Application 端和 Implementation 端角色。
 */
class OneBotConnection(
    private val config: OneBotConfig,
    private val actionChannel: ActionChannel?,
    private val eventChannel: EventChannel?,
) {
    private val logger = LoggerFactory.getLogger(OneBotConnection::class.java)
    private val echoManager = EchoManager(config.timeoutMs)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ============ Application 端 API ============

    /**
     * 执行 API 动作并获取响应（Application 端使用）。
     */
    suspend fun execute(action: Action): ActionResponse {
        val ch = actionChannel ?: throw IllegalStateException("No action channel configured")
        val echo = echoManager.generateEcho()
        val params = action.toParams()

        val request = ActionRequest(action = action.actionName, params = params, echo = echo)
        val deferred = echoManager.register(echo)

        logger.debug("Action sent: {} (echo={})", action.actionName, echo)

        try {
            // 发送并等待响应
            val response = ch.send(request)

            // 同步模式（HTTP）：直接返回
            if (response.echo == null || response.echo == echo) {
                echoManager.resolve(echo, response)
            }
            return deferred.await()
        } catch (e: Exception) {
            echoManager.resolve(echo, ActionResponse("failed", 1, null, echo))
            throw e
        }
    }

    /**
     * 注册事件处理器（Application 端使用）。
     */
    fun onEvent(handler: suspend (OneBotEvent) -> Unit) {
        val ch = eventChannel ?: throw IllegalStateException("No event channel configured")
        scope.launch {
            ch.eventFlow.collect { envelope ->
                try {
                    val event = parseEvent(envelope)
                    if (event != null) {
                        handler(event)
                    }
                } catch (e: Exception) {
                    logger.error("Error handling event: {}", e.message)
                }
            }
        }
    }

    // ============ Implementation 端 API ============

    /**
     * 推送事件到应用端（Implementation 端使用）。
     */
    suspend fun pushEvent(event: OneBotEvent) {
        val ch = eventChannel ?: throw IllegalStateException("No event channel configured")
        val envelope = EventEnvelope(
            time = event.time,
            selfId = event.selfId,
            postType = event.postType,
        )
        ch.push(envelope)
        logger.debug("Event pushed: post_type={}", event.postType)
    }

    /**
     * 注册动作处理器（Implementation 端使用）。
     */
    fun onAction(handler: suspend (Action) -> ActionResponse) {
        val ch = actionChannel ?: throw IllegalStateException("No action channel configured")
        ch.onRequest { request ->
            val action = parseAction(request)
            handler(action)
        }
    }

    // ============ 生命周期 ============

    suspend fun start() {
        actionChannel?.start()
        eventChannel?.start()
        logger.info("OneBotConnection started")
    }

    suspend fun stop() {
        echoManager.stop()
        actionChannel?.stop()
        eventChannel?.stop()
        scope.cancel()
        logger.info("OneBotConnection stopped")
    }

    // ============ 内部解析 ============

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "post_type"
    }

    private fun parseEvent(envelope: EventEnvelope): OneBotEvent? {
        // 简单返回原始事件包装——具体类型由调用方按需解析
        return PolymorphicEvent(
            time = envelope.time,
            selfId = envelope.selfId,
            postType = envelope.postType,
        )
    }

    private fun parseAction(request: ActionRequest): Action {
        // 构建匹配的 Action 对象
        return when (request.action) {
            "get_login_info" -> GetLoginInfo
            "get_friend_list" -> GetFriendList
            "get_group_list" -> GetGroupList
            "get_status" -> GetStatus
            "get_version_info" -> GetVersionInfo
            "can_send_image" -> CanSendImage
            "can_send_record" -> CanSendRecord
            "get_csrf_token" -> GetCsrfToken
            "clean_cache" -> CleanCache
            else -> GenericAction(request.action, request.params)
        }
    }

    /**
     * 辅助方法：通过配置创建连接
     */
    companion object {
        private fun OneBotConfig.transport(
            host: String = "127.0.0.1",
            port: Int = 5700,
            includeTimeout: Boolean = false,
            includeReconnect: Boolean = false,
            includeSelfId: Boolean = false,
            includeSecret: Boolean = false,
        ) = TransportConfig(
            host = host,
            port = port,
            accessToken = this.accessToken,
            secret = if (includeSecret) this.secret else null,
            selfId = if (includeSelfId) this.selfId else 0,
            timeoutMs = if (includeTimeout) this.timeoutMs else 30000,
            reconnectInterval = if (includeReconnect) this.reconnectInterval else 3000,
        )

        fun createForApplication(config: OneBotConfig): OneBotConnection {
            val actionCh = when {
                config.httpEnable -> HttpActionClient(
                    config.transport(config.httpHost, config.httpPort, includeTimeout = true)
                )
                config.wsEnable -> WsClient(
                    config.transport(config.wsHost, config.wsPort, includeTimeout = true, includeReconnect = true),
                    wsUrl = "ws://${config.wsHost}:${config.wsPort}/api",
                    role = WsRole.API,
                )
                config.wsReverseEnable -> WsClient(
                    config.transport(includeTimeout = true, includeReconnect = true, includeSelfId = true),
                    wsUrl = config.wsReverseUrl.ifEmpty {
                        config.wsReverseApiUrl.ifEmpty { "ws://127.0.0.1:8080/ws" }
                    },
                    role = if (config.wsReverseUseUniversal) WsRole.UNIVERSAL else WsRole.API,
                )
                else -> null
            }

            val eventCh = when {
                config.httpPostEnable -> HttpEventServer(
                    config.transport(config.httpPostHost, config.httpPostPort, includeSecret = true)
                )
                config.wsReverseEnable -> actionCh as? EventChannel
                else -> null
            }

            return OneBotConnection(config, actionCh, eventCh)
        }

        fun createForImplementation(config: OneBotConfig): OneBotConnection {
            val actionCh = when {
                config.httpEnable -> HttpActionServer(
                    config.transport(config.httpHost, config.httpPort)
                )
                config.wsEnable -> WsServer(
                    config.transport(config.wsHost, config.wsPort)
                )
                else -> null
            }

            val eventCh = when {
                config.httpPostEnable -> HttpEventClient(
                    config.transport(includeSelfId = true, includeSecret = true),
                    targetUrl = config.httpPostUrl,
                )
                config.wsEnable -> actionCh as? EventChannel
                else -> null
            }

            return OneBotConnection(config, actionCh, eventCh)
        }
    }
}

/**
 * 未知/未匹配的动作，保留原始参数。
 */
data class GenericAction(
    override val actionName: String,
    val rawParams: JsonObject,
) : Action {
    override fun toParams() = rawParams
}
