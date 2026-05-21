package onebot

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
)

// Bot is the main OneBot SDK interface.
type Bot struct {
	config     Config
	conn       *Connection
	registry   *EventHandlerRegistry
	pipeline   *middlewarePipeline
	plugins    []Plugin
	running    bool
}

// NewBot creates a new Bot instance.
func NewBot(config Config) (*Bot, error) {
	conn, err := BuildConnection(config)
	if err != nil {
		return nil, fmt.Errorf("build connection: %w", err)
	}

	return &Bot{
		config:   config,
		conn:     conn,
		registry: NewEventHandlerRegistry(),
		pipeline: newMiddlewarePipeline(),
	}, nil
}

// ==================== Lifecycle ====================

func (b *Bot) Start(ctx context.Context) error {
	// Load plugins
	for _, p := range b.plugins {
		slog.Info("loading plugin", "name", p.Name(), "version", p.Version())
		if err := p.OnLoad(ctx, b); err != nil {
			return fmt.Errorf("plugin %s load: %w", p.Name(), err)
		}
	}

	if err := b.conn.Start(ctx); err != nil {
		return fmt.Errorf("start connection: %w", err)
	}

	// Start event dispatch loop
	go b.eventLoop(ctx)

	b.running = true
	slog.Info("bot started")
	return nil
}

func (b *Bot) Stop(ctx context.Context) error {
	b.running = false

	// Unload plugins (reverse order)
	for i := len(b.plugins) - 1; i >= 0; i-- {
		p := b.plugins[i]
		slog.Info("unloading plugin", "name", p.Name())
		if err := p.OnUnload(ctx, b); err != nil {
			slog.Warn("plugin unload error", "name", p.Name(), "error", err)
		}
	}

	return b.conn.Stop(ctx)
}

func (b *Bot) eventLoop(ctx context.Context) {
	ch := b.conn.Events()
	if ch == nil {
		return
	}

	for {
		select {
		case <-ctx.Done():
			return
		case data, ok := <-ch:
			if !ok {
				return
			}
			event, err := ParseEvent(data)
			if err != nil {
				slog.Error("failed to parse event", "error", err)
				continue
			}

			wrappedHandler := b.pipeline.wrapEvent(func(ctx context.Context, event OneBotEvent) {
				b.registry.Dispatch(ctx, event)
			})
			wrappedHandler(ctx, event)
		}
	}
}

// ==================== Middleware ====================

func (b *Bot) UseAction(mw ActionMiddleware) {
	b.pipeline.useAction(mw)
}

func (b *Bot) UseEvent(mw EventMiddleware) {
	b.pipeline.useEvent(mw)
}

func (b *Bot) Use(mw Middleware) {
	b.pipeline.useAction(mw.InterceptAction)
	b.pipeline.useEvent(mw.InterceptEvent)
}

// ==================== Plugin Management ====================

func (b *Bot) LoadPlugin(p Plugin) {
	b.plugins = append(b.plugins, p)
}

// ==================== Event Handlers ====================

func (b *Bot) OnMessage(handler func(ctx context.Context, evt PrivateMessageEvent)) {
	b.registry.On("message", func(ctx context.Context, e OneBotEvent) {
		if evt, ok := e.(PrivateMessageEvent); ok {
			handler(ctx, evt)
		}
	})
}

func (b *Bot) OnGroupMessage(handler func(ctx context.Context, evt GroupMessageEvent)) {
	b.registry.On("message", func(ctx context.Context, e OneBotEvent) {
		if evt, ok := e.(GroupMessageEvent); ok {
			handler(ctx, evt)
		}
	})
}

func (b *Bot) OnNotice(handler func(ctx context.Context, evt OneBotEvent)) {
	b.registry.On("notice", func(ctx context.Context, e OneBotEvent) {
		handler(ctx, e)
	})
}

func (b *Bot) OnRequest(handler func(ctx context.Context, evt OneBotEvent)) {
	b.registry.On("request", func(ctx context.Context, e OneBotEvent) {
		handler(ctx, e)
	})
}

func (b *Bot) OnMetaEvent(handler func(ctx context.Context, evt OneBotEvent)) {
	b.registry.On("meta_event", func(ctx context.Context, e OneBotEvent) {
		handler(ctx, e)
	})
}

// ==================== API Methods ====================

func (b *Bot) call(ctx context.Context, action string, params map[string]any) (ActionResponse, error) {
	if !b.running {
		return ActionResponse{}, fmt.Errorf("bot not started")
	}

	wrappedHandler := b.pipeline.wrapAction(func(ctx context.Context, req ActionRequest) (ActionResponse, error) {
		return b.conn.Call(ctx, action, params)
	})

	return wrappedHandler(ctx, ActionRequest{Action: action, Params: params})
}

// --- Message Sending ---

func (b *Bot) SendPrivateMsg(ctx context.Context, userID int64, message MessageContent, autoEscape bool) (int64, error) {
	resp, err := b.call(ctx, "send_private_msg", map[string]any{
		"user_id":     userID,
		"message":     segmentsToAny(message),
		"auto_escape": autoEscape,
	})
	if err != nil {
		return 0, err
	}
	if data, ok := resp.Data.(map[string]any); ok {
		if msgID, ok := data["message_id"].(float64); ok {
			return int64(msgID), nil
		}
	}
	return 0, nil
}

func (b *Bot) SendGroupMsg(ctx context.Context, groupID int64, message MessageContent, autoEscape bool) (int64, error) {
	resp, err := b.call(ctx, "send_group_msg", map[string]any{
		"group_id":    groupID,
		"message":     segmentsToAny(message),
		"auto_escape": autoEscape,
	})
	if err != nil {
		return 0, err
	}
	if data, ok := resp.Data.(map[string]any); ok {
		if msgID, ok := data["message_id"].(float64); ok {
			return int64(msgID), nil
		}
	}
	return 0, nil
}

func (b *Bot) DeleteMsg(ctx context.Context, messageID int64) error {
	_, err := b.call(ctx, "delete_msg", map[string]any{"message_id": messageID})
	return err
}

func (b *Bot) SendLike(ctx context.Context, userID int64, times int) error {
	if times > 10 {
		times = 10
	}
	_, err := b.call(ctx, "send_like", map[string]any{"user_id": userID, "times": times})
	return err
}

// --- Group Management ---

func (b *Bot) SetGroupKick(ctx context.Context, groupID, userID int64, rejectAddRequest bool) error {
	_, err := b.call(ctx, "set_group_kick", map[string]any{
		"group_id":           groupID,
		"user_id":            userID,
		"reject_add_request": rejectAddRequest,
	})
	return err
}

func (b *Bot) SetGroupBan(ctx context.Context, groupID, userID int64, duration int64) error {
	_, err := b.call(ctx, "set_group_ban", map[string]any{
		"group_id": groupID,
		"user_id":  userID,
		"duration": duration,
	})
	return err
}

func (b *Bot) SetGroupWholeBan(ctx context.Context, groupID int64, enable bool) error {
	_, err := b.call(ctx, "set_group_whole_ban", map[string]any{
		"group_id": groupID,
		"enable":   enable,
	})
	return err
}

func (b *Bot) SetGroupAdmin(ctx context.Context, groupID, userID int64, enable bool) error {
	_, err := b.call(ctx, "set_group_admin", map[string]any{
		"group_id": groupID,
		"user_id":  userID,
		"enable":   enable,
	})
	return err
}

func (b *Bot) SetGroupCard(ctx context.Context, groupID, userID int64, card string) error {
	_, err := b.call(ctx, "set_group_card", map[string]any{
		"group_id": groupID,
		"user_id":  userID,
		"card":     card,
	})
	return err
}

func (b *Bot) SetGroupName(ctx context.Context, groupID int64, groupName string) error {
	_, err := b.call(ctx, "set_group_name", map[string]any{
		"group_id":   groupID,
		"group_name": groupName,
	})
	return err
}

func (b *Bot) SetGroupLeave(ctx context.Context, groupID int64, isDismiss bool) error {
	_, err := b.call(ctx, "set_group_leave", map[string]any{
		"group_id":  groupID,
		"is_dismiss": isDismiss,
	})
	return err
}

func (b *Bot) SetGroupSpecialTitle(ctx context.Context, groupID, userID int64, specialTitle string, duration int64) error {
	_, err := b.call(ctx, "set_group_special_title", map[string]any{
		"group_id":      groupID,
		"user_id":       userID,
		"special_title": specialTitle,
		"duration":      duration,
	})
	return err
}

// --- Request Handling ---

func (b *Bot) SetFriendAddRequest(ctx context.Context, flag string, approve bool, remark string) error {
	_, err := b.call(ctx, "set_friend_add_request", map[string]any{
		"flag":    flag,
		"approve": approve,
		"remark":  remark,
	})
	return err
}

func (b *Bot) SetGroupAddRequest(ctx context.Context, flag, subType string, approve bool, reason string) error {
	_, err := b.call(ctx, "set_group_add_request", map[string]any{
		"flag":     flag,
		"sub_type": subType,
		"approve":  approve,
		"reason":   reason,
	})
	return err
}

// --- Info Queries ---

func (b *Bot) GetLoginInfo(ctx context.Context) (LoginInfo, error) {
	resp, err := b.call(ctx, "get_login_info", map[string]any{})
	if err != nil {
		return LoginInfo{}, err
	}
	return parseData[LoginInfo](resp)
}

func (b *Bot) GetStrangerInfo(ctx context.Context, userID int64, noCache bool) (StrangerInfo, error) {
	resp, err := b.call(ctx, "get_stranger_info", map[string]any{
		"user_id":  userID,
		"no_cache": noCache,
	})
	if err != nil {
		return StrangerInfo{}, err
	}
	return parseData[StrangerInfo](resp)
}

func (b *Bot) GetFriendList(ctx context.Context) ([]FriendInfo, error) {
	resp, err := b.call(ctx, "get_friend_list", map[string]any{})
	if err != nil {
		return nil, err
	}
	return parseDataSlice[FriendInfo](resp)
}

func (b *Bot) GetGroupInfo(ctx context.Context, groupID int64, noCache bool) (GroupInfo, error) {
	resp, err := b.call(ctx, "get_group_info", map[string]any{
		"group_id": groupID,
		"no_cache": noCache,
	})
	if err != nil {
		return GroupInfo{}, err
	}
	return parseData[GroupInfo](resp)
}

func (b *Bot) GetGroupList(ctx context.Context) ([]GroupInfo, error) {
	resp, err := b.call(ctx, "get_group_list", map[string]any{})
	if err != nil {
		return nil, err
	}
	return parseDataSlice[GroupInfo](resp)
}

func (b *Bot) GetGroupMemberList(ctx context.Context, groupID int64) ([]GroupMemberInfo, error) {
	resp, err := b.call(ctx, "get_group_member_list", map[string]any{
		"group_id": groupID,
	})
	if err != nil {
		return nil, err
	}
	return parseDataSlice[GroupMemberInfo](resp)
}

// --- System ---

func (b *Bot) GetVersionInfo(ctx context.Context) (VersionInfo, error) {
	resp, err := b.call(ctx, "get_version_info", map[string]any{})
	if err != nil {
		return VersionInfo{}, err
	}
	return parseData[VersionInfo](resp)
}

func (b *Bot) GetStatus(ctx context.Context) (StatusInfo, error) {
	resp, err := b.call(ctx, "get_status", map[string]any{})
	if err != nil {
		return StatusInfo{}, err
	}
	return parseData[StatusInfo](resp)
}

func (b *Bot) CanSendImage(ctx context.Context) (bool, error) {
	resp, err := b.call(ctx, "can_send_image", map[string]any{})
	if err != nil {
		return false, err
	}
	if data, ok := resp.Data.(map[string]any); ok {
		if yes, ok := data["yes"].(bool); ok {
			return yes, nil
		}
	}
	return false, nil
}

func (b *Bot) CanSendRecord(ctx context.Context) (bool, error) {
	resp, err := b.call(ctx, "can_send_record", map[string]any{})
	if err != nil {
		return false, err
	}
	if data, ok := resp.Data.(map[string]any); ok {
		if yes, ok := data["yes"].(bool); ok {
			return yes, nil
		}
	}
	return false, nil
}

func (b *Bot) CleanCache(ctx context.Context) error {
	_, err := b.call(ctx, "clean_cache", map[string]any{})
	return err
}

// ==================== Helper ====================

func segmentsToAny(segments MessageContent) []any {
	result := make([]any, len(segments))
	for i, seg := range segments {
		data := make(map[string]any, len(seg.Data))
		for k, v := range seg.Data {
			data[k] = v
		}
		result[i] = map[string]any{
			"type": seg.Type,
			"data": data,
		}
	}
	return result
}

func parseData[T any](resp ActionResponse) (T, error) {
	var result T
	data, err := json.Marshal(resp.Data)
	if err != nil {
		return result, err
	}
	if err := json.Unmarshal(data, &result); err != nil {
		return result, err
	}
	return result, nil
}

func parseDataSlice[T any](resp ActionResponse) ([]T, error) {
	var result []T
	data, err := json.Marshal(resp.Data)
	if err != nil {
		return result, err
	}
	if err := json.Unmarshal(data, &result); err != nil {
		return result, err
	}
	return result, nil
}
