package uesugi.onebot.mock.storage

import uesugi.onebot.core.model.*

interface MockStorage {
    val selfId: Long

    // ===== Users =====
    fun addUser(user: Sender)
    fun getUser(userId: Long): Sender?
    fun getAllUsers(): List<Sender>

    // ===== Friends =====
    fun addFriend(friend: FriendInfo)
    fun getFriend(userId: Long): FriendInfo?
    fun getAllFriends(): List<FriendInfo>

    // ===== Groups =====
    fun addGroup(group: GroupInfo)
    fun getGroup(groupId: Long): GroupInfo?
    fun getAllGroups(): List<GroupInfo>
    fun removeGroup(groupId: Long): Boolean

    // ===== Group Members =====
    fun addGroupMember(groupId: Long, member: GroupMemberInfo)
    fun getGroupMember(groupId: Long, userId: Long): GroupMemberInfo?
    fun getGroupMembers(groupId: Long): List<GroupMemberInfo>
    fun removeGroupMember(groupId: Long, userId: Long): Boolean

    // ===== Messages =====
    fun saveMessage(event: MessageEvent): MessageInfo
    fun getMessage(messageId: Int): MessageInfo?
    fun getGroupMessages(groupId: Long): List<MessageInfo>
    fun getPrivateMessages(userId: Long): List<MessageInfo>

    // ===== Message ID Counter =====
    fun nextMessageId(): Int

    // ===== Group Honor =====
    fun setGroupHonor(groupId: Long, honor: GroupHonorInfo)

    // ===== Reset =====
    fun clear()
}
