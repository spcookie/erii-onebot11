package uesugi.onebot.lib.server.api

import uesugi.onebot.core.model.*
import uesugi.onebot.lib.server.OneBotServer

// ===== 群管理 API =====

fun OneBotServer.onSetGroupKick(handler: suspend (SetGroupKickRequest) -> OneBotActionResult) {
    onAction<SetGroupKickRequest>(ActionName.SET_GROUP_KICK, handler)
}

fun OneBotServer.onSetGroupBan(handler: suspend (SetGroupBanRequest) -> OneBotActionResult) {
    onAction<SetGroupBanRequest>(ActionName.SET_GROUP_BAN, handler)
}

fun OneBotServer.onSetGroupAnonymousBan(handler: suspend (SetGroupAnonymousBanRequest) -> OneBotActionResult) {
    onAction<SetGroupAnonymousBanRequest>(ActionName.SET_GROUP_ANONYMOUS_BAN, handler)
}

fun OneBotServer.onSetGroupWholeBan(handler: suspend (SetGroupWholeBanRequest) -> OneBotActionResult) {
    onAction<SetGroupWholeBanRequest>(ActionName.SET_GROUP_WHOLE_BAN, handler)
}

fun OneBotServer.onSetGroupAdmin(handler: suspend (SetGroupAdminRequest) -> OneBotActionResult) {
    onAction<SetGroupAdminRequest>(ActionName.SET_GROUP_ADMIN, handler)
}

fun OneBotServer.onSetGroupAnonymous(handler: suspend (SetGroupAnonymousRequest) -> OneBotActionResult) {
    onAction<SetGroupAnonymousRequest>(ActionName.SET_GROUP_ANONYMOUS, handler)
}

fun OneBotServer.onSetGroupCard(handler: suspend (SetGroupCardRequest) -> OneBotActionResult) {
    onAction<SetGroupCardRequest>(ActionName.SET_GROUP_CARD, handler)
}

fun OneBotServer.onSetGroupName(handler: suspend (SetGroupNameRequest) -> OneBotActionResult) {
    onAction<SetGroupNameRequest>(ActionName.SET_GROUP_NAME, handler)
}

fun OneBotServer.onSetGroupLeave(handler: suspend (SetGroupLeaveRequest) -> OneBotActionResult) {
    onAction<SetGroupLeaveRequest>(ActionName.SET_GROUP_LEAVE, handler)
}

fun OneBotServer.onSetGroupSpecialTitle(handler: suspend (SetGroupSpecialTitleRequest) -> OneBotActionResult) {
    onAction<SetGroupSpecialTitleRequest>(ActionName.SET_GROUP_SPECIAL_TITLE, handler)
}
