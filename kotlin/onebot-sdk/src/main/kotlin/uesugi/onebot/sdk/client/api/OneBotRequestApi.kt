package uesugi.onebot.sdk.client.api

import uesugi.onebot.core.model.ActionName
import uesugi.onebot.core.model.SetFriendAddRequestRequest
import uesugi.onebot.core.model.SetGroupAddRequestRequest
import uesugi.onebot.sdk.client.OneBotClient

// ===== 请求处理 API =====

suspend fun OneBotClient.setFriendAddRequest(flag: String, approve: Boolean = true, remark: String = "") {
    callWith(ActionName.SET_FRIEND_ADD_REQUEST, SetFriendAddRequestRequest(flag, approve, remark))
}

suspend fun OneBotClient.setGroupAddRequest(
    flag: String,
    subType: String = "add",
    approve: Boolean = true,
    reason: String = ""
) {
    callWith(
        ActionName.SET_GROUP_ADD_REQUEST,
        SetGroupAddRequestRequest(flag, subType, approve = approve, reason = reason)
    )
}
