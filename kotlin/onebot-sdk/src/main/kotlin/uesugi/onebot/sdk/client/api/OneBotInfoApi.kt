package uesugi.onebot.sdk.client.api

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.JsonFactory
import uesugi.onebot.sdk.client.OneBotClient

// ===== 信息获取 API =====

suspend fun OneBotClient.getLoginInfo(): LoginInfo {
    return call(ActionName.GET_LOGIN_INFO, JsonObject(emptyMap())) as LoginInfo
}

suspend fun OneBotClient.getFriendList(): List<FriendInfo> {
    val result = call(ActionName.GET_FRIEND_LIST, JsonObject(emptyMap()))
    return JsonFactory.base.decodeFromJsonElement(
        ListSerializer(FriendInfo.serializer()), (result as RawActionResult).raw
    )
}

suspend fun OneBotClient.getGroupList(): List<GroupInfo> {
    val result = call(ActionName.GET_GROUP_LIST, JsonObject(emptyMap()))
    return JsonFactory.base.decodeFromJsonElement(
        ListSerializer(GroupInfo.serializer()), (result as RawActionResult).raw
    )
}

suspend fun OneBotClient.getGroupInfo(groupId: Long, noCache: Boolean = false): GroupInfo {
    return callWith(ActionName.GET_GROUP_INFO, GetGroupInfoRequest(groupId, noCache)) as GroupInfo
}

suspend fun OneBotClient.getGroupMemberInfo(groupId: Long, userId: Long, noCache: Boolean = false): GroupMemberInfo {
    return callWith(
        ActionName.GET_GROUP_MEMBER_INFO,
        GetGroupMemberInfoRequest(groupId, userId, noCache)
    ) as GroupMemberInfo
}

suspend fun OneBotClient.getGroupMemberList(groupId: Long): List<GroupMemberInfo> {
    val result = callWith(ActionName.GET_GROUP_MEMBER_LIST, GetGroupMemberListRequest(groupId))
    return JsonFactory.base.decodeFromJsonElement(
        ListSerializer(GroupMemberInfo.serializer()), (result as RawActionResult).raw
    )
}

suspend fun OneBotClient.getStrangerInfo(userId: Long, noCache: Boolean = false): StrangerInfo {
    return callWith(ActionName.GET_STRANGER_INFO, GetStrangerInfoRequest(userId, noCache)) as StrangerInfo
}

suspend fun OneBotClient.getGroupHonorInfo(groupId: Long, type: String = "all"): GroupHonorInfo {
    return callWith(ActionName.GET_GROUP_HONOR_INFO, GetGroupHonorInfoRequest(groupId, type)) as GroupHonorInfo
}
