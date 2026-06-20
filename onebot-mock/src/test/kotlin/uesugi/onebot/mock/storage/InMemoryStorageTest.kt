package uesugi.onebot.mock.storage

import org.junit.Test
import uesugi.onebot.core.model.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryStorageTest {
    private val storage = InMemoryStorage(selfId = 10001)

    @Test
    fun `addUser and getUser`() {
        val sender = Sender(10086, "Alice")
        storage.addUser(sender)
        assertEquals(sender, storage.getUser(10086))
    }

    @Test
    fun `getUser returns null for unknown user`() {
        assertNull(storage.getUser(99999))
    }

    @Test
    fun `getAllUsers returns all added users`() {
        storage.addUser(Sender(10086, "Alice"))
        storage.addUser(Sender(10087, "Bob"))
        assertEquals(2, storage.getAllUsers().size)
    }

    @Test
    fun `addFriend and getFriend`() {
        val friend = FriendInfo(10086, "Alice", "A")
        storage.addFriend(friend)
        assertEquals(friend, storage.getFriend(10086))
    }

    @Test
    fun `getAllFriends returns all friends`() {
        storage.addFriend(FriendInfo(10086, "Alice"))
        storage.addFriend(FriendInfo(10087, "Bob"))
        assertEquals(2, storage.getAllFriends().size)
    }

    @Test
    fun `addGroup and getGroup`() {
        val group = GroupInfo(12345, "Test", memberCount = 0)
        storage.addGroup(group)
        assertEquals(group, storage.getGroup(12345))
    }

    @Test
    fun `getAllGroups returns all groups`() {
        storage.addGroup(GroupInfo(1, "G1"))
        storage.addGroup(GroupInfo(2, "G2"))
        assertEquals(2, storage.getAllGroups().size)
    }

    @Test
    fun `removeGroup removes group and members`() {
        storage.addGroup(GroupInfo(12345, "Test"))
        storage.addGroupMember(12345, GroupMemberInfo(groupId = 12345, userId = 10086))
        storage.removeGroup(12345)
        assertNull(storage.getGroup(12345))
        assertTrue(storage.getGroupMembers(12345).isEmpty())
    }

    @Test
    fun `addGroupMember and getGroupMember`() {
        storage.addGroup(GroupInfo(12345, "Test"))
        val member = GroupMemberInfo(groupId = 12345, userId = 10086, nickname = "Alice", role = "member")
        storage.addGroupMember(12345, member)
        assertEquals(member, storage.getGroupMember(12345, 10086))
    }

    @Test
    fun `addGroupMember increments memberCount`() {
        storage.addGroup(GroupInfo(12345, "Test", memberCount = 0))
        storage.addGroupMember(12345, GroupMemberInfo(groupId = 12345, userId = 10086))
        assertEquals(1, storage.getGroup(12345)?.memberCount)
    }

    @Test
    fun `getGroupMembers returns correct members`() {
        storage.addGroup(GroupInfo(12345, "Test"))
        storage.addGroupMember(12345, GroupMemberInfo(groupId = 12345, userId = 10086))
        storage.addGroupMember(12345, GroupMemberInfo(groupId = 12345, userId = 10087))
        assertEquals(2, storage.getGroupMembers(12345).size)
    }

    @Test
    fun `removeGroupMember decrements memberCount`() {
        storage.addGroup(GroupInfo(12345, "Test", memberCount = 1))
        storage.addGroupMember(12345, GroupMemberInfo(groupId = 12345, userId = 10086))
        storage.removeGroupMember(12345, 10086)
        assertEquals(1, storage.getGroup(12345)?.memberCount)
    }

    @Test
    fun `saveMessage from PrivateMessageEvent`() {
        val event = PrivateMessageEvent(
            time = 1000, selfId = 10001, userId = 10086,
            message = listOf(textSegment("hi")), rawMessage = "hi",
            sender = Sender(10086, "Alice")
        )
        val info = storage.saveMessage(event)
        assertEquals("private", info.messageType)
        assertEquals(1, storage.getPrivateMessages(10086).size)
    }

    @Test
    fun `saveMessage from GroupMessageEvent`() {
        val event = GroupMessageEvent(
            time = 1000, selfId = 10001, groupId = 12345, userId = 10086,
            message = listOf(textSegment("hi")), rawMessage = "hi",
            sender = GroupSender(10086, "Alice")
        )
        val info = storage.saveMessage(event)
        assertEquals("group", info.messageType)
        assertEquals(1, storage.getGroupMessages(12345).size)
    }

    @Test
    fun `nextMessageId returns incrementing values`() {
        assertEquals(1, storage.nextMessageId())
        assertEquals(2, storage.nextMessageId())
        assertEquals(3, storage.nextMessageId())
    }

    @Test
    fun `clear resets all data`() {
        storage.addUser(Sender(10086, "Alice"))
        storage.addGroup(GroupInfo(12345, "Test"))
        storage.clear()
        assertTrue(storage.getAllUsers().isEmpty())
        assertTrue(storage.getAllGroups().isEmpty())
    }
}
