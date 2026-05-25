package uesugi.onebot.sdk.client.api

import uesugi.onebot.core.model.*
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.emptyParams

// ===== 信息获取 API =====

suspend fun OneBotClient.getLoginInfo(): LoginInfo {
    val resp = call(ActionName.GET_LOGIN_INFO, emptyParams)
    return parseResponse(resp)
}

suspend fun OneBotClient.getFriendList(): List<FriendInfo> {
    val resp = call(ActionName.GET_FRIEND_LIST, emptyParams)
    return parseResponseList(resp)
}

suspend fun OneBotClient.getGroupList(): List<GroupInfo> {
    val resp = call(ActionName.GET_GROUP_LIST, emptyParams)
    return parseResponseList(resp)
}

suspend fun OneBotClient.getGroupInfo(groupId: Long, noCache: Boolean = false): GroupInfo {
    val resp = callWith(ActionName.GET_GROUP_INFO, GetGroupInfoRequest(groupId, noCache))
    return parseResponse(resp)
}

suspend fun OneBotClient.getGroupMemberInfo(groupId: Long, userId: Long, noCache: Boolean = false): GroupMemberInfo {
    val resp = callWith(ActionName.GET_GROUP_MEMBER_INFO, GetGroupMemberInfoRequest(groupId, userId, noCache))
    return parseResponse(resp)
}

suspend fun OneBotClient.getGroupMemberList(groupId: Long): List<GroupMemberInfo> {
    val resp = callWith(ActionName.GET_GROUP_MEMBER_LIST, GetGroupMemberListRequest(groupId))
    return parseResponseList(resp)
}

suspend fun OneBotClient.getStrangerInfo(userId: Long, noCache: Boolean = false): StrangerInfo {
    val resp = callWith(ActionName.GET_STRANGER_INFO, GetStrangerInfoRequest(userId, noCache))
    return parseResponse(resp)
}

suspend fun OneBotClient.getGroupHonorInfo(groupId: Long, type: String = "all"): GroupHonorInfo {
    val resp = callWith(ActionName.GET_GROUP_HONOR_INFO, GetGroupHonorInfoRequest(groupId, type))
    return parseResponse(resp)
}
