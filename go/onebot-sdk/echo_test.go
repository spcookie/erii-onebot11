package onebot

import (
	"testing"
	"time"
)

func TestEchoTracker_GenerateEcho_Unique(t *testing.T) {
	tracker := NewEchoTracker()
	ids := make(map[string]bool)
	for i := 0; i < 100; i++ {
		id := tracker.GenerateEcho()
		if ids[id] {
			t.Errorf("duplicate echo ID generated: %s", id)
		}
		ids[id] = true
	}
}

func TestEchoTracker_GenerateEcho_Format(t *testing.T) {
	tracker := NewEchoTracker()
	id := tracker.GenerateEcho()
	if id == "" {
		t.Error("expected non-empty echo")
	}
	if id[:7] != "go-echo" {
		t.Errorf("expected echo to start with 'go-echo', got %q", id)
	}
}

func TestEchoTracker_RegisterAndResolve(t *testing.T) {
	tracker := NewEchoTracker()
	echo := tracker.GenerateEcho()

	ch := tracker.Register(echo, 0)
	if ch == nil {
		t.Fatal("expected non-nil channel from Register")
	}

	resp := NewOKResponse(map[string]any{"result": "success"}, echo)
	tracker.Resolve(echo, resp)

	select {
	case received := <-ch:
		if received.Status != "ok" {
			t.Errorf("expected status 'ok', got %q", received.Status)
		}
		if received.Echo != echo {
			t.Errorf("expected echo %q, got %q", echo, received.Echo)
		}
	case <-time.After(1 * time.Second):
		t.Fatal("timeout waiting for resolved response")
	}
}

func TestEchoTracker_ResolveUnknownEcho(t *testing.T) {
	tracker := NewEchoTracker()
	// Resolve with an echo that was never registered should not panic
	resp := NewOKResponse(nil, "unknown")
	tracker.Resolve("unknown", resp)
	// If we reach here without panicking, the test passes
}

func TestEchoTracker_Timeout(t *testing.T) {
	tracker := NewEchoTracker()
	echo := tracker.GenerateEcho()

	ch := tracker.Register(echo, 10*time.Millisecond)

	select {
	case resp := <-ch:
		if resp.Status != "failed" {
			t.Errorf("expected status 'failed' from timeout, got %q", resp.Status)
		}
		if resp.RetCode != -1 {
			t.Errorf("expected retcode -1 from timeout, got %d", resp.RetCode)
		}
		if resp.Echo != echo {
			t.Errorf("expected echo %q, got %q", echo, resp.Echo)
		}
	case <-time.After(1 * time.Second):
		t.Fatal("timeout waiting for timeout response")
	}
}

func TestEchoTracker_TimeoutDoesNotFireAfterResolve(t *testing.T) {
	tracker := NewEchoTracker()
	echo := tracker.GenerateEcho()

	ch := tracker.Register(echo, 100*time.Millisecond)

	// Resolve before timeout fires
	resp := NewOKResponse(map[string]any{"result": "success"}, echo)
	tracker.Resolve(echo, resp)

	select {
	case received := <-ch:
		if received.Status != "ok" {
			t.Errorf("expected status 'ok', got %q", received.Status)
		}
	case <-time.After(200 * time.Millisecond):
		t.Fatal("timeout waiting for resolved response")
	}
}

func TestEchoTracker_CancelAll(t *testing.T) {
	tracker := NewEchoTracker()
	echo1 := tracker.GenerateEcho()
	echo2 := tracker.GenerateEcho()

	ch1 := tracker.Register(echo1, 0)
	ch2 := tracker.Register(echo2, 0)

	tracker.CancelAll()

	// Both channels should receive failed responses
	resp1 := <-ch1
	if resp1.Status != "failed" || resp1.RetCode != -2 {
		t.Errorf("echo1: expected failed/-2, got status=%q retcode=%d", resp1.Status, resp1.RetCode)
	}

	resp2 := <-ch2
	if resp2.Status != "failed" || resp2.RetCode != -2 {
		t.Errorf("echo2: expected failed/-2, got status=%q retcode=%d", resp2.Status, resp2.RetCode)
	}
}

func TestEchoTracker_CancelAllEmpty(t *testing.T) {
	tracker := NewEchoTracker()
	// Canceling with no pending should not panic
	tracker.CancelAll()
}

func TestEchoTracker_MultipleResolveIsIdempotent(t *testing.T) {
	tracker := NewEchoTracker()
	echo := tracker.GenerateEcho()
	ch := tracker.Register(echo, 0)

	resp1 := NewOKResponse(map[string]any{"first": true}, echo)
	tracker.Resolve(echo, resp1)

	// Second resolve on same echo should be a no-op (channel already received)
	resp2 := NewFailedResponse(999, echo)
	tracker.Resolve(echo, resp2)

	select {
	case received := <-ch:
		if received.Status != "ok" {
			t.Errorf("expected first response, got status=%q", received.Status)
		}
	case <-time.After(1 * time.Second):
		t.Fatal("timeout")
	}
}

func TestSignBody(t *testing.T) {
	body := []byte("test body")
	secret := "secret-key"

	sig := SignBody(body, secret)
	if sig == "" {
		t.Error("expected non-empty signature")
	}
	if sig[:5] != "sha1=" {
		t.Errorf("expected signature to start with 'sha1=', got %q", sig[:5])
	}
}

func TestVerifySign(t *testing.T) {
	body := []byte("test body")
	secret := "secret-key"
	sig := SignBody(body, secret)

	if !VerifySign(body, secret, sig) {
		t.Error("expected signature verification to succeed")
	}
}

func TestVerifySign_InvalidSecret(t *testing.T) {
	body := []byte("test body")
	secret := "secret-key"
	sig := SignBody(body, secret)

	if VerifySign(body, "wrong-secret", sig) {
		t.Error("expected signature verification to fail with wrong secret")
	}
}

func TestVerifySign_InvalidBody(t *testing.T) {
	body := []byte("test body")
	secret := "secret-key"
	sig := SignBody(body, secret)

	if VerifySign([]byte("tampered body"), secret, sig) {
		t.Error("expected signature verification to fail with tampered body")
	}
}

func TestSignBody_Deterministic(t *testing.T) {
	body := []byte("test body")
	secret := "secret-key"

	sig1 := SignBody(body, secret)
	sig2 := SignBody(body, secret)

	if sig1 != sig2 {
		t.Error("expected same signature for same input")
	}
}
