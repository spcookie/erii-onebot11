package uesugi.onebot.core.model

/**
 * OneBot 11 标准 Action 名称常量。
 * 所有 API action 的 wire-format 名称集中定义于此，供 SDK client 和 server 框架共用。
 */
object ActionName {
    // ===== 消息 =====
    const val SEND_PRIVATE_MSG = "send_private_msg"
    const val SEND_GROUP_MSG = "send_group_msg"
    const val SEND_MSG = "send_msg"
    const val DELETE_MSG = "delete_msg"
    const val GET_MSG = "get_msg"
    const val GET_FORWARD_MSG = "get_forward_msg"
    const val SEND_LIKE = "send_like"
    const val CAN_MARKDOWN = "can_markdown"

    // ===== 群管理 =====
    const val SET_GROUP_KICK = "set_group_kick"
    const val SET_GROUP_BAN = "set_group_ban"
    const val SET_GROUP_ANONYMOUS_BAN = "set_group_anonymous_ban"
    const val SET_GROUP_WHOLE_BAN = "set_group_whole_ban"
    const val SET_GROUP_ADMIN = "set_group_admin"
    const val SET_GROUP_ANONYMOUS = "set_group_anonymous"
    const val SET_GROUP_CARD = "set_group_card"
    const val SET_GROUP_NAME = "set_group_name"
    const val SET_GROUP_LEAVE = "set_group_leave"
    const val SET_GROUP_SPECIAL_TITLE = "set_group_special_title"

    // ===== 请求处理 =====
    const val SET_FRIEND_ADD_REQUEST = "set_friend_add_request"
    const val SET_GROUP_ADD_REQUEST = "set_group_add_request"

    // ===== 信息获取 =====
    const val GET_LOGIN_INFO = "get_login_info"
    const val GET_STRANGER_INFO = "get_stranger_info"
    const val GET_FRIEND_LIST = "get_friend_list"
    const val GET_GROUP_LIST = "get_group_list"
    const val GET_GROUP_INFO = "get_group_info"
    const val GET_GROUP_MEMBER_INFO = "get_group_member_info"
    const val GET_GROUP_MEMBER_LIST = "get_group_member_list"
    const val GET_GROUP_HONOR_INFO = "get_group_honor_info"

    // ===== 凭证 =====
    const val GET_COOKIES = "get_cookies"
    const val GET_CSRF_TOKEN = "get_csrf_token"
    const val GET_CREDENTIALS = "get_credentials"

    // ===== 媒体 =====
    const val GET_RECORD = "get_record"
    const val GET_IMAGE = "get_image"
    const val CAN_SEND_IMAGE = "can_send_image"
    const val CAN_SEND_RECORD = "can_send_record"

    // ===== 系统 =====
    const val GET_STATUS = "get_status"
    const val GET_VERSION_INFO = "get_version_info"
    const val SET_RESTART = "set_restart"
    const val CLEAN_CACHE = "clean_cache"
    const val HANDLE_QUICK_OPERATION = ".handle_quick_operation"
}