package uesugi.onebot.mock.generators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockDataTest {

    @Test
    fun `botUserId is 10001`() {
        val data = MockData()
        assertEquals(10001L, data.botUserId)
    }

    @Test
    fun `friends list has 3 entries`() {
        val data = MockData()
        assertEquals(3, data.friends.size)
    }

    @Test
    fun `groups list has 3 entries`() {
        val data = MockData()
        assertEquals(3, data.groups.size)
    }

    @Test
    fun `groupMembers has entries for all 3 groups`() {
        val data = MockData()
        assertTrue(data.groupMembers.containsKey(2001L))
        assertTrue(data.groupMembers.containsKey(2002L))
        assertTrue(data.groupMembers.containsKey(2003L))
    }

    @Test
    fun `nextMsgId increments`() {
        val data = MockData()
        val first = data.nextMsgId()
        val second = data.nextMsgId()
        val third = data.nextMsgId()
        assertEquals(first + 1, second)
        assertEquals(second + 1, third)
    }
}
