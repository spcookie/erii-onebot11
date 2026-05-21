package uesugi.onebot.lib.codec

import kotlin.test.Test
import kotlin.test.assertEquals

class CqEscapeTest {

    // ===== escapeText =====

    @Test
    fun `escapeText - ampersand`() {
        assertEquals("&amp;", CqEscape.escapeText("&"))
    }

    @Test
    fun `escapeText - left bracket`() {
        assertEquals("&#91;", CqEscape.escapeText("["))
    }

    @Test
    fun `escapeText - right bracket`() {
        assertEquals("&#93;", CqEscape.escapeText("]"))
    }

    @Test
    fun `escapeText - plain text unchanged`() {
        assertEquals("hello world", CqEscape.escapeText("hello world"))
    }

    @Test
    fun `escapeText - mixed text`() {
        assertEquals(
            "hello &amp; world &#91;test&#93;",
            CqEscape.escapeText("hello & world [test]")
        )
    }

    @Test
    fun `escapeText - multiple ampersands`() {
        assertEquals("&amp;&amp;&amp;", CqEscape.escapeText("&&&"))
    }

    @Test
    fun `escapeText - empty string`() {
        assertEquals("", CqEscape.escapeText(""))
    }

    @Test
    fun `escapeText - nested brackets`() {
        assertEquals("&#91;&#91;nested&#93;&#93;", CqEscape.escapeText("[[nested]]"))
    }

    @Test
    fun `escapeText round-trip`() {
        val original = "hello & world [test] foo"
        val escaped = CqEscape.escapeText(original)
        val unescaped = CqEscape.unescapeText(escaped)
        assertEquals(original, unescaped)
    }

    // ===== escapeParam =====

    @Test
    fun `escapeParam - comma`() {
        assertEquals("&#44;", CqEscape.escapeParam(","))
    }

    @Test
    fun `escapeParam - mixed special chars`() {
        assertEquals(
            "a&amp;b&#91;c&#93;d&#44;e",
            CqEscape.escapeParam("a&b[c]d,e")
        )
    }

    @Test
    fun `escapeParam round-trip`() {
        val original = "file:///a,b&c[d]e"
        val escaped = CqEscape.escapeParam(original)
        val unescaped = CqEscape.unescapeParam(escaped)
        assertEquals(original, unescaped)
    }

    // ===== unescapeText =====

    @Test
    fun `unescapeText - all entities`() {
        assertEquals(
            "hello & world [test]",
            CqEscape.unescapeText("hello &amp; world &#91;test&#93;")
        )
    }

    @Test
    fun `unescapeText - no entities`() {
        assertEquals("plain text", CqEscape.unescapeText("plain text"))
    }

    // ===== unescapeParam =====

    @Test
    fun `unescapeParam - comma entity`() {
        assertEquals("a,b", CqEscape.unescapeParam("a&#44;b"))
    }

    @Test
    fun `unescapeParam - all entities`() {
        assertEquals(
            "a&b[c]d,e",
            CqEscape.unescapeParam("a&amp;b&#91;c&#93;d&#44;e")
        )
    }
}
