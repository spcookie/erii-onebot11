package uesugi.onebot.sdk.client.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.sdk.client.OneBotClient

// ===== 消息 API =====

suspend fun OneBotClient.sendPrivateMsg(userId: Long, message: MessageContent, autoEscape: Boolean = false): Int {
    val result = callWith(ActionName.SEND_PRIVATE_MSG, SendPrivateMsgRequest(userId, message, autoEscape))
    return (result as MessageIdResult).messageId
}

suspend fun OneBotClient.sendGroupMsg(groupId: Long, message: MessageContent, autoEscape: Boolean = false): Int {
    val result = callWith(ActionName.SEND_GROUP_MSG, SendGroupMsgRequest(groupId, message, autoEscape))
    return (result as MessageIdResult).messageId
}

suspend fun OneBotClient.sendMsg(messageType: String, userId: Long?, groupId: Long?, message: MessageContent): Int {
    val result = callWith(ActionName.SEND_MSG, SendMsgRequest(messageType, userId, groupId, message))
    return (result as MessageIdResult).messageId
}

suspend fun OneBotClient.deleteMsg(messageId: Int) {
    callWith(ActionName.DELETE_MSG, DeleteMsgRequest(messageId))
}

suspend fun OneBotClient.sendLike(userId: Long, times: Int = 1) {
    callWith(ActionName.SEND_LIKE, SendLikeRequest(userId, times))
}

suspend fun OneBotClient.getMsg(messageId: Int): MessageInfo {
    return callWith(ActionName.GET_MSG, GetMsgRequest(messageId)) as MessageInfo
}

suspend fun OneBotClient.getForwardMsg(id: String): MessageContent {
    val result = callWith(ActionName.GET_FORWARD_MSG, GetForwardMsgRequest(id))
    val data = (result as? RawActionResult)?.raw as? JsonObject ?: return emptyList()
    val message = data["message"]?.jsonArray ?: return emptyList()
    return message.map {
        JsonFactory.base.decodeFromJsonElement(MessageSegment.serializer(), it)
    }
}
