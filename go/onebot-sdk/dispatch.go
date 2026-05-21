package onebot

import (
	"context"
	"log/slog"
	"sync"
)

// EventHandlerRegistry maps event types to handlers.
type EventHandlerRegistry struct {
	mu       sync.RWMutex
	handlers map[string][]EventHandler
	wildcard []EventHandler
}

func NewEventHandlerRegistry() *EventHandlerRegistry {
	return &EventHandlerRegistry{
		handlers: make(map[string][]EventHandler),
	}
}

// On registers a handler for a specific post_type.
func (r *EventHandlerRegistry) On(postType string, handler EventHandler) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.handlers[postType] = append(r.handlers[postType], handler)
}

// OnAll registers a handler for all events.
func (r *EventHandlerRegistry) OnAll(handler EventHandler) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.wildcard = append(r.wildcard, handler)
}

// Dispatch sends an event to all matching handlers (async, error-isolated).
func (r *EventHandlerRegistry) Dispatch(ctx context.Context, event OneBotEvent) {
	postType := event.GetPostType()

	r.mu.RLock()
	handlers := make([]EventHandler, 0, len(r.handlers[postType])+len(r.wildcard))
	handlers = append(handlers, r.handlers[postType]...)
	handlers = append(handlers, r.wildcard...)
	r.mu.RUnlock()

	for _, h := range handlers {
		go func(handler EventHandler) {
			defer func() {
				if r := recover(); r != nil {
					slog.Error("event handler panicked", "error", r)
				}
			}()
			handler(ctx, event)
		}(h)
	}
}
