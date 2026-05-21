package uesugi.onebot.mock

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import uesugi.onebot.lib.connection.OneBotConfig

fun main() {
    val logger = LoggerFactory.getLogger("mock-bot-server")

    val config = OneBotConfig(
        httpEnable = true,
        httpHost = "0.0.0.0",
        httpPort = 5700,
        wsEnable = true,
        wsHost = "0.0.0.0",
        wsPort = 6700,
        appName = "mock-bot-server",
        appVersion = "1.0.0",
    )

    val server = MockServer(config)

    runBlocking {
        server.start()
        logger.info("Mock bot server running: HTTP={}:{}, WS={}:{}",
            config.httpHost, config.httpPort,
            config.wsHost, config.wsPort)

        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                server.stop()
            }
        })

        while (true) {
            delay(1000)
        }
    }
}
