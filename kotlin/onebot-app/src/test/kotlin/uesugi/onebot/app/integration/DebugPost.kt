package uesugi.onebot.app.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.mock.MockBot
import uesugi.onebot.sdk.client.OneBotClient
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import kotlin.test.assertEquals

class DebugPostTest {
    private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

    @Test
    fun testHttpPostEvent() {
        runBlocking {
            val actionPort = findFreePort()
            val eventPort = findFreePort()

            val mockBot = MockBot(
                OneBotConfig(
                    httpEnable = true, httpHost = "127.0.0.1", httpPort = actionPort,
                    httpPostEnable = true, httpPostUrl = "http://127.0.0.1:$eventPort",
                    httpPostTimeout = 500L,
                    selfId = 10001
                )
            )
            mockBot.start()
            delay(500)

            val client = OneBotClient(
                OneBotConfig(
                    httpEnable = true, httpHost = "127.0.0.1", httpPort = actionPort,
                    httpPostEnable = true, httpPostHost = "127.0.0.1", httpPostPort = eventPort,
                    httpPostTimeout = 500L,
                    selfId = 10001
                )
            )
            client.start()
            delay(1000)

            // Directly POST to event server
            val url = URL("http://127.0.0.1:$eventPort/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val json =
                """{"time":1,"self_id":10001,"post_type":"meta_event","meta_event_type":"lifecycle","sub_type":"connect"}"""
            conn.outputStream.write(json.toByteArray())
            conn.outputStream.close()
            val responseCode = conn.responseCode
            println("DEBUG: responseCode=$responseCode")
            assertEquals(204, responseCode)

            client.stop()
            mockBot.stop()
        }
    }
}
