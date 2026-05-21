package uesugi.onebot.lib.codec

import uesugi.onebot.lib.model.MessageSegment
import uesugi.onebot.lib.model.textSegment
import uesugi.onebot.lib.model.text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CqCodeParserTest {

    @Test
    fun `parse plain text`() {
        val result = CqCodeParser.parse("hello world")
        assertEquals(1, result.size)
        assertEquals("text", result[0].type)
        assertEquals("hello world", result[0].text)
    }

    @Test
    fun `parse single at code`() {
        val result = CqCodeParser.parse("[CQ:at,qq=123456]")
        assertEquals(1, result.size)
        assertEquals("at", result[0].type)
        assertEquals("123456", result[0].data["qq"])
    }

    @Test
    fun `parse text mixed with CQ codes`() {
        val result = CqCodeParser.parse("你好[CQ:at,qq=123456]世界")
        assertEquals(3, result.size)
        assertEquals("text", result[0].type)
        assertEquals("你好", result[0].text)
        assertEquals("at", result[1].type)
        assertEquals("123456", result[1].data["qq"])
        assertEquals("text", result[2].type)
        assertEquals("世界", result[2].text)
    }

    @Test
    fun `parse multiple CQ codes`() {
        val result = CqCodeParser.parse("[CQ:at,qq=111][CQ:at,qq=222]")
        assertEquals(2, result.size)
        assertEquals("at", result[0].type)
        assertEquals("111", result[0].data["qq"])
        assertEquals("at", result[1].type)
        assertEquals("222", result[1].data["qq"])
    }

    @Test
    fun `parse code with no params`() {
        val result = CqCodeParser.parse("[CQ:shake]")
        assertEquals(1, result.size)
        assertEquals("shake", result[0].type)
        assertTrue(result[0].data.isEmpty())
    }

    @Test
    fun `parse code with empty params`() {
        val result = CqCodeParser.parse("[CQ:shake,]")
        assertEquals(1, result.size)
        assertEquals("shake", result[0].type)
        assertTrue(result[0].data.isEmpty())
    }

    @Test
    fun `parse face code`() {
        val result = CqCodeParser.parse("[CQ:face,id=178]")
        assertEquals(1, result.size)
        assertEquals("face", result[0].type)
        assertEquals("178", result[0].data["id"])
    }

    @Test
    fun `parse image code`() {
        val result = CqCodeParser.parse("[CQ:image,file=123.jpg]")
        assertEquals(1, result.size)
        assertEquals("image", result[0].type)
        assertEquals("123.jpg", result[0].data["file"])
    }

    @Test
    fun `parse at all`() {
        val result = CqCodeParser.parse("[CQ:at,qq=all]")
        assertEquals(1, result.size)
        assertEquals("at", result[0].type)
        assertEquals("all", result[0].data["qq"])
    }

    @Test
    fun `parse record code`() {
        val result = CqCodeParser.parse("[CQ:record,file=audio.mp3]")
        assertEquals(1, result.size)
        assertEquals("record", result[0].type)
        assertEquals("audio.mp3", result[0].data["file"])
    }

    @Test
    fun `parse reply code`() {
        val result = CqCodeParser.parse("[CQ:reply,id=-12345]")
        assertEquals(1, result.size)
        assertEquals("reply", result[0].type)
        assertEquals("-12345", result[0].data["id"])
    }

    @Test
    fun `parse escaped brackets in text`() {
        val result = CqCodeParser.parse("&#91;text&#93;")
        assertEquals(1, result.size)
        assertEquals("text", result[0].type)
        assertEquals("[text]", result[0].text)
    }

    @Test
    fun `parse escaped ampersand in text`() {
        val result = CqCodeParser.parse("a&amp;b")
        assertEquals(1, result.size)
        assertEquals("text", result[0].type)
        assertEquals("a&b", result[0].text)
    }

    @Test
    fun `parse escaped comma in params`() {
        val result = CqCodeParser.parse("[CQ:share,title=a&#44;b,url=http://x]")
        assertEquals(1, result.size)
        assertEquals("share", result[0].type)
        assertEquals("a,b", result[0].data["title"])
        assertEquals("http://x", result[0].data["url"])
    }

    @Test
    fun `parse empty input`() {
        val result = CqCodeParser.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse incomplete CQ code as text`() {
        val result = CqCodeParser.parse("[CQ:at,qq=123")
        assertEquals(1, result.size)
        assertEquals("text", result[0].type)
        assertEquals("[CQ:at,qq=123", result[0].text)
    }

    @Test
    fun `parse share code`() {
        val result = CqCodeParser.parse("[CQ:share,url=http://x,title=test,content=desc,image=img.jpg]")
        assertEquals(1, result.size)
        assertEquals("share", result[0].type)
        assertEquals("http://x", result[0].data["url"])
        assertEquals("test", result[0].data["title"])
        assertEquals("desc", result[0].data["content"])
        assertEquals("img.jpg", result[0].data["image"])
    }

    @Test
    fun `parse music code`() {
        val result = CqCodeParser.parse("[CQ:music,type=163,id=12345]")
        assertEquals(1, result.size)
        assertEquals("music", result[0].type)
        assertEquals("163", result[0].data["type"])
        assertEquals("12345", result[0].data["id"])
    }

    @Test
    fun `parse xml code`() {
        val result = CqCodeParser.parse("[CQ:xml,data=<msg>hello</msg>]")
        assertEquals(1, result.size)
        assertEquals("xml", result[0].type)
        assertEquals("<msg>hello</msg>", result[0].data["data"])
    }

    @Test
    fun `parse json code`() {
        val result = CqCodeParser.parse("""[CQ:json,data={"key":"value"}]""")
        assertEquals(1, result.size)
        assertEquals("json", result[0].type)
        assertEquals("""{"key":"value"}""", result[0].data["data"])
    }

    @Test
    fun `parse location code`() {
        val result = CqCodeParser.parse("[CQ:location,lat=39.9,lon=116.4,title=beijing]")
        assertEquals(1, result.size)
        assertEquals("location", result[0].type)
        assertEquals("39.9", result[0].data["lat"])
        assertEquals("116.4", result[0].data["lon"])
    }

    @Test
    fun `parse poke code`() {
        val result = CqCodeParser.parse("[CQ:poke,type=1,id=10000]")
        assertEquals(1, result.size)
        assertEquals("poke", result[0].type)
        assertEquals("1", result[0].data["type"])
    }

    @Test
    fun `parse contact code`() {
        val result = CqCodeParser.parse("[CQ:contact,type=qq,id=123456]")
        assertEquals(1, result.size)
        assertEquals("contact", result[0].type)
        assertEquals("qq", result[0].data["type"])
        assertEquals("123456", result[0].data["id"])
    }

    @Test
    fun `round-trip parse serialize parse`() {
        val original = "你好[CQ:face,id=178]看看[CQ:image,file=photo.jpg]"
        val segments = CqCodeParser.parse(original)
        val serialized = CqCodeSerializer.serialize(segments)
        val reparsed = CqCodeParser.parse(serialized)

        assertEquals(segments.size, reparsed.size)
        for (i in segments.indices) {
            assertEquals(segments[i].type, reparsed[i].type)
            assertEquals(segments[i].data, reparsed[i].data)
        }
    }

    @Test
    fun `round-trip with escaped chars`() {
        val original = "a&b[CQ:at,qq=123]c[d]e"
        val segments = CqCodeParser.parse(original)
        val serialized = CqCodeSerializer.serialize(segments)
        val reparsed = CqCodeParser.parse(serialized)

        assertEquals(segments.size, reparsed.size)
        for (i in segments.indices) {
            assertEquals(segments[i].type, reparsed[i].type)
            assertEquals(segments[i].data, reparsed[i].data)
        }
    }

    @Test
    fun `text without CQ code returns single text segment`() {
        val result = CqCodeParser.parse("just some normal text")
        assertEquals(1, result.size)
        assertEquals("text", result[0].type)
    }

    @Test
    fun `text starts and ends with CQ code`() {
        val result = CqCodeParser.parse("[CQ:at,qq=123]中间[CQ:face,id=1]")
        assertEquals(3, result.size)
        assertEquals("at", result[0].type)
        assertEquals("text", result[1].type)
        assertEquals("中间", result[1].text)
        assertEquals("face", result[2].type)
    }
}
