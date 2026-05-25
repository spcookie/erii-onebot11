package uesugi.onebot.core.parser

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.slf4j.LoggerFactory
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.JsonFactory

/**
 * Action 响应解析器（register 驱动）。
 *
 * 根据 action 名称查找注册的 [OneBotActionResult] 序列化器，完成：
 * - 反序列化：[deserialize] 将 [JsonElement] → 强类型响应对象
 * - 序列化：[serialize] 将强类型响应对象 → [JsonElement]
 *
 * 未知 action 回退到 [RawActionResult]。
 */
class ActionResultParser {
    private val json = JsonFactory.base
    private val logger = LoggerFactory.getLogger(ActionResultParser::class.java)
    private val serializers = mutableMapOf<String, KSerializer<*>>()

    init {
        // 消息发送 → MessageIdResult
        register(ActionName.SEND_PRIVATE_MSG, MessageIdResult.serializer())
        register(ActionName.SEND_GROUP_MSG, MessageIdResult.serializer())
        register(ActionName.SEND_MSG, MessageIdResult.serializer())

        // 消息获取
        register(ActionName.GET_MSG, MessageInfo.serializer())

        // 登录/用户信息
        register(ActionName.GET_LOGIN_INFO, LoginInfo.serializer())
        register(ActionName.GET_STRANGER_INFO, StrangerInfo.serializer())

        // 群信息
        register(ActionName.GET_GROUP_INFO, GroupInfo.serializer())
        register(ActionName.GET_GROUP_MEMBER_INFO, GroupMemberInfo.serializer())
        register(ActionName.GET_GROUP_HONOR_INFO, GroupHonorInfo.serializer())

        // 凭证
        register(ActionName.GET_COOKIES, CookiesInfo.serializer())
        register(ActionName.GET_CSRF_TOKEN, CsrfTokenInfo.serializer())
        register(ActionName.GET_CREDENTIALS, CredentialsInfo.serializer())

        // 媒体
        register(ActionName.GET_RECORD, FilePathInfo.serializer())
        register(ActionName.GET_IMAGE, FilePathInfo.serializer())

        // 能力检查
        register(ActionName.CAN_SEND_IMAGE, CanSendResult.serializer())
        register(ActionName.CAN_SEND_RECORD, CanSendResult.serializer())

        // 状态
        register(ActionName.GET_STATUS, StatusInfo.serializer())
        register(ActionName.GET_VERSION_INFO, VersionInfo.serializer())
    }

    fun <T : OneBotActionResult> register(action: String, serializer: KSerializer<T>) {
        logger.debug("Register result serializer: action={}, type={}", action, serializer.descriptor.serialName)
        serializers[action] = serializer
    }

    // ===== 反序列化：JsonElement → OneBotActionResult =====

    fun deserialize(action: String, element: JsonElement): OneBotActionResult {
        if (element is JsonNull) return RawActionResult(element)
        val serializer = serializers[action]
        if (serializer == null) {
            logger.debug("Unknown action result, falling back to RawActionResult: action={}", action)
            return RawActionResult(element)
        }
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromJsonElement(
            serializer as DeserializationStrategy<OneBotActionResult>,
            element
        )
    }

    // ===== 序列化：OneBotActionResult → JsonElement =====

    /**
     * 将强类型响应对象序列化为 [JsonElement]。
     *
     * 根据 action 名称选择注册的序列化器，将响应对象编码为 JSON。
     * 未注册的 action 回退到 [RawActionResult.raw]。
     */
    fun serialize(action: String, result: OneBotActionResult): JsonElement {
        if (result is RawActionResult) return result.raw
        val serializer = serializers[action]
        if (serializer != null) {
            @Suppress("UNCHECKED_CAST")
            return json.encodeToJsonElement(
                serializer as SerializationStrategy<OneBotActionResult>, result
            )
        }
        logger.debug("Unknown action for serialize result, falling back to JsonNull: action={}", action)
        return JsonNull
    }
}
