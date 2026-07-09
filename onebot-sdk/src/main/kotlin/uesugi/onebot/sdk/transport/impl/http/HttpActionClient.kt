package uesugi.onebot.sdk.transport.impl.http

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import uesugi.onebot.core.config.OneBotConfig
import uesugi.onebot.core.model.ActionResponse
import uesugi.onebot.core.model.OneBotActionParams
import uesugi.onebot.core.model.OneBotActionResult
import uesugi.onebot.core.parser.ActionParamParser
import uesugi.onebot.core.parser.ActionResultParser
import uesugi.onebot.core.transport.ActionChannel
import uesugi.onebot.core.transport.JsonFactory

/**
 * HTTP Action 客户端。
 *
 * 用于 SDK/客户端侧：通过 HTTP POST/GET 向 OneBot 实现发送 API 调用。
 * 将 ActionResponse 同步反序列化为 [ActionResponse]。
 */
class HttpActionClient(
    private val config: OneBotConfig,
    private val client: HttpClient = HttpClient()
) : ActionChannel {

    private val logger = LoggerFactory.getLogger(HttpActionClient::class.java)
    private val json: Json = JsonFactory.base
    private val baseUrl = "http://${config.httpHost}:${config.httpPort}"
    private val paramParser = ActionParamParser(config.messageFormat)
    private val resultParser = ActionResultParser()

    override suspend fun call(action: String, params: OneBotActionParams): OneBotActionResult {
        val response = client.post("$baseUrl/$action") {
            contentType(ContentType.Application.Json)
            config.authHeader?.let { header(HttpHeaders.Authorization, it) }
            header("X-Self-ID", config.selfIdStr)

            setBody(json.encodeToString(paramParser.serialize(action, params)))
        }

        val body = response.bodyAsText()
        val actionResponse: ActionResponse = json.decodeFromString(ActionResponse.serializer(), body)

        if (!response.status.isSuccess()) {
            logger.warn(
                "HTTP action '{}' failed: status={}, retcode={}",
                action,
                response.status.value,
                actionResponse.retcode
            )
        }

        return resultParser.deserialize(action, actionResponse.data)
    }

    override suspend fun start() { /* HttpClient is stateless */
    }

    override suspend fun stop() {
        client.close()
    }
}
