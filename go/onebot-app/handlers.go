package main

import (
	"context"
	"fmt"
	"log/slog"
	"strings"

	onebot "github.com/erii/onebot-sdk"
)

func registerHandlers(bot *onebot.Bot, logger *slog.Logger) {
	// Private message handlers
	bot.OnMessage(func(ctx context.Context, evt onebot.PrivateMessageEvent) {
		rawMsg := evt.RawMessage
		userID := evt.UserID

		switch {
		case rawMsg == "/ping":
			msg := onebot.MessageContent{onebot.TextSegment("pong!")}
			bot.SendPrivateMsg(ctx, userID, msg, false)

		case rawMsg == "/help":
			helpText := "可用命令:\n/ping - 测试连接\n/echo <text> - 回显消息\n/status - 查看状态\n/help - 显示帮助"
			msg := onebot.MessageContent{onebot.TextSegment(helpText)}
			bot.SendPrivateMsg(ctx, userID, msg, false)

		case strings.HasPrefix(rawMsg, "/echo "):
			text := strings.TrimPrefix(rawMsg, "/echo ")
			msg := onebot.MessageContent{onebot.TextSegment(text)}
			bot.SendPrivateMsg(ctx, userID, msg, false)

		case rawMsg == "/status":
			status, err := bot.GetStatus(ctx)
			if err != nil {
				logger.Error("get status failed", "error", err)
				return
			}
			var statusStr string
			if status.Online && status.Good {
				statusStr = "运行中，状态良好"
			} else {
				statusStr = "运行异常"
			}
			msg := onebot.MessageContent{onebot.TextSegment(statusStr)}
			bot.SendPrivateMsg(ctx, userID, msg, false)

		default:
			msg := onebot.MessageContent{
				onebot.TextSegment("收到消息: " + rawMsg + "\n发送 /help 查看可用命令"),
			}
			bot.SendPrivateMsg(ctx, userID, msg, false)
		}
	})

	// Group message handlers
	bot.OnGroupMessage(func(ctx context.Context, evt onebot.GroupMessageEvent) {
		rawMsg := evt.RawMessage
		groupID := evt.GroupID

		switch {
		case rawMsg == "/ping":
			msg := onebot.MessageContent{onebot.TextSegment("pong!")}
			bot.SendGroupMsg(ctx, groupID, msg, false)

		case strings.HasPrefix(rawMsg, "/echo "):
			text := strings.TrimPrefix(rawMsg, "/echo ")
			msg := onebot.MessageContent{
				onebot.AtSegment(fmt.Sprintf("%d", evt.UserID)),
				onebot.TextSegment(" " + text),
			}
			bot.SendGroupMsg(ctx, groupID, msg, false)
		}
	})

	// Notice handlers
	bot.OnNotice(func(ctx context.Context, evt onebot.OneBotEvent) {
		switch e := evt.(type) {
		case onebot.GroupIncreaseEvent:
			msg := onebot.MessageContent{
				onebot.AtSegment(fmt.Sprintf("%d", e.UserID)),
				onebot.TextSegment(" 欢迎加入群聊！"),
			}
			bot.SendGroupMsg(ctx, e.GroupID, msg, false)
		case onebot.FriendAddEvent:
			msg := onebot.MessageContent{onebot.TextSegment("你好，很高兴认识你！")}
			bot.SendPrivateMsg(ctx, e.UserID, msg, false)
		default:
			logger.Info("notice event", "type", e.GetPostType())
		}
	})
}
