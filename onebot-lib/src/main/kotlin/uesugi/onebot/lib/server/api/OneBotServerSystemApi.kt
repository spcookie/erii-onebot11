package uesugi.onebot.lib.server.api

import kotlinx.serialization.json.JsonObject
import uesugi.onebot.core.model.*
import uesugi.onebot.lib.server.OneBotServer

// ===== 凭证 API =====

fun OneBotServer.onGetCookies(handler: suspend (GetCookiesRequest) -> OneBotActionResult) {
    onAction<GetCookiesRequest>(ActionName.GET_COOKIES, handler)
}

fun OneBotServer.onGetCsrfToken(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.GET_CSRF_TOKEN, handler)
}

fun OneBotServer.onGetCredentials(handler: suspend (GetCredentialsRequest) -> OneBotActionResult) {
    onAction<GetCredentialsRequest>(ActionName.GET_CREDENTIALS, handler)
}

// ===== 媒体 API =====

fun OneBotServer.onGetRecord(handler: suspend (GetRecordRequest) -> OneBotActionResult) {
    onAction<GetRecordRequest>(ActionName.GET_RECORD, handler)
}

fun OneBotServer.onGetImage(handler: suspend (GetImageRequest) -> OneBotActionResult) {
    onAction<GetImageRequest>(ActionName.GET_IMAGE, handler)
}

fun OneBotServer.onCanSendImage(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.CAN_SEND_IMAGE, handler)
}

fun OneBotServer.onCanSendRecord(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.CAN_SEND_RECORD, handler)
}

fun OneBotServer.onCanSendMarkdown(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.CAN_SEND_MARKDOWN, handler)
}

// ===== 系统 API =====

fun OneBotServer.onGetStatus(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.GET_STATUS, handler)
}

fun OneBotServer.onGetVersionInfo(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.GET_VERSION_INFO, handler)
}

fun OneBotServer.onSetRestart(handler: suspend (SetRestartRequest) -> OneBotActionResult) {
    onAction<SetRestartRequest>(ActionName.SET_RESTART, handler)
}

fun OneBotServer.onCleanCache(handler: suspend (JsonObject) -> OneBotActionResult) {
    onAction(ActionName.CLEAN_CACHE, handler)
}
