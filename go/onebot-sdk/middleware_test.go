package onebot

import (
	"context"
	"testing"
	"time"
)

func TestMiddlewarePipeline_ActionExecutionOrder(t *testing.T) {
	pipeline := newMiddlewarePipeline()
	var order []int

	pipeline.useAction(func(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
		order = append(order, 1)
		return next(ctx, action)
	})
	pipeline.useAction(func(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
		order = append(order, 2)
		return next(ctx, action)
	})
	pipeline.useAction(func(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
		order = append(order, 3)
		return next(ctx, action)
	})

	handler := pipeline.wrapAction(func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		order = append(order, 4)
		return ActionResponse{Status: "ok"}, nil
	})

	resp, err := handler(context.Background(), ActionRequest{})
	if err != nil {
		t.Fatalf("handler returned error: %v", err)
	}
	if resp.Status != "ok" {
		t.Errorf("expected status 'ok', got %q", resp.Status)
	}

	expected := []int{1, 2, 3, 4}
	if len(order) != len(expected) {
		t.Fatalf("expected order %v, got %v", expected, order)
	}
	for i := range expected {
		if order[i] != expected[i] {
			t.Errorf("at position %d: expected %d, got %d", i, expected[i], order[i])
		}
	}
}

func TestMiddlewarePipeline_ActionShortCircuit(t *testing.T) {
	pipeline := newMiddlewarePipeline()

	pipeline.useAction(func(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
		return ActionResponse{Status: "short_circuit", RetCode: -1}, nil
	})

	nextCalled := false
	pipeline.useAction(func(ctx context.Context, action ActionRequest, next ActionHandler) (ActionResponse, error) {
		nextCalled = true
		return next(ctx, action)
	})

	handlerCalled := false
	handler := pipeline.wrapAction(func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		handlerCalled = true
		return ActionResponse{Status: "ok"}, nil
	})

	resp, err := handler(context.Background(), ActionRequest{})
	if err != nil {
		t.Fatalf("handler returned error: %v", err)
	}
	if resp.Status != "short_circuit" {
		t.Errorf("expected status 'short_circuit', got %q", resp.Status)
	}
	if resp.RetCode != -1 {
		t.Errorf("expected retcode -1, got %d", resp.RetCode)
	}
	if nextCalled {
		t.Error("expected second middleware to NOT be called (short-circuited)")
	}
	if handlerCalled {
		t.Error("expected handler to NOT be called (short-circuited)")
	}
}

func TestMiddlewarePipeline_EventExecutionOrder(t *testing.T) {
	pipeline := newMiddlewarePipeline()
	var order []int

	pipeline.useEvent(func(ctx context.Context, event OneBotEvent, next EventHandler) {
		order = append(order, 1)
		next(ctx, event)
	})
	pipeline.useEvent(func(ctx context.Context, event OneBotEvent, next EventHandler) {
		order = append(order, 2)
		next(ctx, event)
	})
	pipeline.useEvent(func(ctx context.Context, event OneBotEvent, next EventHandler) {
		order = append(order, 3)
		next(ctx, event)
	})

	handler := pipeline.wrapEvent(func(ctx context.Context, event OneBotEvent) {
		order = append(order, 4)
	})

	handler(context.Background(), PrivateMessageEvent{})

	expected := []int{1, 2, 3, 4}
	if len(order) != len(expected) {
		t.Fatalf("expected order %v, got %v", expected, order)
	}
	for i := range expected {
		if order[i] != expected[i] {
			t.Errorf("at position %d: expected %d, got %d", i, expected[i], order[i])
		}
	}
}

func TestMiddlewarePipeline_EventShortCircuit(t *testing.T) {
	pipeline := newMiddlewarePipeline()

	pipeline.useEvent(func(ctx context.Context, event OneBotEvent, next EventHandler) {
		// Short circuit: do not call next
	})

	nextCalled := false
	pipeline.useEvent(func(ctx context.Context, event OneBotEvent, next EventHandler) {
		nextCalled = true
		next(ctx, event)
	})

	handlerCalled := false
	handler := pipeline.wrapEvent(func(ctx context.Context, event OneBotEvent) {
		handlerCalled = true
	})

	handler(context.Background(), PrivateMessageEvent{})

	if nextCalled {
		t.Error("expected second middleware to NOT be called (short-circuited)")
	}
	if handlerCalled {
		t.Error("expected handler to NOT be called (short-circuited)")
	}
}

func TestLoggingMiddleware_Action(t *testing.T) {
	lm := &LoggingMiddleware{}

	handler := func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		return ActionResponse{Status: "ok"}, nil
	}

	resp, err := lm.InterceptAction(context.Background(), ActionRequest{Action: "test_action"}, handler)
	if err != nil {
		t.Fatalf("InterceptAction returned error: %v", err)
	}
	if resp.Status != "ok" {
		t.Errorf("expected status 'ok', got %q", resp.Status)
	}
}

func TestLoggingMiddleware_Event(t *testing.T) {
	lm := &LoggingMiddleware{}
	called := false

	handler := func(ctx context.Context, event OneBotEvent) {
		called = true
	}

	lm.InterceptEvent(context.Background(), PrivateMessageEvent{
		EventBase: EventBase{Time: 123},
		PostType:  "message",
	}, handler)

	if !called {
		t.Error("expected event handler to be called")
	}
}

func TestRateLimitMiddleware_SendsFirstMessage(t *testing.T) {
	rl := NewRateLimitMiddleware(1 * time.Second)
	called := false

	handler := func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		called = true
		return ActionResponse{Status: "ok"}, nil
	}

	resp, err := rl.InterceptAction(context.Background(),
		ActionRequest{Action: "send_group_msg", Echo: "echo1"}, handler)
	if err != nil {
		t.Fatalf("InterceptAction returned error: %v", err)
	}
	if resp.Status != "ok" {
		t.Errorf("expected status 'ok', got %q", resp.Status)
	}
	if !called {
		t.Error("expected handler to be called on first message")
	}
}

func TestRateLimitMiddleware_BlocksSecondMessage(t *testing.T) {
	rl := NewRateLimitMiddleware(1 * time.Second)
	callCount := 0

	handler := func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		callCount++
		return ActionResponse{Status: "ok"}, nil
	}

	// First call passes through
	_, _ = rl.InterceptAction(context.Background(),
		ActionRequest{Action: "send_group_msg", Echo: "echo1"}, handler)

	// Second call should be rate limited (within the same second)
	resp, err := rl.InterceptAction(context.Background(),
		ActionRequest{Action: "send_group_msg", Echo: "echo2"}, handler)
	if err != nil {
		t.Fatalf("InterceptAction returned error: %v", err)
	}

	if callCount != 1 {
		t.Errorf("expected 1 handler call, got %d", callCount)
	}
	if resp.Status != "failed" {
		t.Errorf("expected status 'failed', got %q", resp.Status)
	}
	if resp.RetCode != 103 {
		t.Errorf("expected retcode 103 (rate limited), got %d", resp.RetCode)
	}
}

func TestRateLimitMiddleware_AllowsDifferentActions(t *testing.T) {
	rl := NewRateLimitMiddleware(1 * time.Second)
	callCount := 0

	handler := func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		callCount++
		return ActionResponse{Status: "ok"}, nil
	}

	// Send a message first
	_, _ = rl.InterceptAction(context.Background(),
		ActionRequest{Action: "send_group_msg", Echo: "echo1"}, handler)

	// Different action should not be rate limited
	resp, err := rl.InterceptAction(context.Background(),
		ActionRequest{Action: "get_group_info", Echo: "echo2"}, handler)
	if err != nil {
		t.Fatalf("InterceptAction returned error: %v", err)
	}

	if resp.Status != "ok" {
		t.Errorf("expected status 'ok' for non-message action, got %q", resp.Status)
	}
	if callCount != 2 {
		t.Errorf("expected 2 handler calls, got %d", callCount)
	}
}

func TestRateLimitMiddleware_Event(t *testing.T) {
	rl := NewRateLimitMiddleware(1 * time.Second)
	called := false

	handler := func(ctx context.Context, event OneBotEvent) {
		called = true
	}

	rl.InterceptEvent(context.Background(), PrivateMessageEvent{}, handler)

	if !called {
		t.Error("expected event handler to be called")
	}
}

func TestRetryMiddleware_NoRetryOnSuccess(t *testing.T) {
	rm := &RetryMiddleware{MaxRetries: 3, Delay: 10 * time.Millisecond}
	callCount := 0

	handler := func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		callCount++
		return ActionResponse{Status: "ok"}, nil
	}

	resp, err := rm.InterceptAction(context.Background(), ActionRequest{}, handler)
	if err != nil {
		t.Fatalf("InterceptAction returned error: %v", err)
	}
	if resp.Status != "ok" {
		t.Errorf("expected status 'ok', got %q", resp.Status)
	}
	if callCount != 1 {
		t.Errorf("expected 1 call, got %d", callCount)
	}
}

func TestRetryMiddleware_RetriesOnFailure(t *testing.T) {
	rm := &RetryMiddleware{MaxRetries: 2, Delay: 10 * time.Millisecond}
	callCount := 0

	handler := func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		callCount++
		if callCount < 3 {
			return ActionResponse{Status: "failed", RetCode: -1}, nil
		}
		return ActionResponse{Status: "ok"}, nil
	}

	resp, err := rm.InterceptAction(context.Background(), ActionRequest{}, handler)
	if err != nil {
		t.Fatalf("InterceptAction returned error: %v", err)
	}
	if resp.Status != "ok" {
		t.Errorf("expected status 'ok' after retries, got %q", resp.Status)
	}
	if callCount != 3 {
		t.Errorf("expected 3 calls (1 initial + 2 retries), got %d", callCount)
	}
}

func TestRetryMiddleware_ExhaustsRetries(t *testing.T) {
	rm := &RetryMiddleware{MaxRetries: 2, Delay: 10 * time.Millisecond}
	callCount := 0

	handler := func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		callCount++
		return ActionResponse{Status: "failed", RetCode: -1}, nil
	}

	resp, err := rm.InterceptAction(context.Background(), ActionRequest{}, handler)
	if err != nil {
		t.Fatalf("InterceptAction returned error: %v", err)
	}
	if resp.Status != "failed" {
		t.Errorf("expected status 'failed' after exhausting retries, got %q", resp.Status)
	}
	if callCount != 3 {
		t.Errorf("expected 3 calls (1 initial + 2 retries), got %d", callCount)
	}
}

func TestRetryMiddleware_Event(t *testing.T) {
	rm := &RetryMiddleware{MaxRetries: 3, Delay: 10 * time.Millisecond}
	called := false

	handler := func(ctx context.Context, event OneBotEvent) {
		called = true
	}

	rm.InterceptEvent(context.Background(), PrivateMessageEvent{}, handler)

	if !called {
		t.Error("expected event handler to be called")
	}
}

func TestMiddlewarePipeline_EmptyAction(t *testing.T) {
	pipeline := newMiddlewarePipeline()

	called := false
	handler := pipeline.wrapAction(func(ctx context.Context, action ActionRequest) (ActionResponse, error) {
		called = true
		return ActionResponse{Status: "ok"}, nil
	})

	resp, err := handler(context.Background(), ActionRequest{})
	if err != nil {
		t.Fatalf("handler returned error: %v", err)
	}
	if !called {
		t.Error("expected handler to be called")
	}
	if resp.Status != "ok" {
		t.Errorf("expected status 'ok', got %q", resp.Status)
	}
}

func TestMiddlewarePipeline_EmptyEvent(t *testing.T) {
	pipeline := newMiddlewarePipeline()

	called := false
	handler := pipeline.wrapEvent(func(ctx context.Context, event OneBotEvent) {
		called = true
	})

	handler(context.Background(), PrivateMessageEvent{})

	if !called {
		t.Error("expected handler to be called")
	}
}
