package uesugi.onebot.lib.transport

import kotlin.test.Test
import kotlin.test.assertEquals

class TransportConfigTest {

    @Test
    fun `default config values`() {
        val config = TransportConfig()
        assertEquals("127.0.0.1", config.host)
        assertEquals(5700, config.port)
        assertEquals(null, config.accessToken)
        assertEquals(null, config.secret)
        assertEquals(0L, config.selfId)
        assertEquals(15000L, config.heartbeatInterval)
        assertEquals(3000L, config.reconnectInterval)
        assertEquals(30000L, config.timeoutMs)
    }

    @Test
    fun `config with custom values`() {
        val config = TransportConfig(
            host = "192.168.1.1",
            port = 8080,
            accessToken = "token123",
            secret = "secret456",
            selfId = 10001,
            timeoutMs = 5000,
        )
        assertEquals("192.168.1.1", config.host)
        assertEquals(8080, config.port)
        assertEquals("token123", config.accessToken)
        assertEquals("secret456", config.secret)
        assertEquals(10001L, config.selfId)
        assertEquals(5000L, config.timeoutMs)
    }
}
