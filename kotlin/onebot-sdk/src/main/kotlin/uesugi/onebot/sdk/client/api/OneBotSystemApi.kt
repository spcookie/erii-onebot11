package uesugi.onebot.sdk.client.api

import uesugi.onebot.core.model.*
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.emptyParams

// ===== 系统 API =====

suspend fun OneBotClient.canSendImage(): Boolean {
    val resp = call(ActionName.CAN_SEND_IMAGE, emptyParams)
    return parseResponse<CanSendResult>(resp).yes
}

suspend fun OneBotClient.canSendRecord(): Boolean {
    val resp = call(ActionName.CAN_SEND_RECORD, emptyParams)
    return parseResponse<CanSendResult>(resp).yes
}

suspend fun OneBotClient.getStatus(): StatusInfo {
    val resp = call(ActionName.GET_STATUS, emptyParams)
    return parseResponse(resp)
}

suspend fun OneBotClient.getVersionInfo(): VersionInfo {
    val resp = call(ActionName.GET_VERSION_INFO, emptyParams)
    return parseResponse(resp)
}

suspend fun OneBotClient.setRestart(delay: Long = 0) {
    callWith(ActionName.SET_RESTART, SetRestartRequest(delay))
}

suspend fun OneBotClient.cleanCache() {
    call(ActionName.CLEAN_CACHE, emptyParams)
}

suspend fun OneBotClient.getCookies(domain: String = ""): CookiesInfo {
    val resp = callWith(ActionName.GET_COOKIES, GetCookiesRequest(domain))
    return parseResponse(resp)
}

suspend fun OneBotClient.getCsrfToken(): CsrfTokenInfo {
    val resp = call(ActionName.GET_CSRF_TOKEN, emptyParams)
    return parseResponse(resp)
}

suspend fun OneBotClient.getCredentials(domain: String = ""): CredentialsInfo {
    val resp = callWith(ActionName.GET_CREDENTIALS, GetCredentialsRequest(domain))
    return parseResponse(resp)
}

suspend fun OneBotClient.getRecord(file: String, outFormat: String = "mp3"): FilePathInfo {
    val resp = callWith(ActionName.GET_RECORD, GetRecordRequest(file, outFormat))
    return parseResponse(resp)
}

suspend fun OneBotClient.getImage(file: String): FilePathInfo {
    val resp = callWith(ActionName.GET_IMAGE, GetImageRequest(file))
    return parseResponse(resp)
}
