package onebot

import (
	"context"
	"log/slog"
	"time"
)

// ActionHandler is a function that processes an action and returns a response.
type ActionHandler func(ctx context.Context, action ActionRequest) (ActionResponse, error)

// EventHandler is a function that processes an event.
type EventHandler func(ctx context.Context, event OneBotEvent)

// ActionMiddleware intercepts action processing.
type ActionMiddleware func(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error)

// EventMiddleware intercepts event processing.
type EventMiddleware func(ctx context.Context, event OneBotEvent, next EventHandler)

// Middleware combines action and event interception.
type Middleware interface {
	InterceptAction(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error)
	InterceptEvent(ctx context.Context, event OneBotEvent, next EventHandler)
}

// ==================== Pipeline ====================

type middlewarePipeline struct {
	actionMiddlewares []ActionMiddleware
	eventMiddlewares  []EventMiddleware
}

func newMiddlewarePipeline() *middlewarePipeline {
	return &middlewarePipeline{}
}

func (p *middlewarePipeline) useAction(mw ActionMiddleware) {
	p.actionMiddlewares = append(p.actionMiddlewares, mw)
}

func (p *middlewarePipeline) useEvent(mw EventMiddleware) {
	p.eventMiddlewares = append(p.eventMiddlewares, mw)
}

func (p *middlewarePipeline) wrapAction(handler ActionHandler) ActionHandler {
	result := handler
	for i := len(p.actionMiddlewares) - 1; i >= 0; i-- {
		mw := p.actionMiddlewares[i]
		next := result
		result = func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
			return mw(ctx, action, next)
		}
	}
	return result
}

func (p *middlewarePipeline) wrapEvent(handler EventHandler) EventHandler {
	result := handler
	for i := len(p.eventMiddlewares) - 1; i >= 0; i-- {
		mw := p.eventMiddlewares[i]
		next := result
		result = func(ctx context.Context, event OneBotEvent) {
			mw(ctx, event, next)
		}
	}
	return result
}

// ==================== Built-in Middleware ====================

// LoggingMiddleware logs all actions and events.
type LoggingMiddleware struct{}

func (m *LoggingMiddleware) InterceptAction(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
	slog.Debug("action received", "action", action.Action, "echo", action.Echo)
	resp, err := next(ctx, action)
	if err != nil {
		slog.Error("action failed", "action", action.Action, "error", err)
	} else {
		slog.Debug("action completed", "action", action.Action, "status", resp.Status, "retcode", resp.RetCode)
	}
	return resp, err
}

func (m *LoggingMiddleware) InterceptEvent(ctx context.Context, event OneBotEvent, next EventHandler) {
	slog.Debug("event received", "post_type", event.GetPostType())
	next(ctx, event)
}

// RateLimitMiddleware limits the rate of send_message actions.
type RateLimitMiddleware struct {
	interval time.Duration
	lastCall time.Time
}

func NewRateLimitMiddleware(interval time.Duration) *RateLimitMiddleware {
	return &RateLimitMiddleware{interval: interval}
}

func (m *RateLimitMiddleware) InterceptAction(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
	if action.Action == "send_private_msg" || action.Action == "send_group_msg" || action.Action == "send_msg" {
		elapsed := time.Since(m.lastCall)
		if elapsed < m.interval {
			slog.Warn("rate limited", "action", action.Action)
			return NewFailedResponse(103, action.Echo), nil
		}
		m.lastCall = time.Now()
	}
	return next(ctx, action)
}

func (m *RateLimitMiddleware) InterceptEvent(ctx context.Context, event OneBotEvent, next EventHandler) {
	next(ctx, event)
}

// RetryMiddleware retries failed actions.
type RetryMiddleware struct {
	MaxRetries int
	Delay      time.Duration
}

func (m *RetryMiddleware) InterceptAction(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
	resp, err := next(ctx, action)
	if err == nil && resp.Status != "failed" {
		return resp, nil
	}

	for i := 0; i < m.MaxRetries; i++ {
		slog.Warn("retrying action", "action", action.Action, "attempt", i+1)
		time.Sleep(m.Delay)
		resp, err = next(ctx, action)
		if err == nil && resp.Status != "failed" {
			return resp, nil
		}
	}
	return resp, err
}

func (m *RetryMiddleware) InterceptEvent(ctx context.Context, event OneBotEvent, next EventHandler) {
	next(ctx, event)
}
