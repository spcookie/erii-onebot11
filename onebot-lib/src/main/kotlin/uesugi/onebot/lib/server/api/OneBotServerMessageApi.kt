package uesugi.onebot.lib.server.api

import uesugi.onebot.core.model.*
import uesugi.onebot.lib.server.OneBotServer

// ===== 消息 API =====

fun OneBotServer.onSendPrivateMsg(handler: suspend (SendPrivateMsgRequest) -> OneBotActionResult) {
    onAction<SendPrivateMsgRequest>(ActionName.SEND_PRIVATE_MSG, handler)
}

fun OneBotServer.onSendGroupMsg(handler: suspend (SendGroupMsgRequest) -> OneBotActionResult) {
    onAction<SendGroupMsgRequest>(ActionName.SEND_GROUP_MSG, handler)
}

fun OneBotServer.onSendMsg(handler: suspend (SendMsgRequest) -> OneBotActionResult) {
    onAction<SendMsgRequest>(ActionName.SEND_MSG, handler)
}

fun OneBotServer.onDeleteMsg(handler: suspend (DeleteMsgRequest) -> OneBotActionResult) {
    onAction<DeleteMsgRequest>(ActionName.DELETE_MSG, handler)
}

fun OneBotServer.onGetMsg(handler: suspend (GetMsgRequest) -> OneBotActionResult) {
    onAction<GetMsgRequest>(ActionName.GET_MSG, handler)
}

fun OneBotServer.onGetForwardMsg(handler: suspend (GetForwardMsgRequest) -> OneBotActionResult) {
    onAction<GetForwardMsgRequest>(ActionName.GET_FORWARD_MSG, handler)
}

fun OneBotServer.onSendLike(handler: suspend (SendLikeRequest) -> OneBotActionResult) {
    onAction<SendLikeRequest>(ActionName.SEND_LIKE, handler)
}
