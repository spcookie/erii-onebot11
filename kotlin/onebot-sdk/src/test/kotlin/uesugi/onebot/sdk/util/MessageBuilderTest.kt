package uesugi.onebot.sdk.util

import uesugi.onebot.lib.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageBuilderTest {

    // ============ individual segment types ============

    @Test
    fun `text segment`() {
        val msg = buildMessage { text("hello") }
        assertEquals(1, msg.size)
        assertEquals("text", msg[0].type)
        assertEquals("hello", msg[0].text)
    }

    @Test
    fun `at segment with Long`() {
        val msg = buildMessage { at(123456L) }
        assertEquals(1, msg.size)
        assertEquals("at", msg[0].type)
        assertEquals("123456", msg[0].atQq)
    }

    @Test
    fun `atAll segment`() {
        val msg = buildMessage { atAll() }
        assertEquals(1, msg.size)
        assertEquals("at", msg[0].type)
        assertEquals("all", msg[0].atQq)
    }

    @Test
    fun `image segment`() {
        val msg = buildMessage { image("photo.jpg") }
        assertEquals(1, msg.size)
        assertEquals("image", msg[0].type)
        assertEquals("photo.jpg", msg[0].imageFile)
    }

    @Test
    fun `face segment`() {
        val msg = buildMessage { face(12) }
        assertEquals(1, msg.size)
        assertEquals("face", msg[0].type)
        assertEquals("12", msg[0].faceId)
    }

    @Test
    fun `reply segment`() {
        val msg = buildMessage { reply(999L) }
        assertEquals(1, msg.size)
        assertEquals("reply", msg[0].type)
        assertEquals("999", msg[0].replyId)
    }

    @Test
    fun `record segment`() {
        val msg = buildMessage { record("audio.mp3") }
        assertEquals(1, msg.size)
        assertEquals("record", msg[0].type)
        assertEquals("audio.mp3", msg[0].recordFile)
    }

    @Test
    fun `video segment`() {
        val msg = buildMessage { video("movie.mp4") }
        assertEquals(1, msg.size)
        assertEquals("video", msg[0].type)
        assertEquals("movie.mp4", msg[0].videoFile)
    }

    @Test
    fun `shake segment`() {
        val msg = buildMessage { shake() }
        assertEquals(1, msg.size)
        assertEquals("shake", msg[0].type)
    }

    @Test
    fun `poke segment`() {
        val msg = buildMessage { poke("1", "100") }
        assertEquals(1, msg.size)
        assertEquals("poke", msg[0].type)
        assertEquals("1", msg[0].data["type"])
        assertEquals("100", msg[0].data["id"])
    }

    @Test
    fun `share segment`() {
        val msg = buildMessage {
            share("https://example.com", "Title", "content", "img.jpg")
        }
        assertEquals(1, msg.size)
        assertEquals("share", msg[0].type)
        assertEquals("https://example.com", msg[0].data["url"])
        assertEquals("Title", msg[0].data["title"])
    }

    @Test
    fun `dice segment`() {
        val msg = buildMessage { dice() }
        assertEquals(1, msg.size)
        assertEquals("dice", msg[0].type)
    }

    @Test
    fun `rps segment`() {
        val msg = buildMessage { rps() }
        assertEquals(1, msg.size)
        assertEquals("rps", msg[0].type)
    }

    @Test
    fun `location segment`() {
        val msg = buildMessage {
            location(39.9, 116.4, "Beijing", "China")
        }
        assertEquals(1, msg.size)
        assertEquals("location", msg[0].type)
        assertEquals("39.9", msg[0].data["lat"])
        assertEquals("116.4", msg[0].data["lon"])
    }

    @Test
    fun `music segment`() {
        val msg = buildMessage {
            music("custom", url = "http://example.com/a.mp3", title = "Song")
        }
        assertEquals(1, msg.size)
        assertEquals("music", msg[0].type)
        assertEquals("custom", msg[0].data["type"])
    }

    @Test
    fun `xml segment`() {
        val msg = buildMessage { xml("<msg>hello</msg>") }
        assertEquals(1, msg.size)
        assertEquals("xml", msg[0].type)
        assertEquals("<msg>hello</msg>", msg[0].data["data"])
    }

    @Test
    fun `json segment`() {
        val msg = buildMessage { json("{\"key\":\"val\"}") }
        assertEquals(1, msg.size)
        assertEquals("json", msg[0].type)
        assertEquals("{\"key\":\"val\"}", msg[0].data["data"])
    }

    @Test
    fun `generic segment`() {
        val msg = buildMessage {
            segment("custom_type", mapOf("key" to "value"))
        }
        assertEquals(1, msg.size)
        assertEquals("custom_type", msg[0].type)
        assertEquals("value", msg[0].data["key"])
    }

    // ============ chaining and composition ============

    @Test
    fun `multiple segments chained`() {
        val msg = buildMessage {
            text("Hello, ")
            at(123456L)
            text(" check this: ")
            image("photo.jpg")
        }
        assertEquals(4, msg.size)
        assertEquals("text", msg[0].type)
        assertEquals("Hello, ", msg[0].text)
        assertEquals("at", msg[1].type)
        assertEquals("123456", msg[1].atQq)
        assertEquals("text", msg[2].type)
        assertEquals(" check this: ", msg[2].text)
        assertEquals("image", msg[3].type)
        assertEquals("photo.jpg", msg[3].imageFile)
    }

    @Test
    fun `empty builder returns empty list`() {
        val msg = buildMessage { }
        assertEquals(0, msg.size)
        assertTrue(msg.isEmpty())
    }

    @Test
    fun `all segment types in one message`() {
        val msg = buildMessage {
            text("text")
            at(1L)
            atAll()
            image("img.jpg")
            face(1)
            reply(1L)
            record("audio.mp3")
            video("video.mp4")
            shake()
            poke("1", "2")
            share("url", "title")
            dice()
            rps()
            location(0.0, 0.0)
            music("custom")
            xml("<xml/>")
            json("{}")
            segment("custom", mapOf("k" to "v"))
        }
        assertEquals(18, msg.size)
    }
}
