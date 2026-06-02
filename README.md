# erii-onebot11

基于 [OneBot 11 协议](https://github.com/botuniverse/onebot-11) 的 Kotlin 实现。

## 项目结构

```
erii-onebot11/
├── onebot-core/              # 协议核心：模型、编解码、传输层
├── onebot-lib/               # 封装库：便捷 API、事件分发、中间件
├── onebot-sdk/               # 上层 SDK：消息 DSL、客户端、类型安全访问器
├── onebot-mock/              # Mock QQ 后端，用于集成测试
├── onebot-integration-test/  # 集成测试
├── onebot-11/                # 协议文档
└── go/                       # Go SDK（net/http + gorilla/websocket）
    ├── onebot-sdk/
    └── onebot-app/
```

## 快速开始

```bash
# 编译全部模块
./gradlew build

# 运行全部测试
./gradlew test

# 运行指定模块测试
./gradlew :onebot-core:test
./gradlew :onebot-sdk:test
./gradlew :onebot-mock:test
```

### Go

```bash
cd go/onebot-sdk
go test ./... -v
```

## 模块说明

| 模块                          | 语言     | 说明                                                         |
|-----------------------------|--------|------------------------------------------------------------|
| **onebot-core**             | Kotlin | 协议核心：消息段模型、37 个 API 动作、4 类 20 种事件、CQ 码编解码、HTTP/WS 传输层      |
| **onebot-lib**              | Kotlin | 封装库：事件分发管道、中间件链、连接管理                                       |
| **onebot-sdk**              | Kotlin | 上层 SDK：消息 DSL（`MessageBuilder`）、`OneBotClient`、类型安全数据类与访问器 |
| **onebot-mock**             | Kotlin | Mock QQ 后端，提供全部 API 的 mock 实现和事件生成                         |
| **onebot-integration-test** | Kotlin | SDK 与 Mock 之间的端到端集成测试                                      |
| **go/onebot-sdk**           | Go     | Go SDK，net/http + gorilla/websocket                        |

## 设计

- **传输抽象**：`ActionChannel` + `EventChannel` 统一四种通信模式（HTTP 服务端、HTTP POST、正向 WS、反向 WS）
- **消息段**：扁平 `Map<String, JsonElement>` + 类型安全 `SegmentData` 数据类和扩展访问器，兼容协议扩展
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
