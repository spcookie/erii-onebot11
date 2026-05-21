package uesugi.onebot.lib.codec

import uesugi.onebot.lib.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CqCodeSerializerTest {

    @Test
    fun `serialize text segment`() {
        val result = CqCodeSerializer.serialize(listOf(textSegment("hello")))
        assertEquals("hello", result)
    }

    @Test
    fun `serialize text with special chars`() {
        val result = CqCodeSerializer.serialize(listOf(textSegment("a&b[c]")))
        assertEquals("a&amp;b&#91;c&#93;", result)
    }

    @Test
    fun `serialize image segment`() {
        val result = CqCodeSerializer.serialize(listOf(imageSegment("photo.jpg")))
        assertEquals("[CQ:image,file=photo.jpg]", result)
    }

    @Test
    fun `serialize at segment`() {
        val result = CqCodeSerializer.serialize(listOf(atSegment(123456L)))
        assertEquals("[CQ:at,qq=123456]", result)
    }

    @Test
    fun `serialize at all`() {
        val result = CqCodeSerializer.serialize(listOf(atSegment("all")))
        assertEquals("[CQ:at,qq=all]", result)
    }

    @Test
    fun `serialize face segment`() {
        val result = CqCodeSerializer.serialize(listOf(faceSegment(178)))
        assertEquals("[CQ:face,id=178]", result)
    }

    @Test
    fun `serialize shake segment`() {
        val result = CqCodeSerializer.serialize(listOf(shakeSegment()))
        assertEquals("[CQ:shake]", result)
    }

    @Test
    fun `serialize mixed segments`() {
        val segments = listOf(
            textSegment("你好"),
            atSegment(123456L),
            textSegment(" "),
            imageSegment("photo.jpg"),
        )
        val result = CqCodeSerializer.serialize(segments)
        assertEquals("你好[CQ:at,qq=123456] [CQ:image,file=photo.jpg]", result)
    }

    @Test
    fun `serialize param with comma`() {
        val segment = MessageSegment("share", mapOf("title" to "a,b", "url" to "http://x"))
        val result = CqCodeSerializer.serialize(listOf(segment))
        assertEquals("[CQ:share,title=a&#44;b,url=http://x]", result)
    }

    @Test
    fun `serialize param with brackets`() {
        val segment = MessageSegment("json", mapOf("data" to "[test]"))
        val result = CqCodeSerializer.serialize(listOf(segment))
        assertEquals("[CQ:json,data=&#91;test&#93;]", result)
    }

    @Test
    fun `serialize empty list`() {
        val result = CqCodeSerializer.serialize(emptyList())
        assertEquals("", result)
    }

    @Test
    fun `serialize reply segment`() {
        val result = CqCodeSerializer.serialize(listOf(replySegment(-12345L)))
        assertEquals("[CQ:reply,id=-12345]", result)
    }

    @Test
    fun `serialize record segment`() {
        val result = CqCodeSerializer.serialize(listOf(recordSegment("audio.mp3")))
        assertEquals("[CQ:record,file=audio.mp3]", result)
    }

    @Test
    fun `round-trip segments to string and back`() {
        val segments = listOf(
            textSegment("Hi & hello"),
            imageSegment("img.jpg"),
            atSegment(999L),
        )
        val serialized = CqCodeSerializer.serialize(segments)
        val parsed = CqCodeParser.parse(serialized)

        assertEquals(segments.size, parsed.size)
        for (i in segments.indices) {
            assertEquals(segments[i].type, parsed[i].type)
            assertEquals(segments[i].data, parsed[i].data)
        }
    }
}
