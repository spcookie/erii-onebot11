package uesugi.onebot.core.model

import java.util.concurrent.ConcurrentHashMap

/**
 * 快速操作回调句柄。
 *
 * 由传输层在事件分发前绑定，事件处理器调用 [respond] 即可触发快速操作。
 * - HTTP 模式：回调通过 CompletableDeferred 同步返回给 HttpEventServer
 * - WS 模式：回调通过 API channel 调用 .handle_quick_operation
 */
fun interface QuickOpHandler {
    suspend fun respond(operation: QuickOperation)
}

private val handlerBindings = ConcurrentHashMap<OneBotEvent, QuickOpHandler>()

/** 事件携带的快速操作回调，由传输层注入 */
var OneBotEvent.quickOpHandler: QuickOpHandler?
    get() = handlerBindings[this]
    set(value) {
        if (value != null) handlerBindings[this] = value
        else handlerBindings.remove(this)
    }

/** 便捷方法：对当前事件执行快速操作 */
suspend fun OneBotEvent.respondQuickOp(operation: QuickOperation) {
    quickOpHandler?.respond(operation)
}

// ===== 私聊消息 =====

/** 回复私聊消息 */
suspend fun PrivateMessageEvent.reply(message: MessageContent, autoEscape: Boolean = false) {
    respondQuickOp(QuickOperation(reply = message, autoEscape = autoEscape))
}

// ===== 群聊消息 =====

/** 回复群聊消息，可选择是否 @发送者 */
suspend fun GroupMessageEvent.reply(
    message: MessageContent,
    autoEscape: Boolean = false,
    atSender: Boolean = true
) {
    respondQuickOp(QuickOperation(reply = message, autoEscape = autoEscape, atSender = atSender))
}

/** 撤回当前群消息 */
suspend fun GroupMessageEvent.delete() {
    respondQuickOp(QuickOperation(delete = true))
}

/** 踢出发送者 */
suspend fun GroupMessageEvent.kick() {
    respondQuickOp(QuickOperation(kick = true))
}

/** 禁言发送者，默认 30 分钟 */
suspend fun GroupMessageEvent.ban(duration: Long = 30 * 60) {
    respondQuickOp(QuickOperation(ban = true, banDuration = duration))
}

// ===== 好友请求 =====

/** 同意好友请求 */
suspend fun FriendRequestEvent.approve(remark: String = "") {
    respondQuickOp(QuickOperation(approve = true, remark = remark))
}

/** 拒绝好友请求 */
suspend fun FriendRequestEvent.refuse(reason: String = "") {
    respondQuickOp(QuickOperation(approve = false, reason = reason))
}

// ===== 群请求 =====

/** 同意群请求/邀请 */
suspend fun GroupRequestEvent.approve(reason: String = "") {
    respondQuickOp(QuickOperation(approve = true, reason = reason))
}

/** 拒绝群请求/邀请 */
suspend fun GroupRequestEvent.refuse(reason: String = "") {
    respondQuickOp(QuickOperation(approve = false, reason = reason))
}
