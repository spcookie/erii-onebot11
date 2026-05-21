package uesugi.onebot.mock.handlers

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import uesugi.onebot.lib.connection.GenericAction
import uesugi.onebot.lib.model.CleanCache
import uesugi.onebot.lib.model.DeleteMsg
import uesugi.onebot.lib.model.GetFriendList
import uesugi.onebot.lib.model.GetGroupList
import uesugi.onebot.lib.model.GetLoginInfo
import uesugi.onebot.lib.model.GetStatus
import uesugi.onebot.lib.model.GetVersionInfo
import uesugi.onebot.lib.model.SendGroupMsg
import uesugi.onebot.lib.model.SendMsg
import uesugi.onebot.lib.model.SendPrivateMsg
import uesugi.onebot.lib.model.SetGroupBan
import uesugi.onebot.lib.model.SetGroupKick
import uesugi.onebot.lib.model.SetGroupLeave
import uesugi.onebot.lib.model.SetRestart
import uesugi.onebot.lib.model.textSegment
import uesugi.onebot.mock.generators.MockData

/**
 * Helper to build a GenericAction with named params for testing handlers
 * that extract data via (action as? GenericAction)?.rawParams.
 */
private fun genericAction(actionName: String, vararg pairs: Pair<String, Any?>): GenericAction {
    val json = buildJsonObject {
        for ((key, value) in pairs) {
            when (value) {
                is Long -> put(key, value)
                is Int -> put(key, value)
                is String -> put(key, value)
                is Boolean -> put(key, value)
            }
        }
    }
    return GenericAction(actionName, json)
}

class MockActionHandlersTest {

    private val data = MockData()
    private val handlers = MockActionHandlers(data)

    // -- send_private_msg --

    @Test
    fun `send_private_msg returns ok with message_id`() = runTest {
        val response = handlers.handle(
            SendPrivateMsg(userId = 1001, message = listOf(textSegment("hi")))
        )
        assertEquals("ok", response.status)
        assertEquals(0, response.retcode)
        assertNotNull(response.data)
        val respData = response.data!!.jsonObject
        assertTrue(respData.containsKey("message_id"))
    }

    // -- send_group_msg --

    @Test
    fun `send_group_msg returns ok with message_id`() = runTest {
        val response = handlers.handle(
            SendGroupMsg(groupId = 2001, message = listOf(textSegment("hi")))
        )
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertTrue(respData.containsKey("message_id"))
    }

    // -- send_msg --

    @Test
    fun `send_msg returns ok with message_id`() = runTest {
        val response = handlers.handle(
            SendMsg(messageType = "private", userId = 1001, message = listOf(textSegment("hi")))
        )
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertTrue(respData.containsKey("message_id"))
    }

    // -- delete_msg --

    @Test
    fun `delete_msg returns ok`() = runTest {
        val response = handlers.handle(DeleteMsg(messageId = 1000))
        assertEquals("ok", response.status)
    }

    // -- get_msg (needs GenericAction for params) --

    @Test
    fun `get_msg returns message data`() = runTest {
        val action = genericAction("get_msg", "message_id" to 1000L)
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(1000L, respData["message_id"]!!.jsonPrimitive!!.long)
        assertTrue(respData.containsKey("sender"))
        assertTrue(respData.containsKey("message"))
    }

    // -- send_like --

    @Test
    fun `send_like returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SendLike(userId = 1001, times = 1)
        )
        assertEquals("ok", response.status)
    }

    // -- group management --

    @Test
    fun `set_group_kick returns ok`() = runTest {
        val response = handlers.handle(SetGroupKick(groupId = 2001, userId = 1011))
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_ban returns ok`() = runTest {
        val response = handlers.handle(SetGroupBan(groupId = 2001, userId = 1011, duration = 1800))
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_whole_ban returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetGroupWholeBan(groupId = 2001, enable = true)
        )
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_admin returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetGroupAdmin(groupId = 2001, userId = 1011)
        )
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_card returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetGroupCard(groupId = 2001, userId = 1011, card = "NewCard")
        )
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_name returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetGroupName(groupId = 2001, groupName = "NewName")
        )
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_leave returns ok`() = runTest {
        val response = handlers.handle(SetGroupLeave(groupId = 2001))
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_special_title returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetGroupSpecialTitle(
                groupId = 2001, userId = 1011, specialTitle = "VIP"
            )
        )
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_anonymous_ban returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetGroupAnonymousBan(groupId = 2001, duration = 1800)
        )
        assertEquals("ok", response.status)
    }

    // -- request handling --

    @Test
    fun `set_friend_add_request returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetFriendAddRequest(flag = "abc", approve = true)
        )
        assertEquals("ok", response.status)
    }

    @Test
    fun `set_group_add_request returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.SetGroupAddRequest(
                flag = "abc", subType = "invite", approve = true
            )
        )
        assertEquals("ok", response.status)
    }

    // -- get_login_info --

    @Test
    fun `get_login_info returns bot info`() = runTest {
        val response = handlers.handle(GetLoginInfo)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(10001L, respData["user_id"]!!.jsonPrimitive!!.long)
        assertEquals("MockBot", respData["nickname"]!!.jsonPrimitive!!.content)
    }

    // -- get_stranger_info (needs GenericAction for params) --

    @Test
    fun `get_stranger_info returns friend data for known user`() = runTest {
        val action = genericAction("get_stranger_info", "user_id" to 1001L)
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(1001L, respData["user_id"]!!.jsonPrimitive!!.long)
        assertEquals("Alice", respData["nickname"]!!.jsonPrimitive!!.content)
    }

    @Test
    fun `get_stranger_info returns default for unknown user`() = runTest {
        val action = genericAction("get_stranger_info", "user_id" to 9999L)
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals("Stranger", respData["nickname"]!!.jsonPrimitive!!.content)
    }

    // -- get_friend_list --

    @Test
    fun `get_friend_list returns friends array`() = runTest {
        val response = handlers.handle(GetFriendList)
        assertEquals("ok", response.status)
        val arr = response.data!!.jsonArray
        assertEquals(3, arr.size)
    }

    // -- get_group_info (needs GenericAction for params) --

    @Test
    fun `get_group_info returns group data`() = runTest {
        val action = genericAction("get_group_info", "group_id" to 2001L)
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(2001L, respData["group_id"]!!.jsonPrimitive!!.long)
        assertEquals("测试群A", respData["group_name"]!!.jsonPrimitive!!.content)
    }

    // -- get_group_list --

    @Test
    fun `get_group_list returns groups array`() = runTest {
        val response = handlers.handle(GetGroupList)
        assertEquals("ok", response.status)
        val arr = response.data!!.jsonArray
        assertEquals(3, arr.size)
    }

    // -- get_group_member_info (needs GenericAction for params) --

    @Test
    fun `get_group_member_info returns member data`() = runTest {
        val action = genericAction(
            "get_group_member_info",
            "group_id" to 2001L,
            "user_id" to 1011L,
        )
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(2001L, respData["group_id"]!!.jsonPrimitive!!.long)
        assertEquals(1011L, respData["user_id"]!!.jsonPrimitive!!.long)
    }

    // -- get_group_member_list (needs GenericAction for params) --

    @Test
    fun `get_group_member_list returns members for valid group`() = runTest {
        val action = genericAction("get_group_member_list", "group_id" to 2001L)
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val arr = response.data!!.jsonArray
        assertTrue(arr.isNotEmpty())
    }

    @Test
    fun `get_group_member_list returns empty for unknown group`() = runTest {
        val action = genericAction("get_group_member_list", "group_id" to 9999L)
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val arr = response.data!!.jsonArray
        assertEquals(0, arr.size)
    }

    // -- system/tool actions --

    @Test
    fun `get_status returns online status`() = runTest {
        val response = handlers.handle(GetStatus)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(true, respData["online"]!!.jsonPrimitive!!.boolean)
    }

    @Test
    fun `get_version_info returns version`() = runTest {
        val response = handlers.handle(GetVersionInfo)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals("mock-bot-server", respData["app_name"]!!.jsonPrimitive!!.content)
        assertEquals("v11", respData["protocol_version"]!!.jsonPrimitive!!.content)
    }

    @Test
    fun `set_restart returns async`() = runTest {
        val response = handlers.handle(SetRestart())
        assertEquals("async", response.status)
    }

    @Test
    fun `clean_cache returns ok`() = runTest {
        val response = handlers.handle(CleanCache)
        assertEquals("ok", response.status)
    }

    @Test
    fun `can_send_image returns yes`() = runTest {
        val response = handlers.handle(uesugi.onebot.lib.model.CanSendImage)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(true, respData["yes"]!!.jsonPrimitive!!.boolean)
    }

    @Test
    fun `can_send_record returns yes`() = runTest {
        val response = handlers.handle(uesugi.onebot.lib.model.CanSendRecord)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(true, respData["yes"]!!.jsonPrimitive!!.boolean)
    }

    @Test
    fun `get_cookies returns mock cookie`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.GetCookies(domain = "example.com")
        )
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals("mock_cookie=value", respData["cookies"]!!.jsonPrimitive!!.content)
    }

    @Test
    fun `get_csrf_token returns token`() = runTest {
        val response = handlers.handle(uesugi.onebot.lib.model.GetCsrfToken)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(42, respData["token"]!!.jsonPrimitive!!.int)
    }

    @Test
    fun `get_credentials returns credentials`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.GetCredentials(domain = "example.com")
        )
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals("mock_cookie=value", respData["cookies"]!!.jsonPrimitive!!.content)
    }

    @Test
    fun `get_record returns file`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.GetRecord(file = "test.mp3")
        )
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals("mock_record.mp3", respData["file"]!!.jsonPrimitive!!.content)
    }

    @Test
    fun `get_image returns file`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.GetImage(file = "test.png")
        )
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals("mock_image.png", respData["file"]!!.jsonPrimitive!!.content)
    }

    @Test
    fun `get_group_honor_info returns honor data`() = runTest {
        val action = genericAction("get_group_honor_info", "group_id" to 2001L)
        val response = handlers.handle(action)
        assertEquals("ok", response.status)
        val respData = response.data!!.jsonObject
        assertEquals(2001L, respData["group_id"]!!.jsonPrimitive!!.long)
        assertTrue(respData.containsKey("current_talkative"))
    }

    // -- get_forward_msg --

    @Test
    fun `get_forward_msg returns ok`() = runTest {
        val response = handlers.handle(
            uesugi.onebot.lib.model.GetForwardMsg(id = "test-id")
        )
        assertEquals("ok", response.status)
    }

    // -- unknown action --

    @Test
    fun `unknown action returns failed with retcode 404`() = runTest {
        val response = handlers.handle(
            GenericAction("unknown_action", JsonObject(emptyMap()))
        )
        assertEquals("failed", response.status)
        assertEquals(404, response.retcode)
    }

    // -- message ID counter increments --

    @Test
    fun `message ID counter increments across actions`() = runTest {
        val resp1 = handlers.handle(
            SendPrivateMsg(userId = 1001, message = listOf(textSegment("hi")))
        )
        val msgId1 = resp1.data!!.jsonObject["message_id"]!!.jsonPrimitive!!.long

        val resp2 = handlers.handle(
            SendGroupMsg(groupId = 2001, message = listOf(textSegment("hi")))
        )
        val msgId2 = resp2.data!!.jsonObject["message_id"]!!.jsonPrimitive!!.long

        assertEquals(msgId1 + 1, msgId2)
    }
}
