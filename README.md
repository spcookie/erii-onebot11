# erii-onebot11

基于 [OneBot 11 协议](https://github.com/botuniverse/onebot-11) 的 Kotlin / Go 双语言实现。

## 项目结构

```
erii-onebot11/
├── kotlin/
│   ├── lib-onebot/        # 核心协议库（模型、编解码、传输、连接）
│   ├── onebot-sdk/        # 上层 SDK（Bot 主类、中间件、事件分发、消息 DSL）
│   ├── onebot-app/        # 示例机器人应用
│   └── mock-bot-server/   # Mock QQ 后端，用于测试
├── go/
│   ├── onebot-sdk/        # Go SDK（net/http + gorilla/websocket）
│   └── onebot-app/        # Go 示例机器人
└── onebot-11/             # 协议文档
```

## 快速开始

### Kotlin

```bash
# 运行全部测试
./gradlew :erii-onebot11:kotlin:lib-onebot:test
./gradlew :erii-onebot11:kotlin:onebot-sdk:test
./gradlew :erii-onebot11:kotlin:mock-bot-server:test
```

### Go

```bash
cd go/onebot-sdk
go test ./... -v
```

## 模块说明

| 模块 | 语言 | 说明 |
|------|------|------|
| **lib-onebot** | Kotlin | 协议核心：37 个 API 动作、4 类 20 种事件、CQ 码编解码、HTTP/WS 传输层、Echo 匹配 |
| **onebot-sdk** | Kotlin / Go | Bot 开发框架：便捷 API 封装、中间件管道、类型化事件分发、消息 DSL |
| **onebot-app** | Kotlin / Go | 示例机器人，展示 SDK 用法 |
| **mock-bot-server** | Kotlin | 模拟 QQ 后端，提供全部 API 的 mock 实现和事件生成 |

## 设计

- **传输抽象**：`ActionChannel` + `EventChannel` 统一四种通信模式（HTTP 服务端、HTTP POST、正向 WS、反向 WS）
- **消息段**：扁平 `Map<String, String>` + 类型安全访问器，兼容协议扩展
- **事件多态**：两级判别（post_type → 子类型字段）自动分派到 20 种具体事件类型
- **Echo 匹配**：Kotlin 端用 `CompletableDeferred`，Go 端用 channel
- **中间件**：链式职责，支持动作拦截和事件拦截

## 依赖

| 技术 | Kotlin | Go |
|------|--------|-----|
| HTTP | Ktor 3 (Netty) | net/http |
| WebSocket | Ktor WebSockets | gorilla/websocket |
| JSON | kotlinx.serialization | encoding/json |
| 日志 | SLF4J + Logback | log/slog |
