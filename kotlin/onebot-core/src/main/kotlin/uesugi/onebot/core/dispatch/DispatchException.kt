package uesugi.onebot.core.dispatch

/**
 * Action 分发异常。
 *
 * 由 [ActionDispatcher] 在无法处理 action 时抛出，transport 层捕获后映射为对应的 retcode：
 * - [ActionNotFoundException] → RETCODE_NOT_FOUND (1404) / HTTP 404
 * - [MiddlewareException] → 自定义 retcode
 * - 其他未捕获异常 → RETCODE_BAD_REQUEST (1400) / HTTP 400
 */
sealed class DispatchException(val retcode: Int, message: String) : Exception(message)

class ActionNotFoundException(action: String) : DispatchException(1404, "No handler registered for action: $action")

/** 中间件短路异常，transport 层映射为 [ActionResponse.failed] */
class MiddlewareException(retcode: Int, message: String) : DispatchException(retcode, message)
