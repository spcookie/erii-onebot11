package uesugi.onebot.lib.transport

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal fun hmacSha1(data: String, key: String): String {
    val mac = Mac.getInstance("HmacSHA1")
    val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1")
    mac.init(keySpec)
    val raw = mac.doFinal(data.toByteArray(Charsets.UTF_8))
    return raw.joinToString("") { "%02x".format(it) }
}
