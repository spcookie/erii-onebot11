package uesugi.onebot.lib.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageSegmentTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `text segment - type and data`() {
        val seg = textSegment("hello")
        assertEquals("text", seg.type)
        assertEquals("hello", seg.text)
    }

    @Test
    fun `at segment with Long`() {
        val seg = atSegment(123456L)
        assertEquals("at", seg.type)
        assertEquals("123456", seg.atQq)
    }

    @Test
    fun `at segment with String`() {
        val seg = atSegment("all")
        assertEquals("at", seg.type)
        assertEquals("all", seg.atQq)
    }

    @Test
    fun `image segment`() {
        val seg = imageSegment("file:///test.jpg")
        assertEquals("image", seg.type)
        assertEquals("file:///test.jpg", seg.imageFile)
    }

    @Test
    fun `face segment`() {
        val seg = faceSegment(12)
        assertEquals("face", seg.type)
        assertEquals("12", seg.faceId)
    }

    @Test
    fun `reply segment`() {
        val seg = replySegment(999L)
        assertEquals("reply", seg.type)
        assertEquals("999", seg.replyId)
    }

    @Test
    fun `record segment`() {
        val seg = recordSegment("audio.mp3")
        assertEquals("record", seg.type)
        assertEquals("audio.mp3", seg.recordFile)
    }

    @Test
    fun `video segment`() {
        val seg = videoSegment("movie.mp4")
        assertEquals("video", seg.type)
        assertEquals("movie.mp4", seg.videoFile)
    }

    @Test
    fun `poke segment`() {
        val seg = pokeSegment("1", "100")
        assertEquals("poke", seg.type)
        assertEquals("1", seg.pokeType)
        assertEquals("100", seg.data["id"])
    }

    @Test
    fun `shake segment has no data`() {
        val seg = shakeSegment()
        assertEquals("shake", seg.type)
        assertEquals(emptyMap(), seg.data)
    }

    @Test
    fun `anonymous segment has no data`() {
        val seg = anonymousSegment()
        assertEquals("anonymous", seg.type)
        assertEquals(emptyMap(), seg.data)
    }

    @Test
    fun `rps segment`() {
        val seg = rpsSegment()
        assertEquals("rps", seg.type)
    }

    @Test
    fun `dice segment`() {
        val seg = diceSegment()
        assertEquals("dice", seg.type)
    }

    @Test
    fun `share segment with all fields`() {
        val seg = shareSegment("https://example.com", "Title", "content", "img.jpg")
        assertEquals("share", seg.type)
        assertEquals("https://example.com", seg.data["url"])
        assertEquals("Title", seg.data["title"])
    }

    @Test
    fun `contact segment`() {
        val seg = contactSegment("qq", 123456L)
        assertEquals("contact", seg.type)
        assertEquals("qq", seg.data["type"])
        assertEquals("123456", seg.data["id"])
    }

    @Test
    fun `location segment`() {
        val seg = locationSegment(39.9, 116.4, "北京", "中国")
        assertEquals("location", seg.type)
        assertEquals("39.9", seg.data["lat"])
        assertEquals("116.4", seg.data["lon"])
    }

    @Test
    fun `music segment`() {
        val seg = musicSegment("custom", url = "http://music.com/a.mp3", title = "Song")
        assertEquals("music", seg.type)
        assertEquals("custom", seg.data["type"])
        assertEquals("Song", seg.data["title"])
    }

    @Test
    fun `xml segment`() {
        val seg = xmlSegment("<msg>hello</msg>")
        assertEquals("xml", seg.type)
        assertEquals("<msg>hello</msg>", seg.data["data"])
    }

    @Test
    fun `json segment`() {
        val seg = jsonSegment("{\"key\":\"val\"}")
        assertEquals("json", seg.type)
        assertEquals("{\"key\":\"val\"}", seg.data["data"])
    }

    @Test
    fun `String toMessage extension`() {
        val msg = "hello world".toMessage()
        assertEquals(1, msg.size)
        assertEquals("text", msg[0].type)
        assertEquals("hello world", msg[0].text)
    }

    @Test
    fun `extension accessor returns null for wrong type`() {
        val seg = textSegment("hello")
        assertNull(seg.atQq)
        assertNull(seg.imageFile)
    }
}
