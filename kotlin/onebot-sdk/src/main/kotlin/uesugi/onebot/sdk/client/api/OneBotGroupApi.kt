package uesugi.onebot.sdk.client.api

import uesugi.onebot.core.model.*
import uesugi.onebot.sdk.client.OneBotClient

// ===== 群管理 API =====

suspend fun OneBotClient.setGroupKick(groupId: Long, userId: Long, rejectAddRequest: Boolean = false) {
    callWith(ActionName.SET_GROUP_KICK, SetGroupKickRequest(groupId, userId, rejectAddRequest))
}

suspend fun OneBotClient.setGroupBan(groupId: Long, userId: Long, duration: Long = 30 * 60) {
    callWith(ActionName.SET_GROUP_BAN, SetGroupBanRequest(groupId, userId, duration))
}

suspend fun OneBotClient.setGroupWholeBan(groupId: Long, enable: Boolean = true) {
    callWith(ActionName.SET_GROUP_WHOLE_BAN, SetGroupWholeBanRequest(groupId, enable))
}

suspend fun OneBotClient.setGroupAdmin(groupId: Long, userId: Long, enable: Boolean = true) {
    callWith(ActionName.SET_GROUP_ADMIN, SetGroupAdminRequest(groupId, userId, enable))
}

suspend fun OneBotClient.setGroupCard(groupId: Long, userId: Long, card: String = "") {
    callWith(ActionName.SET_GROUP_CARD, SetGroupCardRequest(groupId, userId, card))
}

suspend fun OneBotClient.setGroupName(groupId: Long, groupName: String) {
    callWith(ActionName.SET_GROUP_NAME, SetGroupNameRequest(groupId, groupName))
}

suspend fun OneBotClient.setGroupLeave(groupId: Long, isDismiss: Boolean = false) {
    callWith(ActionName.SET_GROUP_LEAVE, SetGroupLeaveRequest(groupId, isDismiss))
}

suspend fun OneBotClient.setGroupSpecialTitle(groupId: Long, userId: Long, specialTitle: String, duration: Long = -1) {
    callWith(ActionName.SET_GROUP_SPECIAL_TITLE, SetGroupSpecialTitleRequest(groupId, userId, specialTitle, duration))
}
