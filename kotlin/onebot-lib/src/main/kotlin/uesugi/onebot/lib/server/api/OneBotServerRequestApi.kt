package uesugi.onebot.lib.server.api

import uesugi.onebot.core.model.ActionName
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.model.SetFriendAddRequestRequest
import uesugi.onebot.core.model.SetGroupAddRequestRequest
import uesugi.onebot.lib.server.OneBotServer

// ===== 请求处理 API =====

fun OneBotServer.onSetFriendAddRequest(handler: suspend (SetFriendAddRequestRequest) -> OneBotActionResult) {
    onAction<SetFriendAddRequestRequest>(ActionName.SET_FRIEND_ADD_REQUEST, handler)
}

fun OneBotServer.onSetGroupAddRequest(handler: suspend (SetGroupAddRequestRequest) -> OneBotActionResult) {
    onAction<SetGroupAddRequestRequest>(ActionName.SET_GROUP_ADD_REQUEST, handler)
}
