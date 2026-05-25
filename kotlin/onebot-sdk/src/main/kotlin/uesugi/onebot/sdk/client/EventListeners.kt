package uesugi.onebot.sdk.client

import uesugi.onebot.core.model.*

// 基础类型安全监听器

/** 监听私聊消息事件 */
fun OneBotClient.onPrivateMessage(handler: suspend (PrivateMessageEvent) -> Unit) {
    onEvent("message") { event ->
        if (event is PrivateMessageEvent) handler(event)
    }
}

/** 监听群聊消息事件 */
fun OneBotClient.onGroupMessage(handler: suspend (GroupMessageEvent) -> Unit) {
    onEvent("message") { event ->
        if (event is GroupMessageEvent) handler(event)
    }
}

/** 监听通知事件 */
fun OneBotClient.onNotice(handler: suspend (NoticeEvent) -> Unit) {
    onEvent("notice") { event ->
        if (event is NoticeEvent) handler(event)
    }
}

/** 监听请求事件 */
fun OneBotClient.onRequest(handler: suspend (RequestEvent) -> Unit) {
    onEvent("request") { event ->
        if (event is RequestEvent) handler(event)
    }
}

/** 监听元事件 */
fun OneBotClient.onMetaEvent(handler: suspend (MetaEvent) -> Unit) {
    onEvent("meta_event") { event ->
        if (event is MetaEvent) handler(event)
    }
}

// 过滤式监听器 — 带 filter 参数

/** 监听私聊消息事件（仅当 [filter] 返回 true 时触发） */
fun OneBotClient.onPrivateMessage(
    filter: (PrivateMessageEvent) -> Boolean,
    handler: suspend (PrivateMessageEvent) -> Unit
) {
    onEvent("message") { event ->
        if (event is PrivateMessageEvent && filter(event)) handler(event)
    }
}

/** 监听群聊消息事件（仅当 [filter] 返回 true 时触发） */
fun OneBotClient.onGroupMessage(
    filter: (GroupMessageEvent) -> Boolean,
    handler: suspend (GroupMessageEvent) -> Unit
) {
    onEvent("message") { event ->
        if (event is GroupMessageEvent && filter(event)) handler(event)
    }
}

/** 监听通知事件（仅当 [filter] 返回 true 时触发） */
fun OneBotClient.onNotice(
    filter: (NoticeEvent) -> Boolean,
    handler: suspend (NoticeEvent) -> Unit
) {
    onEvent("notice") { event ->
        if (event is NoticeEvent && filter(event)) handler(event)
    }
}

/** 监听请求事件（仅当 [filter] 返回 true 时触发） */
fun OneBotClient.onRequest(
    filter: (RequestEvent) -> Boolean,
    handler: suspend (RequestEvent) -> Unit
) {
    onEvent("request") { event ->
        if (event is RequestEvent && filter(event)) handler(event)
    }
}

/** 监听元事件（仅当 [filter] 返回 true 时触发） */
fun OneBotClient.onMetaEvent(
    filter: (MetaEvent) -> Boolean,
    handler: suspend (MetaEvent) -> Unit
) {
    onEvent("meta_event") { event ->
        if (event is MetaEvent && filter(event)) handler(event)
    }
}

// DSL 监听器 — where + handle 构建器

// -- 群聊消息 DSL --

class GroupMessageListenerBuilder {
    internal val filters = mutableListOf<(GroupMessageEvent) -> Boolean>()
    internal var handler: (suspend (GroupMessageEvent) -> Unit)? = null

    fun where(predicate: (GroupMessageEvent) -> Boolean) {
        filters.add(predicate)
    }

    fun handle(action: suspend (GroupMessageEvent) -> Unit) {
        handler = action
    }
}

/** DSL 方式监听群聊消息，支持多个 [where] 过滤条件 */
fun OneBotClient.onGroupMessage(dsl: GroupMessageListenerBuilder.() -> Unit) {
    val b = GroupMessageListenerBuilder().apply(dsl)
    val filters = b.filters.toList()
    val handler = b.handler ?: return
    onEvent("message") { event ->
        if (event is GroupMessageEvent && filters.all { it(event) }) {
            handler(event)
        }
    }
}

// -- 私聊消息 DSL --

class PrivateMessageListenerBuilder {
    internal val filters = mutableListOf<(PrivateMessageEvent) -> Boolean>()
    internal var handler: (suspend (PrivateMessageEvent) -> Unit)? = null

    fun where(predicate: (PrivateMessageEvent) -> Boolean) {
        filters.add(predicate)
    }

    fun handle(action: suspend (PrivateMessageEvent) -> Unit) {
        handler = action
    }
}

/** DSL 方式监听私聊消息，支持多个 [where] 过滤条件 */
fun OneBotClient.onPrivateMessage(dsl: PrivateMessageListenerBuilder.() -> Unit) {
    val b = PrivateMessageListenerBuilder().apply(dsl)
    val filters = b.filters.toList()
    val handler = b.handler ?: return
    onEvent("message") { event ->
        if (event is PrivateMessageEvent && filters.all { it(event) }) {
            handler(event)
        }
    }
}

// -- 通知事件 DSL --

class NoticeListenerBuilder {
    internal val filters = mutableListOf<(NoticeEvent) -> Boolean>()
    internal var handler: (suspend (NoticeEvent) -> Unit)? = null

    fun where(predicate: (NoticeEvent) -> Boolean) {
        filters.add(predicate)
    }

    fun handle(action: suspend (NoticeEvent) -> Unit) {
        handler = action
    }
}

/** DSL 方式监听通知事件，支持多个 [where] 过滤条件 */
fun OneBotClient.onNotice(dsl: NoticeListenerBuilder.() -> Unit) {
    val b = NoticeListenerBuilder().apply(dsl)
    val filters = b.filters.toList()
    val handler = b.handler ?: return
    onEvent("notice") { event ->
        if (event is NoticeEvent && filters.all { it(event) }) {
            handler(event)
        }
    }
}

// -- 请求事件 DSL --

class RequestListenerBuilder {
    internal val filters = mutableListOf<(RequestEvent) -> Boolean>()
    internal var handler: (suspend (RequestEvent) -> Unit)? = null

    fun where(predicate: (RequestEvent) -> Boolean) {
        filters.add(predicate)
    }

    fun handle(action: suspend (RequestEvent) -> Unit) {
        handler = action
    }
}

/** DSL 方式监听请求事件，支持多个 [where] 过滤条件 */
fun OneBotClient.onRequest(dsl: RequestListenerBuilder.() -> Unit) {
    val b = RequestListenerBuilder().apply(dsl)
    val filters = b.filters.toList()
    val handler = b.handler ?: return
    onEvent("request") { event ->
        if (event is RequestEvent && filters.all { it(event) }) {
            handler(event)
        }
    }
}
