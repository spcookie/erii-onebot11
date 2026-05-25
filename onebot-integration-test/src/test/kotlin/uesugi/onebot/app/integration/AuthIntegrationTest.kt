package uesugi.onebot.app.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.mock.MockBot
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.api.getLoginInfo
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class AuthIntegrationTest {

    @Test
    fun testAccessTokenAccepted() {
        val token = "test-token-123"
        val config = OneBotConfig(
            httpEnable = true, httpHost = "127.0.0.1", httpPort = 5760,
            accessToken = token, selfId = 10001
        )

        val mockBot = MockBot(config)

        runBlocking {
            mockBot.start()
            delay(500)

            val client = OneBotClient(
                OneBotConfig(
                    httpEnable = true, httpHost = "127.0.0.1", httpPort = 5760,
                    accessToken = token, selfId = 10001
                )
            )
            client.start()
            delay(500)

            try {
                val info = client.getLoginInfo()
                assertEquals(10001, info.userId)
            } finally {
                client.stop()
                mockBot.stop()
            }
        }
    }

    @Test
    fun testAccessTokenRejected() {
        val serverConfig = OneBotConfig(
            httpEnable = true, httpHost = "127.0.0.1", httpPort = 5760,
            accessToken = "correct-token", selfId = 10001
        )
        val mockBot = MockBot(serverConfig)

        runBlocking {
            mockBot.start()
            delay(500)

            val client = OneBotClient(
                OneBotConfig(
                    httpEnable = true, httpHost = "127.0.0.1", httpPort = 5760,
                    accessToken = "wrong-token", selfId = 10001
                )
            )
            client.start()
            delay(500)

            try {
                client.getLoginInfo()
                fail("Should have thrown due to 401 Unauthorized")
            } catch (_: Exception) {
                assertTrue(true)
            } finally {
                client.stop()
                mockBot.stop()
            }
        }
    }

    @Test
    fun testAccessTokenMissingWhenRequired() {
        val serverConfig = OneBotConfig(
            httpEnable = true, httpHost = "127.0.0.1", httpPort = 5760,
            accessToken = "required-token", selfId = 10001
        )
        val mockBot = MockBot(serverConfig)

        runBlocking {
            mockBot.start()
            delay(500)

            val client = OneBotClient(
                OneBotConfig(
                    httpEnable = true, httpHost = "127.0.0.1", httpPort = 5760,
                    accessToken = null, selfId = 10001
                )
            )
            client.start()
            delay(500)

            try {
                client.getLoginInfo()
                fail("Should have thrown due to missing token")
            } catch (_: Exception) {
                assertTrue(true)
            } finally {
                client.stop()
                mockBot.stop()
            }
        }
    }
}
