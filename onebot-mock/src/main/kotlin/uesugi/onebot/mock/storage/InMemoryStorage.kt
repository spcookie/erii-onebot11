package uesugi.onebot.mock.storage

import uesugi.onebot.core.model.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class InMemoryStorage(override val selfId: Long = 10001) : MockStorage {
    private val users = ConcurrentHashMap<Long, Sender>()
    private val friends = ConcurrentHashMap<Long, FriendInfo>()
    private val groups = ConcurrentHashMap<Long, GroupInfo>()
    private val groupMembers = ConcurrentHashMap<String, GroupMemberInfo>()
    private val messages = ConcurrentHashMap<Int, MessageInfo>()
    private val groupMessages = ConcurrentHashMap<Long, MutableList<Int>>()
    private val privateMessages = ConcurrentHashMap<Long, MutableList<Int>>()
    private val messageIdCounter = AtomicInteger(1)

    override fun addUser(user: Sender) {
        users[user.userId] = user
    }

    override fun getUser(userId: Long): Sender? = users[userId]
    override fun getAllUsers(): List<Sender> = users.values.toList()

    override fun addFriend(friend: FriendInfo) {
        friends[friend.userId] = friend
    }

    override fun getFriend(userId: Long): FriendInfo? = friends[userId]
    override fun getAllFriends(): List<FriendInfo> = friends.values.toList()

    override fun addGroup(group: GroupInfo) {
        groups[group.groupId] = group
    }

    override fun getGroup(groupId: Long): GroupInfo? = groups[groupId]
    override fun getAllGroups(): List<GroupInfo> = groups.values.toList()
    override fun removeGroup(groupId: Long): Boolean {
        groups.remove(groupId)
        groupMembers.keys.removeIf { it.startsWith("$groupId:") }
        return true
    }

    override fun addGroupMember(groupId: Long, member: GroupMemberInfo) {
        groupMembers["$groupId:${member.userId}"] = member
        groups[groupId]?.let { groups[groupId] = it.copy(memberCount = it.memberCount + 1) }
    }

    override fun getGroupMember(groupId: Long, userId: Long): GroupMemberInfo? =
        groupMembers["$groupId:$userId"]

    override fun getGroupMembers(groupId: Long): List<GroupMemberInfo> =
        groupMembers.filter { it.key.startsWith("$groupId:") }.values.toList()

    override fun removeGroupMember(groupId: Long, userId: Long): Boolean {
        groupMembers.remove("$groupId:$userId")
        groups[groupId]?.let { groups[groupId] = it.copy(memberCount = maxOf(0, it.memberCount - 1)) }
        return true
    }

    override fun saveMessage(event: MessageEvent): MessageInfo {
        val msgId = when (event) {
            is PrivateMessageEvent -> if (event.messageId > 0) event.messageId else messageIdCounter.getAndIncrement()
            is GroupMessageEvent -> if (event.messageId > 0) event.messageId else messageIdCounter.getAndIncrement()
        }
        val info = when (event) {
            is PrivateMessageEvent -> MessageInfo(
                time = event.time, messageType = "private", messageId = msgId,
                realId = msgId, sender = event.sender, message = event.message
            )

            is GroupMessageEvent -> MessageInfo(
                time = event.time, messageType = "group", messageId = msgId,
                realId = msgId, sender = Sender(event.userId, event.sender.nickname),
                message = event.message
            )
        }
        messages[msgId] = info
        when (event) {
            is GroupMessageEvent -> groupMessages.getOrPut(event.groupId) { mutableListOf() }.add(msgId)
            is PrivateMessageEvent -> privateMessages.getOrPut(event.userId) { mutableListOf() }.add(msgId)
        }
        return info
    }

    override fun getMessage(messageId: Int): MessageInfo? = messages[messageId]
    override fun getGroupMessages(groupId: Long): List<MessageInfo> =
        groupMessages[groupId]?.mapNotNull { messages[it] } ?: emptyList()

    override fun getPrivateMessages(userId: Long): List<MessageInfo> =
        privateMessages[userId]?.mapNotNull { messages[it] } ?: emptyList()

    override fun nextMessageId(): Int = messageIdCounter.getAndIncrement()
    override fun setGroupHonor(groupId: Long, honor: GroupHonorInfo) { /* no-op for in-memory */
    }

    override fun clear() {
        users.clear(); friends.clear(); groups.clear(); groupMembers.clear()
        messages.clear(); groupMessages.clear(); privateMessages.clear()
        messageIdCounter.set(1)
    }
}
