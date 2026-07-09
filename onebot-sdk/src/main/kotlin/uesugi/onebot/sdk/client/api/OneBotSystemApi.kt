package uesugi.onebot.sdk.client.api

import kotlinx.serialization.json.JsonObject
import uesugi.onebot.core.model.*
import uesugi.onebot.sdk.client.OneBotClient

// ===== 系统 API =====

suspend fun OneBotClient.canSendImage(): Boolean {
    val result = call(ActionName.CAN_SEND_IMAGE, JsonObject(emptyMap()))
    return (result as CanSendResult).yes
}

suspend fun OneBotClient.canSendRecord(): Boolean {
    val result = call(ActionName.CAN_SEND_RECORD, JsonObject(emptyMap()))
    return (result as CanSendResult).yes
}

suspend fun OneBotClient.canSendMarkdown(): Boolean {
    val result = call(ActionName.CAN_MARKDOWN, JsonObject(emptyMap()))
    return (result as CanSendResult).yes
}

suspend fun OneBotClient.getStatus(): StatusInfo {
    return call(ActionName.GET_STATUS, JsonObject(emptyMap())) as StatusInfo
}

suspend fun OneBotClient.getVersionInfo(): VersionInfo {
    return call(ActionName.GET_VERSION_INFO, JsonObject(emptyMap())) as VersionInfo
}

suspend fun OneBotClient.setRestart(delay: Long = 0) {
    callWith(ActionName.SET_RESTART, SetRestartRequest(delay))
}

suspend fun OneBotClient.cleanCache() {
    call(ActionName.CLEAN_CACHE, JsonObject(emptyMap()))
}

suspend fun OneBotClient.getCookies(domain: String = ""): CookiesInfo {
    return callWith(ActionName.GET_COOKIES, GetCookiesRequest(domain)) as CookiesInfo
}

suspend fun OneBotClient.getCsrfToken(): CsrfTokenInfo {
    return call(ActionName.GET_CSRF_TOKEN, JsonObject(emptyMap())) as CsrfTokenInfo
}

suspend fun OneBotClient.getCredentials(domain: String = ""): CredentialsInfo {
    return callWith(ActionName.GET_CREDENTIALS, GetCredentialsRequest(domain)) as CredentialsInfo
}

suspend fun OneBotClient.getRecord(file: String, outFormat: String = "mp3"): FilePathInfo {
    return callWith(ActionName.GET_RECORD, GetRecordRequest(file, outFormat)) as FilePathInfo
}

suspend fun OneBotClient.getImage(file: String): FilePathInfo {
    return callWith(ActionName.GET_IMAGE, GetImageRequest(file)) as FilePathInfo
}
