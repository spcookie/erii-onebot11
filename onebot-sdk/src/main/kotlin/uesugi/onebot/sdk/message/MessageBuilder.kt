package uesugi.onebot.sdk.message

import uesugi.onebot.core.model.*

/**
 * 消息 DSL 构建器。
 *
 * 用法：
 * ```kotlin
 * val msg = buildMessage {
 *     text("你好，")
 *     at(123456789L)
 *     text(" 看看这张图：")
 *     image("https://example.com/img.jpg")
 * }
 * ```
 */
class MessageBuilder {
    private val segments = mutableListOf<MessageSegment>()

    fun text(text: String) {
        segments.add(textSegment(text))
    }

    fun at(qq: Long) {
        segments.add(atSegment(qq))
    }

    fun atAll() {
        segments.add(MessageSegment("at", mapOf("qq" to "all")))
    }

    fun image(
        file: String,
        type: String? = null,
        url: String? = null,
        cache: Boolean? = null,
        proxy: Boolean? = null,
        timeout: Int? = null
    ) {
        segments.add(imageSegment(file, type, url, cache, proxy, timeout))
    }

    fun face(id: String) {
        segments.add(faceSegment(id))
    }

    fun reply(id: Long) {
        segments.add(replySegment(id))
    }

    fun record(
        file: String,
        magic: Boolean? = null,
        url: String? = null,
        cache: Boolean? = null,
        proxy: Boolean? = null,
        timeout: Int? = null
    ) {
        segments.add(recordSegment(file, magic, url, cache, proxy, timeout))
    }

    fun video(
        file: String,
        url: String? = null,
        cache: Boolean? = null,
        proxy: Boolean? = null,
        timeout: Int? = null
    ) {
        segments.add(videoSegment(file, url, cache, proxy, timeout))
    }

    fun share(url: String, title: String, content: String = "", image: String = "") {
        segments.add(shareSegment(url, title, content, image))
    }

    fun contact(type: String, id: Long) {
        segments.add(contactSegment(type, id))
    }

    fun location(lat: Double, lon: Double, title: String = "", content: String = "") {
        segments.add(locationSegment(lat, lon, title, content))
    }

    fun music(
        type: String,
        id: String = "",
        url: String = "",
        audio: String = "",
        title: String = "",
        content: String = "",
        image: String = ""
    ) {
        segments.add(musicSegment(type, id, url, audio, title, content, image))
    }

    fun rps() {
        segments.add(rpsSegment())
    }

    fun dice() {
        segments.add(diceSegment())
    }

    fun shake() {
        segments.add(shakeSegment())
    }

    fun poke(type: String = "", id: String = "") {
        segments.add(pokeSegment(type, id))
    }

    fun anonymous(ignore: Boolean? = null) {
        segments.add(anonymousSegment(ignore))
    }

    fun forward(id: String) {
        segments.add(forwardSegment(id))
    }

    fun node(id: String) {
        segments.add(nodeSegment(id))
    }

    fun xml(data: String) {
        segments.add(xmlSegment(data))
    }

    fun json(data: String) {
        segments.add(jsonSegment(data))
    }

    internal fun build(): MessageContent = segments.toList()
}

/** 构建消息内容 */
fun buildMessage(block: MessageBuilder.() -> Unit): MessageContent {
    return MessageBuilder().apply(block).build()
}
