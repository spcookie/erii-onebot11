package uesugi.onebot.sdk.util

import uesugi.onebot.lib.model.*

/**
 * 消息构建器 DSL。
 *
 * 用法：
 * ```
 * val msg = buildMessage {
 *     text("你好")
 *     at(123456L)
 *     image("photo.jpg")
 * }
 * ```
 */
class MessageBuilder {
    private val segments = mutableListOf<MessageSegment>()

    fun text(content: String) {
        segments.add(textSegment(content))
    }

    fun at(qq: Long) {
        segments.add(atSegment(qq))
    }

    fun atAll() {
        segments.add(atSegment("all"))
    }

    fun image(file: String) {
        segments.add(imageSegment(file))
    }

    fun face(id: Int) {
        segments.add(faceSegment(id))
    }

    fun reply(id: Long) {
        segments.add(replySegment(id))
    }

    fun record(file: String) {
        segments.add(recordSegment(file))
    }

    fun video(file: String) {
        segments.add(videoSegment(file))
    }

    fun shake() {
        segments.add(shakeSegment())
    }

    fun poke(type: String = "1", id: String = "") {
        segments.add(pokeSegment(type, id))
    }

    fun share(url: String, title: String, content: String = "", image: String = "") {
        segments.add(shareSegment(url, title, content, image))
    }

    fun dice() {
        segments.add(diceSegment())
    }

    fun rps() {
        segments.add(rpsSegment())
    }

    fun location(lat: Double, lon: Double, title: String = "", content: String = "") {
        segments.add(locationSegment(lat, lon, title, content))
    }

    fun music(type: String, id: String = "", url: String = "", audio: String = "", title: String = "") {
        segments.add(musicSegment(type, id, url, audio, title))
    }

    fun xml(data: String) {
        segments.add(xmlSegment(data))
    }

    fun json(data: String) {
        segments.add(jsonSegment(data))
    }

    fun segment(type: String, data: Map<String, String>) {
        segments.add(MessageSegment(type, data))
    }

    fun build(): List<MessageSegment> = segments.toList()
}

fun buildMessage(block: MessageBuilder.() -> Unit): List<MessageSegment> {
    return MessageBuilder().apply(block).build()
}
