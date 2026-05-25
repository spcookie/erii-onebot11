package uesugi.onebot.lib.server.api

import kotlinx.serialization.json.JsonObject
import uesugi.onebot.core.model.*
import uesugi.onebot.lib.server.OneBotServer

// ===== 信息获取 API =====

fun OneBotServer.onGetLoginInfo(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.GET_LOGIN_INFO, handler)
}

fun OneBotServer.onGetStrangerInfo(handler: suspend (GetStrangerInfoRequest) -> OneBotActionResult) {
    onAction<GetStrangerInfoRequest>(ActionName.GET_STRANGER_INFO, handler)
}

fun OneBotServer.onGetFriendList(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.GET_FRIEND_LIST, handler)
}

fun OneBotServer.onGetGroupList(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.GET_GROUP_LIST, handler)
}

fun OneBotServer.onGetGroupInfo(handler: suspend (GetGroupInfoRequest) -> OneBotActionResult) {
    onAction<GetGroupInfoRequest>(ActionName.GET_GROUP_INFO, handler)
}

fun OneBotServer.onGetGroupMemberInfo(handler: suspend (GetGroupMemberInfoRequest) -> OneBotActionResult) {
    onAction<GetGroupMemberInfoRequest>(ActionName.GET_GROUP_MEMBER_INFO, handler)
}

fun OneBotServer.onGetGroupMemberList(handler: suspend (GetGroupMemberListRequest) -> OneBotActionResult) {
    onAction<GetGroupMemberListRequest>(ActionName.GET_GROUP_MEMBER_LIST, handler)
}

fun OneBotServer.onGetGroupHonorInfo(handler: suspend (GetGroupHonorInfoRequest) -> OneBotActionResult) {
    onAction<GetGroupHonorInfoRequest>(ActionName.GET_GROUP_HONOR_INFO, handler)
}
