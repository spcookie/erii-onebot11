package uesugi.onebot.lib.model

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ActionSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SendPrivateMsg serialization`() {
        val action = SendPrivateMsg(userId = 123456, message = listOf(textSegment("hello")))
        val params = action.toParams()
        assertEquals(123456L, params["user_id"]?.jsonPrimitive?.long)
        assertEquals("send_private_msg", action.actionName)
    }

    @Test
    fun `SendGroupMsg serialization`() {
        val action = SendGroupMsg(groupId = 789, message = listOf(textSegment("hi")))
        val params = action.toParams()
        assertEquals(789L, params["group_id"]?.jsonPrimitive?.long)
        assertEquals("send_group_msg", action.actionName)
    }

    @Test
    fun `SendMsg serialization`() {
        val action = SendMsg(messageType = "private", userId = 1, message = listOf(textSegment("x")))
        val params = action.toParams()
        assertEquals("private", params["message_type"]?.jsonPrimitive?.content)
        assertEquals("send_msg", action.actionName)
    }

    @Test
    fun `DeleteMsg serialization`() {
        val action = DeleteMsg(messageId = 42)
        val params = action.toParams()
        assertEquals(42L, params["message_id"]?.jsonPrimitive?.long)
    }

    @Test
    fun `SendLike serialization`() {
        val action = SendLike(userId = 123, times = 5)
        val params = action.toParams()
        assertEquals(123L, params["user_id"]?.jsonPrimitive?.long)
        assertEquals(5, params["times"]?.jsonPrimitive?.int)
    }

    @Test
    fun `SetGroupKick with reject`() {
        val action = SetGroupKick(groupId = 1, userId = 2, rejectAddRequest = true)
        val params = action.toParams()
        assertEquals(true, params["reject_add_request"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `SetGroupBan serialization`() {
        val action = SetGroupBan(groupId = 1, userId = 2, duration = 3600)
        val params = action.toParams()
        assertEquals(3600L, params["duration"]?.jsonPrimitive?.long)
    }

    @Test
    fun `SetGroupAnonymousBan without flag`() {
        val action = SetGroupAnonymousBan(groupId = 1, duration = 60)
        assertEquals("set_group_anonymous_ban", action.actionName)
        val params = action.toParams()
        assertEquals(false, params.containsKey("anonymous_flag"))
    }

    @Test
    fun `SetGroupWholeBan disable`() {
        val action = SetGroupWholeBan(groupId = 1, enable = false)
        val params = action.toParams()
        assertEquals(false, params["enable"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `SetGroupAdmin serialization`() {
        val action = SetGroupAdmin(groupId = 1, userId = 2, enable = false)
        assertEquals("set_group_admin", action.actionName)
    }

    @Test
    fun `SetGroupCard serialization`() {
        val action = SetGroupCard(groupId = 1, userId = 2, card = "NewCard")
        assertEquals("set_group_card", action.actionName)
    }

    @Test
    fun `SetGroupName serialization`() {
        val action = SetGroupName(groupId = 1, groupName = "New Name")
        assertEquals("set_group_name", action.actionName)
    }

    @Test
    fun `SetGroupLeave with dismiss`() {
        val action = SetGroupLeave(groupId = 1, isDismiss = true)
        val params = action.toParams()
        assertEquals(true, params["is_dismiss"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `SetGroupSpecialTitle serialization`() {
        val action = SetGroupSpecialTitle(groupId = 1, userId = 2, specialTitle = "VIP")
        assertEquals("set_group_special_title", action.actionName)
    }

    @Test
    fun `SetFriendAddRequest deny`() {
        val action = SetFriendAddRequest(flag = "abc", approve = false, remark = "no")
        val params = action.toParams()
        assertEquals(false, params["approve"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `SetGroupAddRequest serialization`() {
        val action = SetGroupAddRequest(flag = "abc", subType = "invite", approve = true, reason = "welcome")
        assertEquals("set_group_add_request", action.actionName)
    }

    @Test
    fun `GetLoginInfo is object`() {
        assertEquals("get_login_info", GetLoginInfo.actionName)
        assertEquals(0, GetLoginInfo.toParams().size)
    }

    @Test
    fun `GetFriendList is object`() {
        assertEquals("get_friend_list", GetFriendList.actionName)
        assertEquals(0, GetFriendList.toParams().size)
    }

    @Test
    fun `GetGroupList is object`() {
        assertEquals("get_group_list", GetGroupList.actionName)
        assertEquals(0, GetGroupList.toParams().size)
    }

    @Test
    fun `GetGroupMemberList with groupId`() {
        val action = GetGroupMemberList(groupId = 456)
        val params = action.toParams()
        assertEquals(456L, params["group_id"]?.jsonPrimitive?.long)
    }

    @Test
    fun `CanSendImage is object`() {
        assertEquals("can_send_image", CanSendImage.actionName)
    }

    @Test
    fun `GetStatus is object`() {
        assertEquals("get_status", GetStatus.actionName)
    }

    @Test
    fun `GetVersionInfo is object`() {
        assertEquals("get_version_info", GetVersionInfo.actionName)
    }

    @Test
    fun `GetCsrfToken is object`() {
        assertEquals("get_csrf_token", GetCsrfToken.actionName)
    }

    @Test
    fun `CleanCache is object`() {
        assertEquals("clean_cache", CleanCache.actionName)
    }

    @Test
    fun `SetRestart with delay`() {
        val action = SetRestart(delay = 5000)
        val params = action.toParams()
        assertEquals(5000L, params["delay"]?.jsonPrimitive?.long)
    }

    @Test
    fun `GetGroupHonorInfo with type`() {
        val action = GetGroupHonorInfo(groupId = 1, type = "talkative")
        val params = action.toParams()
        assertEquals("talkative", params["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `SendPrivateMsg autoEscape default false`() {
        val action = SendPrivateMsg(userId = 1, message = listOf(textSegment("hi")))
        val params = action.toParams()
        assertEquals(false, params["auto_escape"]?.jsonPrimitive?.boolean)
    }
}
