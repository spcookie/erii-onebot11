package uesugi.onebot.core.transport

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * kotlinx.serialization Json 实例工厂。
 *
 * 统一管理序列化配置。每个传输通道可获取不同配置的 Json 实例。
 */
@OptIn(ExperimentalSerializationApi::class)
object JsonFactory {

    private fun JsonBuilder.applyDefaults() {
        namingStrategy = JsonNamingStrategy.SnakeCase
        ignoreUnknownKeys = true
        isLenient = true
    }

    val base: Json = Json {
        applyDefaults()
        encodeDefaults = true
    }

    val lenient: Json = Json {
        applyDefaults()
        encodeDefaults = true
        coerceInputValues = true
    }

    val compact: Json = Json {
        applyDefaults()
        encodeDefaults = false
    }
}
