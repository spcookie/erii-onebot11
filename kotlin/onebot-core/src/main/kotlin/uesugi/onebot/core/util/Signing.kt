package uesugi.onebot.core.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA1 签名工具。
 *
 * OneBot HTTP POST 模式支持通过 X-Signature 头对事件进行签名，
 * 格式：X-Signature: sha1=<hmac_hex>
 */
object Signing {

    private const val HMAC_SHA1 = "HmacSHA1"

    private val macThreadLocal: ThreadLocal<Mac> = ThreadLocal.withInitial {
        Mac.getInstance(HMAC_SHA1)
    }

    private val keySpecThreadLocal: ThreadLocal<MutableMap<String, SecretKeySpec>> =
        ThreadLocal.withInitial { mutableMapOf() }

    private fun getKeySpec(secret: String): SecretKeySpec =
        keySpecThreadLocal.get().getOrPut(secret) {
            SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA1)
        }

    fun signBody(body: ByteArray, secret: String): String {
        val mac = macThreadLocal.get()
        mac.init(getKeySpec(secret))
        val hash = mac.doFinal(body)
        val hex = hash.joinToString("") { "%02x".format(it) }
        return "sha1=$hex"
    }

    fun signBody(body: String, secret: String): String =
        signBody(body.toByteArray(Charsets.UTF_8), secret)

    /** 常量时间比较，防止时序攻击。 */
    fun verifySign(body: ByteArray, signature: String?, secret: String): Boolean {
        if (signature == null) return false
        val expected = signBody(body, secret)
        if (expected.length != signature.length) return false
        return expected.indices.fold(true) { acc, i -> acc && expected[i] == signature[i] }
    }

    fun verifySign(body: String, signature: String?, secret: String): Boolean =
        verifySign(body.toByteArray(Charsets.UTF_8), signature, secret)
}
