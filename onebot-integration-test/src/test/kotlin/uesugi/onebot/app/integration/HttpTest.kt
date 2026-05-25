package uesugi.onebot.app.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.mock.MockBot
import uesugi.onebot.sdk.client.OneBotClient
import uesugi.onebot.sdk.client.api.getLoginInfo
import kotlin.test.assertEquals

class HttpTest {
    @Test
    fun testHttpLoginInfo() {
        runBlocking {
            val mockBot = MockBot(
                OneBotConfig(
                    httpEnable = true, httpHost = "127.0.0.1", httpPort = 5720,
                    selfId = 10001
                )
            )
            mockBot.start()
            delay(500)

            val client = OneBotClient(
                OneBotConfig(
                    httpEnable = true, httpHost = "127.0.0.1",
                    httpPort = 5720,
                    selfId = 10001
                )
            )
            client.start()
            delay(500)

            val info = client.getLoginInfo()
            assertEquals(10001, info.userId)

            client.stop()
            mockBot.stop()
        }
    }
}
