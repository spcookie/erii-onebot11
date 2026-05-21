package uesugi.onebot.mock.handlers

import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.connection.GenericAction
import uesugi.onebot.lib.model.*
import uesugi.onebot.mock.generators.MockData

/**
 * 所有 OneBot 动作的 mock 实现，返回模拟数据。
 */
class MockActionHandlers(private val data: MockData) {
    private val logger = LoggerFactory.getLogger(MockActionHandlers::class.java)

    suspend fun handle(action: Action): ActionResponse {
        logger.debug("Handling action: {}", action.actionName)

        return when (action.actionName) {
            // 消息发送
            "send_private_msg" -> ActionResponse.ok(
                JsonObject(mapOf("message_id" to JsonPrimitive(data.nextMsgId())))
            )
            "send_group_msg" -> ActionResponse.ok(
                JsonObject(mapOf("message_id" to JsonPrimitive(data.nextMsgId())))
            )
            "send_msg" -> ActionResponse.ok(
                JsonObject(mapOf("message_id" to JsonPrimitive(data.nextMsgId())))
            )
            "delete_msg" -> ActionResponse.ok()
            "get_msg" -> {
                val params = (action as? GenericAction)?.rawParams ?: JsonObject(emptyMap())
                val msgId = params["message_id"]?.jsonPrimitive?.longOrNull ?: 0
                ActionResponse.ok(buildJsonObject {
                    put("message_id", msgId)
                    put("real_id", msgId)
                    put("sender", buildJsonObject {
                        put("user_id", data.botUserId)
                        put("nickname", data.botNickname)
                    })
                    put("time", System.currentTimeMillis() / 1000)
                    put("message", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("data", buildJsonObject {
                                put("text", "mock message content")
                            })
                        })
                    })
                    put("raw_message", "mock message content")
                })
            }
            "get_forward_msg" -> ActionResponse.ok(buildJsonObject {
                put("messages", buildJsonArray { add(JsonPrimitive("mock forward")) })
            })
            "send_like" -> ActionResponse.ok()

            // 群管理
            "set_group_kick" -> ActionResponse.ok()
            "set_group_ban" -> ActionResponse.ok()
            "set_group_anonymous_ban" -> ActionResponse.ok()
            "set_group_whole_ban" -> ActionResponse.ok()
            "set_group_admin" -> ActionResponse.ok()
            "set_group_anonymous" -> ActionResponse.ok()
            "set_group_card" -> ActionResponse.ok()
            "set_group_name" -> ActionResponse.ok()
            "set_group_leave" -> ActionResponse.ok()
            "set_group_special_title" -> ActionResponse.ok()

            // 请求处理
            "set_friend_add_request" -> ActionResponse.ok()
            "set_group_add_request" -> ActionResponse.ok()

            // 信息查询
            "get_login_info" -> ActionResponse.ok(buildJsonObject {
                put("user_id", data.botUserId)
                put("nickname", data.botNickname)
            })
            "get_stranger_info" -> {
                val params = (action as? GenericAction)?.rawParams ?: JsonObject(emptyMap())
                val userId = params["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                val friend = data.friends.find { it.userId == userId }
                ActionResponse.ok(buildJsonObject {
                    put("user_id", userId)
                    put("nickname", friend?.nickname ?: "Stranger")
                    put("sex", "unknown")
                    put("age", 0)
                })
            }
            "get_friend_list" -> ActionResponse.ok(
                JsonArray(data.friends.map { f ->
                    buildJsonObject {
                        put("user_id", f.userId)
                        put("nickname", f.nickname)
                        put("remark", f.remark)
                    }
                })
            )
            "get_group_info" -> {
                val params = (action as? GenericAction)?.rawParams ?: JsonObject(emptyMap())
                val groupId = params["group_id"]?.jsonPrimitive?.longOrNull ?: 0
                val group = data.groups.find { it.groupId == groupId }
                ActionResponse.ok(buildJsonObject {
                    put("group_id", groupId)
                    put("group_name", group?.groupName ?: "Unknown")
                    put("member_count", group?.memberCount ?: 0)
                    put("max_member_count", group?.maxMemberCount ?: 0)
                })
            }
            "get_group_list" -> ActionResponse.ok(
                JsonArray(data.groups.map { g ->
                    buildJsonObject {
                        put("group_id", g.groupId)
                        put("group_name", g.groupName)
                        put("member_count", g.memberCount)
                        put("max_member_count", g.maxMemberCount)
                    }
                })
            )
            "get_group_member_info" -> {
                val params = (action as? GenericAction)?.rawParams ?: JsonObject(emptyMap())
                val groupId = params["group_id"]?.jsonPrimitive?.longOrNull ?: 0
                val userId = params["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                val member = data.groupMembers[groupId]?.find { it.userId == userId }
                ActionResponse.ok(buildJsonObject {
                    put("group_id", groupId)
                    put("user_id", userId)
                    put("nickname", member?.nickname ?: "")
                    put("card", member?.card ?: "")
                    put("role", member?.role ?: "member")
                })
            }
            "get_group_member_list" -> {
                val params = (action as? GenericAction)?.rawParams ?: JsonObject(emptyMap())
                val groupId = params["group_id"]?.jsonPrimitive?.longOrNull ?: 0
                val members = data.groupMembers[groupId] ?: emptyList()
                ActionResponse.ok(
                    JsonArray(members.map { m ->
                        buildJsonObject {
                            put("group_id", m.groupId)
                            put("user_id", m.userId)
                            put("nickname", m.nickname)
                            put("card", m.card)
                            put("sex", m.sex)
                            put("age", m.age)
                            put("role", m.role)
                            put("title", m.title)
                        }
                    })
                )
            }
            "get_group_honor_info" -> {
                val params = (action as? GenericAction)?.rawParams ?: JsonObject(emptyMap())
                val groupId = params["group_id"]?.jsonPrimitive?.longOrNull ?: 0
                ActionResponse.ok(buildJsonObject {
                    put("group_id", groupId)
                    put("current_talkative", buildJsonObject {
                        put("user_id", data.botUserId)
                        put("nickname", data.botNickname)
                        put("avatar", "")
                        put("day_count", 42)
                    })
                    put("talkative_list", buildJsonArray { })
                    put("performer_list", buildJsonArray { })
                    put("legend_list", buildJsonArray { })
                    put("strong_newbie_list", buildJsonArray { })
                    put("emotion_list", buildJsonArray { })
                })
            }

            // 系统/工具
            "get_cookies" -> ActionResponse.ok(
                JsonObject(mapOf("cookies" to JsonPrimitive("mock_cookie=value")))
            )
            "get_csrf_token" -> ActionResponse.ok(
                JsonObject(mapOf("token" to JsonPrimitive(42)))
            )
            "get_credentials" -> ActionResponse.ok(buildJsonObject {
                put("cookies", "mock_cookie=value")
                put("csrf_token", 42)
            })
            "get_record" -> ActionResponse.ok(
                JsonObject(mapOf("file" to JsonPrimitive("mock_record.mp3")))
            )
            "get_image" -> ActionResponse.ok(
                JsonObject(mapOf("file" to JsonPrimitive("mock_image.png")))
            )
            "can_send_image" -> ActionResponse.ok(
                JsonObject(mapOf("yes" to JsonPrimitive(true)))
            )
            "can_send_record" -> ActionResponse.ok(
                JsonObject(mapOf("yes" to JsonPrimitive(true)))
            )
            "get_status" -> ActionResponse.ok(buildJsonObject {
                put("online", true)
                put("good", true)
            })
            "get_version_info" -> ActionResponse.ok(buildJsonObject {
                put("app_name", "mock-bot-server")
                put("app_version", "1.0.0")
                put("protocol_version", "v11")
            })
            "set_restart" -> ActionResponse.async()
            "clean_cache" -> ActionResponse.ok()

            // 隐藏 API
            ".handle_quick_operation" -> ActionResponse.ok()

            else -> {
                logger.debug("Unknown action: {}", action.actionName)
                ActionResponse.failed(retcode = 404)
            }
        }
    }
}
