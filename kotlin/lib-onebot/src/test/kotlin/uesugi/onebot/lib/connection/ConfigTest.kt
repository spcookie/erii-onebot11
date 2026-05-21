package uesugi.onebot.lib.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigTest {

    @Test
    fun `default config has HTTP disabled`() {
        val config = OneBotConfig()
        assertFalse(config.httpEnable)
        assertEquals("0.0.0.0", config.httpHost)
        assertEquals(5700, config.httpPort)
    }

    @Test
    fun `default config has WS disabled`() {
        val config = OneBotConfig()
        assertFalse(config.wsEnable)
    }

    @Test
    fun `custom HTTP config`() {
        val config = OneBotConfig(
            httpHost = "127.0.0.1",
            httpPort = 8080,
        )
        assertEquals("127.0.0.1", config.httpHost)
        assertEquals(8080, config.httpPort)
    }

    @Test
    fun `WS config`() {
        val config = OneBotConfig(
            wsEnable = true,
            wsHost = "192.168.1.1",
            wsPort = 9000,
        )
        assertTrue(config.wsEnable)
        assertEquals("192.168.1.1", config.wsHost)
        assertEquals(9000, config.wsPort)
    }

    @Test
    fun `HTTP POST config`() {
        val config = OneBotConfig(
            httpPostEnable = true,
            httpPostUrl = "http://example.com/events",
            secret = "my-secret",
        )
        assertTrue(config.httpPostEnable)
        assertEquals("http://example.com/events", config.httpPostUrl)
        assertEquals("my-secret", config.secret)
    }

    @Test
    fun `reverse WS config`() {
        val config = OneBotConfig(
            wsReverseEnable = true,
            wsReverseUrl = "ws://remote:6700/ws",
            wsReverseUseUniversal = true,
        )
        assertTrue(config.wsReverseEnable)
        assertEquals("ws://remote:6700/ws", config.wsReverseUrl)
        assertTrue(config.wsReverseUseUniversal)
    }

    @Test
    fun `access token config`() {
        val config = OneBotConfig(accessToken = "bearer-token-123")
        assertEquals("bearer-token-123", config.accessToken)
    }

    @Test
    fun `timeout config`() {
        val config = OneBotConfig(timeoutMs = 15000)
        assertEquals(15000L, config.timeoutMs)
    }

    @Test
    fun `self ID config`() {
        val config = OneBotConfig(selfId = 10086)
        assertEquals(10086L, config.selfId)
    }

    @Test
    fun `app metadata config`() {
        val config = OneBotConfig(appName = "MyBot", appVersion = "2.0.0")
        assertEquals("MyBot", config.appName)
        assertEquals("2.0.0", config.appVersion)
    }
}
