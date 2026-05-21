package onebot

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/hex"
	"fmt"
	"sync"
	"time"
)

// EchoTracker manages request/response matching via echo IDs.
type EchoTracker struct {
	mu      sync.Mutex
	pending map[string]chan ActionResponse
	nextID  int64
}

func NewEchoTracker() *EchoTracker {
	return &EchoTracker{
		pending: make(map[string]chan ActionResponse),
		nextID:  1,
	}
}

func (e *EchoTracker) GenerateEcho() string {
	e.mu.Lock()
	defer e.mu.Unlock()
	id := e.nextID
	e.nextID++
	return fmt.Sprintf("go-echo-%d", id)
}

func (e *EchoTracker) Register(echo string, timeout time.Duration) <-chan ActionResponse {
	ch := make(chan ActionResponse, 1)
	e.mu.Lock()
	e.pending[echo] = ch
	e.mu.Unlock()

	if timeout > 0 {
		go func() {
			timer := time.NewTimer(timeout)
			defer timer.Stop()
			select {
			case <-timer.C:
				e.Resolve(echo, NewFailedResponse(-1, echo))
			case <-ch:
				// completed normally
			}
		}()
	}
	return ch
}

func (e *EchoTracker) Resolve(echo string, resp ActionResponse) {
	e.mu.Lock()
	ch, ok := e.pending[echo]
	if ok {
		delete(e.pending, echo)
	}
	e.mu.Unlock()

	if ok {
		select {
		case ch <- resp:
		default:
		}
	}
}

func (e *EchoTracker) CancelAll() {
	e.mu.Lock()
	defer e.mu.Unlock()
	for echo, ch := range e.pending {
		ch <- NewFailedResponse(-2, echo)
		delete(e.pending, echo)
	}
}

// SignBody computes HMAC-SHA1 signature of body with secret.
func SignBody(body []byte, secret string) string {
	mac := hmac.New(sha1.New, []byte(secret))
	mac.Write(body)
	return "sha1=" + hex.EncodeToString(mac.Sum(nil))
}

// VerifySign checks the HMAC-SHA1 signature of the body.
func VerifySign(body []byte, secret, expected string) bool {
	actual := SignBody(body, secret)
	return hmac.Equal([]byte(actual), []byte(expected))
}
