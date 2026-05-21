package uesugi.onebot.mock.generators

import uesugi.onebot.lib.model.*

/**
 * 预置模拟数据集。
 */
class MockData {
    val botUserId: Long = 10001
    val botNickname: String = "MockBot"

    private var nextMessageId = 1000L

    val friends = listOf(
        FriendInfo(1001, "Alice", "Alice备注"),
        FriendInfo(1002, "Bob", ""),
        FriendInfo(1003, "Charlie", "查理"),
    )

    val groups = listOf(
        GroupInfo(2001, "测试群A", 50, 200),
        GroupInfo(2002, "测试群B", 30, 200),
        GroupInfo(2003, "开发交流群", 120, 500),
    )

    // groupId → members
    val groupMembers: Map<Long, List<GroupMemberInfo>> = mapOf(
        2001L to listOf(
            GroupMemberInfo(2001, 10001, botNickname, "Bot", "unknown", 0, "", 0, 0, "member", "owner", "Bot", 0),
            GroupMemberInfo(2001, 1011, "Alice", "爱丽丝", "female", 20, "", 0, 0, "admin", "admin", "", 0),
            GroupMemberInfo(2001, 1012, "Bob", "", "male", 22, "", 0, 0, "member", "member", "", 0),
        ),
        2002L to listOf(
            GroupMemberInfo(2002, 10001, botNickname, "", "unknown", 0, "", 0, 0, "member", "member", "", 0),
        ),
        2003L to listOf(
            GroupMemberInfo(2003, 10001, botNickname, "", "unknown", 0, "", 0, 0, "member", "admin", "", 0),
        ),
    )

    fun nextMsgId(): Long = nextMessageId++
}
