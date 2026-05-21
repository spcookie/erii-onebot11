package onebot

import (
	"context"
	"sync"
	"testing"
	"time"
)

func TestEventHandlerRegistry_RegisterAndDispatch(t *testing.T) {
	registry := NewEventHandlerRegistry()
	received := make(chan bool, 1)

	registry.On("message", func(ctx context.Context, event OneBotEvent) {
		received <- true
	})

	registry.Dispatch(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})

	select {
	case <-received:
		// success
	case <-time.After(1 * time.Second):
		t.Fatal("handler was not called within timeout")
	}
}

func TestEventHandlerRegistry_DispatchToMatchingType(t *testing.T) {
	registry := NewEventHandlerRegistry()
	var mu sync.Mutex
	var messageCalled, noticeCalled bool

	registry.On("message", func(ctx context.Context, event OneBotEvent) {
		mu.Lock()
		messageCalled = true
		mu.Unlock()
	})
	registry.On("notice", func(ctx context.Context, event OneBotEvent) {
		mu.Lock()
		noticeCalled = true
		mu.Unlock()
	})

	// Dispatch a message event - only message handler should fire
	registry.Dispatch(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})

	time.Sleep(100 * time.Millisecond)

	mu.Lock()
	if !messageCalled {
		t.Error("expected message handler to be called")
	}
	if noticeCalled {
		t.Error("expected notice handler NOT to be called")
	}
	mu.Unlock()
}

func TestEventHandlerRegistry_MultipleHandlersForSameType(t *testing.T) {
	registry := NewEventHandlerRegistry()
	var wg sync.WaitGroup
	wg.Add(3)

	for i := 0; i < 3; i++ {
		registry.On("message", func(ctx context.Context, event OneBotEvent) {
			wg.Done()
		})
	}

	registry.Dispatch(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})

	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()

	select {
	case <-done:
		// all handlers called
	case <-time.After(2 * time.Second):
		t.Fatal("not all handlers were called within timeout")
	}
}

func TestEventHandlerRegistry_WildcardHandler(t *testing.T) {
	registry := NewEventHandlerRegistry()
	received := make(chan string, 3)

	registry.OnAll(func(ctx context.Context, event OneBotEvent) {
		received <- event.GetPostType()
	})

	// Dispatch events of different types
	registry.Dispatch(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})
	registry.Dispatch(context.Background(), GroupAdminEvent{
		EventBase:   EventBase{Time: 123, SelfID: 10001},
		PostType:    "notice",
		NoticeType:  "group_admin",
	})
	registry.Dispatch(context.Background(), FriendRequestEvent{
		EventBase:   EventBase{Time: 123, SelfID: 10001},
		PostType:    "request",
		RequestType: "friend",
	})

	time.Sleep(100 * time.Millisecond)

	results := make(map[string]bool)
	for i := 0; i < 3; i++ {
		select {
		case pt := <-received:
			results[pt] = true
		case <-time.After(1 * time.Second):
			t.Fatal("not all events received")
		}
	}

	for _, pt := range []string{"message", "notice", "request"} {
		if !results[pt] {
			t.Errorf("expected wildcard handler to receive %q event", pt)
		}
	}
}

func TestEventHandlerRegistry_PanicIsolation(t *testing.T) {
	registry := NewEventHandlerRegistry()
	done := make(chan bool, 1)

	// First handler panics
	registry.On("message", func(ctx context.Context, event OneBotEvent) {
		panic("test panic in handler")
	})

	// Second handler should still run
	registry.On("message", func(ctx context.Context, event OneBotEvent) {
		done <- true
	})

	registry.Dispatch(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})

	select {
	case <-done:
		// success - second handler ran despite first handler panic
	case <-time.After(2 * time.Second):
		t.Fatal("second handler was not called after first handler panic")
	}
}

func TestEventHandlerRegistry_DispatchWithNoHandlers(t *testing.T) {
	registry := NewEventHandlerRegistry()

	// Dispatching with no registered handlers should not panic
	registry.Dispatch(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})

	// Give goroutines time to complete (there are none, but just in case)
	time.Sleep(50 * time.Millisecond)
	// If we reach here without panicking, the test passes
}

func TestEventHandlerRegistry_ConcurrentDispatch(t *testing.T) {
	registry := NewEventHandlerRegistry()
	var mu sync.Mutex
	counter := 0

	registry.On("message", func(ctx context.Context, event OneBotEvent) {
		mu.Lock()
		counter++
		mu.Unlock()
	})

	// Dispatch many events concurrently
	var wg sync.WaitGroup
	for i := 0; i < 10; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			registry.Dispatch(context.Background(), PrivateMessageEvent{
				EventBase: EventBase{Time: 123, SelfID: 10001},
				PostType:  "message",
			})
		}()
	}
	wg.Wait()

	// Give goroutines time to complete
	time.Sleep(200 * time.Millisecond)

	mu.Lock()
	if counter != 10 {
		t.Errorf("expected 10 handler calls, got %d", counter)
	}
	mu.Unlock()
}

func TestEventHandlerRegistry_WildcardAndTypedHandler(t *testing.T) {
	registry := NewEventHandlerRegistry()
	var mu sync.Mutex
	typedCount, wildcardCount := 0, 0

	registry.On("message", func(ctx context.Context, event OneBotEvent) {
		mu.Lock()
		typedCount++
		mu.Unlock()
	})
	registry.OnAll(func(ctx context.Context, event OneBotEvent) {
		mu.Lock()
		wildcardCount++
		mu.Unlock()
	})

	registry.Dispatch(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})

	time.Sleep(100 * time.Millisecond)

	mu.Lock()
	if typedCount != 1 {
		t.Errorf("expected 1 typed handler call, got %d", typedCount)
	}
	if wildcardCount != 1 {
		t.Errorf("expected 1 wildcard handler call, got %d", wildcardCount)
	}
	mu.Unlock()
}

func TestEventHandlerRegistry_HasContext(t *testing.T) {
	registry := NewEventHandlerRegistry()
	received := make(chan context.Context, 1)

	registry.On("message", func(ctx context.Context, event OneBotEvent) {
		received <- ctx
	})

	type ctxKey struct{}
	ctx := context.WithValue(context.Background(), ctxKey{}, "test-value")
	registry.Dispatch(ctx, PrivateMessageEvent{
		EventBase: EventBase{Time: 123, SelfID: 10001},
		PostType:  "message",
	})

	select {
	case receivedCtx := <-received:
		if receivedCtx.Value(ctxKey{}) != "test-value" {
			t.Error("expected context value to be preserved")
		}
	case <-time.After(1 * time.Second):
		t.Fatal("handler was not called within timeout")
	}
}
