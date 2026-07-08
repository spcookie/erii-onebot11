package uesugi.onebot.core.parser

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import uesugi.onebot.core.codec.MessageFormatConverter
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.*
import uesugi.onebot.core.transport.JsonFactory

/**
 * Action 参数解析器（register 驱动）。
 *
 * 根据 action 名称查找注册的 [OneBotActionParams] 序列化器，完成：
 * - 反序列化：[deserialize] 将 [JsonObject] → 强类型参数对象
 * - 序列化：[serialize] 将强类型参数对象 → [JsonObject]
 *
 * 未知 action 回退到 [RawActionParams]。
 */
class ActionParamParser(
    private val messageFormat: String = MESSAGE_FORMAT_DEFAULT
) {
    private val json = JsonFactory.base
    private val logger = LoggerFactory.getLogger(ActionParamParser::class.java)
    private val serializers = mutableMapOf<String, KSerializer<*>>()

    companion object {
        const val MESSAGE_FORMAT_DEFAULT = OneBotConfig.MESSAGE_FORMAT_ARRAY
    }

    init {
        // 基础
        register(ActionName.GET_LOGIN_INFO, EmptyActionParams.serializer())
        register(ActionName.GET_STATUS, EmptyActionParams.serializer())
        register(ActionName.GET_VERSION_INFO, EmptyActionParams.serializer())

        // 消息
        register(ActionName.SEND_PRIVATE_MSG, SendPrivateMsgRequest.serializer())
        register(ActionName.SEND_GROUP_MSG, SendGroupMsgRequest.serializer())
        register(ActionName.SEND_MSG, SendMsgRequest.serializer())
        register(ActionName.DELETE_MSG, DeleteMsgRequest.serializer())
        register(ActionName.GET_MSG, GetMsgRequest.serializer())
        register(ActionName.GET_FORWARD_MSG, GetForwardMsgRequest.serializer())
        register(ActionName.SEND_LIKE, SendLikeRequest.serializer())

        // 群管理
        register(ActionName.SET_GROUP_KICK, SetGroupKickRequest.serializer())
        register(ActionName.SET_GROUP_BAN, SetGroupBanRequest.serializer())
        register(ActionName.SET_GROUP_ANONYMOUS_BAN, SetGroupAnonymousBanRequest.serializer())
        register(ActionName.SET_GROUP_WHOLE_BAN, SetGroupWholeBanRequest.serializer())
        register(ActionName.SET_GROUP_ADMIN, SetGroupAdminRequest.serializer())
        register(ActionName.SET_GROUP_ANONYMOUS, SetGroupAnonymousRequest.serializer())
        register(ActionName.SET_GROUP_CARD, SetGroupCardRequest.serializer())
        register(ActionName.SET_GROUP_NAME, SetGroupNameRequest.serializer())
        register(ActionName.SET_GROUP_LEAVE, SetGroupLeaveRequest.serializer())
        register(ActionName.SET_GROUP_SPECIAL_TITLE, SetGroupSpecialTitleRequest.serializer())

        // 请求处理
        register(ActionName.SET_FRIEND_ADD_REQUEST, SetFriendAddRequestRequest.serializer())
        register(ActionName.SET_GROUP_ADD_REQUEST, SetGroupAddRequestRequest.serializer())

        // 信息获取
        register(ActionName.GET_STRANGER_INFO, GetStrangerInfoRequest.serializer())
        register(ActionName.GET_GROUP_INFO, GetGroupInfoRequest.serializer())
        register(ActionName.GET_GROUP_MEMBER_INFO, GetGroupMemberInfoRequest.serializer())
        register(ActionName.GET_GROUP_MEMBER_LIST, GetGroupMemberListRequest.serializer())
        register(ActionName.GET_GROUP_HONOR_INFO, GetGroupHonorInfoRequest.serializer())

        // 凭证
        register(ActionName.GET_COOKIES, GetCookiesRequest.serializer())
        register(ActionName.GET_CREDENTIALS, GetCredentialsRequest.serializer())

        // 媒体
        register(ActionName.GET_RECORD, GetRecordRequest.serializer())
        register(ActionName.GET_IMAGE, GetImageRequest.serializer())

        // 系统
        register(ActionName.SET_RESTART, SetRestartRequest.serializer())
        register(ActionName.HANDLE_QUICK_OPERATION, HandleQuickOperationRequest.serializer())
    }

    fun <T : OneBotActionParams> register(action: String, serializer: KSerializer<T>) {
        logger.debug("Register action serializer: action={}, type={}", action, serializer.descriptor.serialName)
        serializers[action] = serializer
    }

    // ===== 反序列化：JsonObject → OneBotActionParams =====

    fun deserialize(action: String, params: JsonObject): OneBotActionParams {
        val serializer = serializers[action]
        if (serializer == null) {
            logger.debug("Unknown action, falling back to RawActionParams: action={}", action)
            return RawActionParams(params)
        }
        // 兼容 CQ 码字符串格式的 message 字段
        val processed = MessageFormatConverter.convertInput(params)
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromJsonElement(
            serializer as DeserializationStrategy<OneBotActionParams>,
            processed
        )
    }

    // ===== 序列化：OneBotActionParams → JsonObject =====

    /**
     * 将强类型参数对象序列化为 [JsonObject]。
     *
     * 根据 action 名称选择注册的序列化器，将参数对象编码为 JSON。
     * 未注册的 action 回退到 [RawActionParams.raw]。
     */
    fun serialize(action: String, params: OneBotActionParams): JsonObject {
        if (params is RawActionParams) return params.raw
        val serializer = serializers[action]
        val result = if (serializer != null) {
            @Suppress("UNCHECKED_CAST")
            json.encodeToJsonElement(
                serializer as SerializationStrategy<OneBotActionParams>, params
            ).jsonObject
        } else {
            logger.debug("Unknown action for serialize, falling back to empty: action={}", action)
            JsonObject(emptyMap())
        }
        return MessageFormatConverter.convertOutput(result, messageFormat)
    }
}
