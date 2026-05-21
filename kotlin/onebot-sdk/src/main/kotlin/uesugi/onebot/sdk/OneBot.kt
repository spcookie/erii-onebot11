package uesugi.onebot.sdk

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.connection.OneBotConfig
import uesugi.onebot.lib.connection.OneBotConnection
import uesugi.onebot.lib.model.*
import uesugi.onebot.sdk.event.EventBus
import uesugi.onebot.sdk.middleware.Middleware
import uesugi.onebot.sdk.middleware.MiddlewarePipeline
import uesugi.onebot.sdk.util.buildMessage
import kotlin.reflect.KClass

/**
 * OneBot SDK 主类。
 * 提供便捷的 API 方法和事件处理能力。
 */
class OneBot(
    private val config: OneBotConfig,
) {
    private val logger = LoggerFactory.getLogger(OneBot::class.java)
    private val connection: OneBotConnection = OneBotConnection.createForApplication(config)
    private val pipeline = MiddlewarePipeline()
    private val eventBus = EventBus()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var started = false

    // ============ 中间件 ============

    fun use(middleware: Middleware) {
        pipeline.use(middleware)
    }

    // ============ 事件处理 ============

    fun onMessage(handler: suspend (MessageEvent) -> Unit) {
        eventBus.on(MessageEvent::class) { handler(it) }
    }

    fun onNotice(handler: suspend (NoticeEvent) -> Unit) {
        eventBus.on(NoticeEvent::class) { handler(it) }
    }

    fun onRequest(handler: suspend (RequestEvent) -> Unit) {
        eventBus.on(RequestEvent::class) { handler(it) }
    }

    fun onMetaEvent(handler: suspend (MetaEvent) -> Unit) {
        eventBus.on(MetaEvent::class) { handler(it) }
    }

    fun <T : OneBotEvent> on(type: KClass<T>, handler: suspend (T) -> Unit) {
        eventBus.on(type, handler = handler)
    }

    // ============ API 便捷方法 ============

    private suspend fun execute(action: Action): ActionResponse {
        return pipeline.executeAction(action) { connection.execute(it) }
    }

    /** 发送私聊消息，返回 message_id */
    suspend fun sendPrivateMsg(userId: Long, message: List<MessageSegment>, autoEscape: Boolean = false): Long {
        val resp = execute(SendPrivateMsg(userId, message, autoEscape))
        return resp.data?.jsonObject?.get("message_id")?.jsonPrimitive?.longOrNull ?: 0
    }

    /** 发送群消息，返回 message_id */
    suspend fun sendGroupMsg(groupId: Long, message: List<MessageSegment>, autoEscape: Boolean = false): Long {
        val resp = execute(SendGroupMsg(groupId, message, autoEscape))
        return resp.data?.jsonObject?.get("message_id")?.jsonPrimitive?.longOrNull ?: 0
    }

    /** 撤回消息 */
    suspend fun deleteMsg(messageId: Long) {
        execute(DeleteMsg(messageId))
    }

    /** 获取登录信息 */
    suspend fun getLoginInfo(): LoginInfo {
        val resp = execute(GetLoginInfo)
        val data = resp.data?.jsonObject ?: return LoginInfo()
        return LoginInfo(
            userId = data["user_id"]?.jsonPrimitive?.longOrNull ?: 0,
            nickname = data["nickname"]?.jsonPrimitive?.contentOrNull ?: "",
        )
    }

    /** 获取好友列表 */
    suspend fun getFriendList(): List<FriendInfo> {
        val resp = execute(GetFriendList)
        val data = resp.data?.jsonArray ?: return emptyList()
        return data.map { element ->
            val obj = element.jsonObject
            FriendInfo(
                userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0,
                nickname = obj["nickname"]?.jsonPrimitive?.contentOrNull ?: "",
                remark = obj["remark"]?.jsonPrimitive?.contentOrNull ?: "",
            )
        }
    }

    /** 获取群列表 */
    suspend fun getGroupList(): List<GroupInfo> {
        val resp = execute(GetGroupList)
        val data = resp.data?.jsonArray ?: return emptyList()
        return data.map { element ->
            val obj = element.jsonObject
            GroupInfo(
                groupId = obj["group_id"]?.jsonPrimitive?.longOrNull ?: 0,
                groupName = obj["group_name"]?.jsonPrimitive?.contentOrNull ?: "",
                memberCount = obj["member_count"]?.jsonPrimitive?.intOrNull ?: 0,
                maxMemberCount = obj["max_member_count"]?.jsonPrimitive?.intOrNull ?: 0,
            )
        }
    }

    /** 获取群成员列表 */
    suspend fun getGroupMemberList(groupId: Long): List<GroupMemberInfo> {
        val resp = execute(GetGroupMemberList(groupId))
        val data = resp.data?.jsonArray ?: return emptyList()
        return data.map { element ->
            val obj = element.jsonObject
            GroupMemberInfo(
                groupId = obj["group_id"]?.jsonPrimitive?.longOrNull ?: groupId,
                userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0,
                nickname = obj["nickname"]?.jsonPrimitive?.contentOrNull ?: "",
                card = obj["card"]?.jsonPrimitive?.contentOrNull ?: "",
                role = obj["role"]?.jsonPrimitive?.contentOrNull ?: "member",
            )
        }
    }

    /** 获取群信息 */
    suspend fun getGroupInfo(groupId: Long): GroupInfo {
        val resp = execute(GetGroupInfo(groupId))
        val data = resp.data?.jsonObject ?: return GroupInfo(groupId)
        return GroupInfo(
            groupId = data["group_id"]?.jsonPrimitive?.longOrNull ?: groupId,
            groupName = data["group_name"]?.jsonPrimitive?.contentOrNull ?: "",
            memberCount = data["member_count"]?.jsonPrimitive?.intOrNull ?: 0,
            maxMemberCount = data["max_member_count"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    /** 踢出群成员 */
    suspend fun setGroupKick(groupId: Long, userId: Long, rejectAddRequest: Boolean = false) {
        execute(SetGroupKick(groupId, userId, rejectAddRequest))
    }

    /** 群禁言 */
    suspend fun setGroupBan(groupId: Long, userId: Long, duration: Long = 1800) {
        execute(SetGroupBan(groupId, userId, duration))
    }

    /** 全员禁言 */
    suspend fun setGroupWholeBan(groupId: Long, enable: Boolean = true) {
        execute(SetGroupWholeBan(groupId, enable))
    }

    /** 设置群名片 */
    suspend fun setGroupCard(groupId: Long, userId: Long, card: String) {
        execute(SetGroupCard(groupId, userId, card))
    }

    /** 设置群名 */
    suspend fun setGroupName(groupId: Long, groupName: String) {
        execute(SetGroupName(groupId, groupName))
    }

    /** 退群 */
    suspend fun setGroupLeave(groupId: Long, isDismiss: Boolean = false) {
        execute(SetGroupLeave(groupId, isDismiss))
    }

    /** 处理好友请求 */
    suspend fun setFriendAddRequest(flag: String, approve: Boolean = true, remark: String = "") {
        execute(SetFriendAddRequest(flag, approve, remark))
    }

    /** 处理加群请求 */
    suspend fun setGroupAddRequest(flag: String, subType: String, approve: Boolean = true, reason: String = "") {
        execute(SetGroupAddRequest(flag, subType, approve, reason))
    }

    /** 获取版本信息 */
    suspend fun getVersionInfo(): VersionInfo {
        val resp = execute(GetVersionInfo)
        val data = resp.data?.jsonObject ?: return VersionInfo("", "")
        return VersionInfo(
            appName = data["app_name"]?.jsonPrimitive?.contentOrNull ?: "",
            appVersion = data["app_version"]?.jsonPrimitive?.contentOrNull ?: "",
            protocolVersion = data["protocol_version"]?.jsonPrimitive?.contentOrNull ?: "",
        )
    }

    /** 获取运行状态 */
    suspend fun getStatus(): StatusInfo {
        val resp = execute(GetStatus)
        val data = resp.data?.jsonObject ?: return StatusInfo()
        return StatusInfo(
            online = data["online"]?.jsonPrimitive?.booleanOrNull ?: true,
            good = data["good"]?.jsonPrimitive?.booleanOrNull ?: true,
        )
    }

    /** 快速回复消息 */
    suspend fun replyPrivate(userId: Long, text: String): Long {
        return sendPrivateMsg(userId, buildMessage { text(text) })
    }

    /** 快速回复群消息 */
    suspend fun replyGroup(groupId: Long, text: String): Long {
        return sendGroupMsg(groupId, buildMessage { text(text) })
    }

    // ============ 生命周期 ============

    suspend fun start() {
        logger.info("OneBot starting: app={}, version={}", config.appName, config.appVersion)

        connection.onEvent { event ->
            pipeline.processEvent(event) { evt ->
                eventBus.dispatch(evt)
            }
        }

        connection.start()
        started = true
        logger.info("OneBot started, mode={}",
            when {
                config.httpEnable -> "http"
                config.wsEnable -> "forward_ws"
                config.wsReverseEnable -> "reverse_ws"
                config.httpPostEnable -> "http_post"
                else -> "unknown"
            }
        )
    }

    suspend fun stop() {
        eventBus.shutdown()
        connection.stop()
        scope.cancel()
        started = false
        logger.info("OneBot stopped")
    }
}
