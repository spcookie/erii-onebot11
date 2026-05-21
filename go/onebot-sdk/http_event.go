package onebot

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"strconv"
)

// HTTPEventServer receives events pushed via HTTP POST from an implementation.
type HTTPEventServer struct {
	config Config
	server *http.Server
	events chan []byte
}

func NewHTTPEventServer(config Config) *HTTPEventServer {
	return &HTTPEventServer{
		config: config,
		events: make(chan []byte, 100),
	}
}

func (s *HTTPEventServer) Events() <-chan []byte {
	return s.events
}

func (s *HTTPEventServer) Start(ctx context.Context) error {
	mux := http.NewServeMux()
	mux.HandleFunc("/", s.handleEvent)
	mux.HandleFunc("/event", s.handleEvent)

	addr := fmt.Sprintf("%s:%d", s.config.HTTPPostHost, s.config.HTTPPostPort)
	s.server = &http.Server{
		Addr:    addr,
		Handler: mux,
	}

	go func() {
		slog.Info("HTTP event server listening", "addr", addr)
		if err := s.server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("HTTP event server error", "error", err)
		}
	}()

	return nil
}

func (s *HTTPEventServer) Stop(ctx context.Context) error {
	if s.server != nil {
		return s.server.Shutdown(ctx)
	}
	return nil
}

func (s *HTTPEventServer) handleEvent(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	// Verify signature
	if s.config.Secret != "" {
		sig := r.Header.Get("X-Signature")
		if sig == "" || !VerifySign(body, s.config.Secret, sig) {
			http.Error(w, "Forbidden", http.StatusForbidden)
			slog.Warn("event signature verification failed")
			return
		}
	}

	selfIDStr := r.Header.Get("X-Self-ID")
	if selfIDStr != "" {
		selfID, _ := strconv.ParseInt(selfIDStr, 10, 64)
		_ = selfID // self_id is embedded in the event body
	}

	// Push raw event data
	select {
	case s.events <- body:
	default:
		slog.Warn("event channel full, dropping event")
	}

	w.WriteHeader(http.StatusNoContent)
}

// HTTPEventClient pushes events to an application via HTTP POST.
type HTTPEventClient struct {
	config    Config
	targetURL string
	client    *http.Client
}

func NewHTTPEventClient(config Config, targetURL string) *HTTPEventClient {
	return &HTTPEventClient{
		config:    config,
		targetURL: targetURL,
		client:    &http.Client{},
	}
}

func (c *HTTPEventClient) Start(ctx context.Context) error {
	return nil
}

func (c *HTTPEventClient) Stop(ctx context.Context) error {
	return nil
}

func (c *HTTPEventClient) PushEvent(ctx context.Context, data []byte) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.targetURL, bytes.NewReader(data))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Self-ID", fmt.Sprintf("%d", c.config.SelfID))

	if c.config.Secret != "" {
		req.Header.Set("X-Signature", SignBody(data, c.config.Secret))
	}

	resp, err := c.client.Do(req)
	if err != nil {
		return fmt.Errorf("push event: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		return fmt.Errorf("event push failed with status: %d", resp.StatusCode)
	}
	return nil
}
