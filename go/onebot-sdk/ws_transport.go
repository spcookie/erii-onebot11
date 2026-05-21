package onebot

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"net/url"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// ==================== WS Action Client ====================

// WSActionClient sends actions via WebSocket and receives responses.
type WSActionClient struct {
	config    Config
	wsURL     string
	role      string
	conn      *websocket.Conn
	mu        sync.Mutex
	pending   map[string]chan ActionResponse
	closeCh   chan struct{}
	reconnect bool
}

func NewWSActionClient(config Config, wsURL, role string) *WSActionClient {
	return &WSActionClient{
		config:  config,
		wsURL:   wsURL,
		role:    role,
		pending: make(map[string]chan ActionResponse),
		closeCh: make(chan struct{}),
	}
}

func (c *WSActionClient) Start(ctx context.Context) error {
	return c.connect(ctx)
}

func (c *WSActionClient) Stop(ctx context.Context) error {
	close(c.closeCh)
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}

func (c *WSActionClient) connect(ctx context.Context) error {
	u, err := url.Parse(c.wsURL)
	if err != nil {
		return err
	}

	if c.config.AccessToken != "" {
		q := u.Query()
		q.Set("access_token", c.config.AccessToken)
		u.RawQuery = q.Encode()
	}

	header := http.Header{}
	header.Set("X-Client-Role", c.role)
	if c.config.SelfID != 0 {
		header.Set("X-Self-ID", fmt.Sprintf("%d", c.config.SelfID))
	}

	conn, _, err := websocket.DefaultDialer.DialContext(ctx, u.String(), header)
	if err != nil {
		return fmt.Errorf("ws dial: %w", err)
	}

	c.conn = conn
	slog.Info("WS connected", "url", c.wsURL, "role", c.role)

	go c.readLoop()
	go c.heartbeat()
	go c.reconnectLoop(ctx)

	return nil
}

func (c *WSActionClient) readLoop() {
	defer func() {
		c.mu.Lock()
		c.conn = nil
		c.mu.Unlock()
	}()
	for {
		_, msg, err := c.conn.ReadMessage()
		if err != nil {
			slog.Debug("WS read error", "error", err)
			return
		}

		var resp ActionResponse
		if err := json.Unmarshal(msg, &resp); err != nil {
			slog.Error("Failed to parse WS response", "error", err)
			continue
		}

		if resp.Echo != "" {
			c.mu.Lock()
			ch, ok := c.pending[resp.Echo]
			if ok {
				delete(c.pending, resp.Echo)
			}
			c.mu.Unlock()
			if ok {
				ch <- resp
			}
		}
	}
}

func (c *WSActionClient) heartbeat() {
	ticker := time.NewTicker(time.Duration(c.config.HeartbeatInterval) * time.Millisecond)
	defer ticker.Stop()

	for {
		select {
		case <-c.closeCh:
			return
		case <-ticker.C:
			c.mu.Lock()
			if c.conn != nil {
				c.conn.WriteMessage(websocket.PingMessage, nil)
			}
			c.mu.Unlock()
		}
	}
}

func (c *WSActionClient) reconnectLoop(ctx context.Context) {
	interval := time.Duration(c.config.ReconnectInterval) * time.Millisecond
	if interval == 0 {
		interval = 3 * time.Second
	}

	for {
		select {
		case <-c.closeCh:
			return
		case <-time.After(interval):
			if c.conn == nil {
				slog.Info("WS reconnecting", "url", c.wsURL)
				if err := c.connect(ctx); err != nil {
					slog.Warn("WS reconnect failed", "error", err)
				}
			}
		}
	}
}

func (c *WSActionClient) Call(ctx context.Context, req ActionRequest) (ActionResponse, error) {
	ch := make(chan ActionResponse, 1)

	c.mu.Lock()
	c.pending[req.Echo] = ch
	conn := c.conn
	c.mu.Unlock()

	// Serialize request
	payload := map[string]any{
		"action": req.Action,
		"params": req.Params,
		"echo":   req.Echo,
	}
	data, err := json.Marshal(payload)
	if err != nil {
		c.mu.Lock()
		delete(c.pending, req.Echo)
		c.mu.Unlock()
		return ActionResponse{}, err
	}

	c.mu.Lock()
	err = conn.WriteMessage(websocket.TextMessage, data)
	c.mu.Unlock()
	if err != nil {
		c.mu.Lock()
		delete(c.pending, req.Echo)
		c.mu.Unlock()
		return ActionResponse{}, fmt.Errorf("ws write: %w", err)
	}

	select {
	case <-ctx.Done():
		c.mu.Lock()
		delete(c.pending, req.Echo)
		c.mu.Unlock()
		return ActionResponse{}, ctx.Err()
	case resp := <-ch:
		return resp, nil
	}
}

// ==================== WS Event Client (reverse WS) ====================

// WSEventClient receives events via WebSocket (reverse WS mode).
type WSEventClient struct {
	config    Config
	wsURL     string
	conn      *websocket.Conn
	mu        sync.Mutex
	events    chan []byte
	closeCh   chan struct{}
}

func NewWSEventClient(config Config, wsURL string) *WSEventClient {
	return &WSEventClient{
		config:  config,
		wsURL:   wsURL,
		events:  make(chan []byte, 100),
		closeCh: make(chan struct{}),
	}
}

func (c *WSEventClient) Events() <-chan []byte {
	return c.events
}

func (c *WSEventClient) Start(ctx context.Context) error {
	u, err := url.Parse(c.wsURL)
	if err != nil {
		return err
	}

	if c.config.AccessToken != "" {
		q := u.Query()
		q.Set("access_token", c.config.AccessToken)
		u.RawQuery = q.Encode()
	}

	header := http.Header{}
	header.Set("X-Client-Role", "Event")
	if c.config.SelfID != 0 {
		header.Set("X-Self-ID", fmt.Sprintf("%d", c.config.SelfID))
	}

	conn, _, err := websocket.DefaultDialer.DialContext(ctx, u.String(), header)
	if err != nil {
		return fmt.Errorf("ws event dial: %w", err)
	}
	c.conn = conn
	slog.Info("WS event connected", "url", c.wsURL)

	go c.readLoop()
	return nil
}

func (c *WSEventClient) Stop(ctx context.Context) error {
	close(c.closeCh)
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}

func (c *WSEventClient) readLoop() {
	defer func() {
		c.mu.Lock()
		c.conn = nil
		c.mu.Unlock()
	}()
	for {
		_, msg, err := c.conn.ReadMessage()
		if err != nil {
			slog.Debug("WS event read error", "error", err)
			return
		}
		select {
		case c.events <- msg:
		default:
			slog.Warn("event channel full, dropping event")
		}
	}
}

// ==================== WS Universal Client ====================

// WSUniversalClient handles both actions and events over a single WS connection.
type WSUniversalClient struct {
	config    Config
	wsURL     string
	conn      *websocket.Conn
	mu        sync.Mutex
	pending   map[string]chan ActionResponse
	events    chan []byte
	closeCh   chan struct{}
}

func NewWSUniversalClient(config Config, wsURL string) *WSUniversalClient {
	return &WSUniversalClient{
		config:  config,
		wsURL:   wsURL,
		pending: make(map[string]chan ActionResponse),
		events:  make(chan []byte, 100),
		closeCh: make(chan struct{}),
	}
}

func (c *WSUniversalClient) Start(ctx context.Context) error {
	return c.connect(ctx)
}

func (c *WSUniversalClient) Stop(ctx context.Context) error {
	close(c.closeCh)
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}

func (c *WSUniversalClient) connect(ctx context.Context) error {
	u, err := url.Parse(c.wsURL)
	if err != nil {
		return err
	}

	if c.config.AccessToken != "" {
		q := u.Query()
		q.Set("access_token", c.config.AccessToken)
		u.RawQuery = q.Encode()
	}

	header := http.Header{}
	header.Set("X-Client-Role", "Universal")
	if c.config.SelfID != 0 {
		header.Set("X-Self-ID", fmt.Sprintf("%d", c.config.SelfID))
	}

	conn, _, err := websocket.DefaultDialer.DialContext(ctx, u.String(), header)
	if err != nil {
		return fmt.Errorf("ws universal dial: %w", err)
	}

	c.conn = conn
	slog.Info("WS universal connected", "url", c.wsURL)

	go c.readLoop()
	go c.heartbeat()
	go c.reconnectLoop(ctx)

	return nil
}

func (c *WSUniversalClient) readLoop() {
	defer func() {
		c.mu.Lock()
		c.conn = nil
		c.mu.Unlock()
	}()
	for {
		_, msg, err := c.conn.ReadMessage()
		if err != nil {
			slog.Debug("WS universal read error", "error", err)
			return
		}

		// Dispatch based on message content: events have post_type, responses have echo
		var discrim struct {
			PostType string `json:"post_type"`
			Echo     string `json:"echo"`
		}
		if err := json.Unmarshal(msg, &discrim); err != nil {
			slog.Error("Failed to parse WS message", "error", err)
			continue
		}

		if discrim.PostType != "" {
			// This is an event
			select {
			case c.events <- msg:
			default:
				slog.Warn("universal event channel full, dropping event")
			}
		} else if discrim.Echo != "" {
			// This is an action response
			c.mu.Lock()
			ch, ok := c.pending[discrim.Echo]
			if ok {
				delete(c.pending, discrim.Echo)
			}
			c.mu.Unlock()
			if ok {
				var resp ActionResponse
				if err := json.Unmarshal(msg, &resp); err != nil {
					slog.Error("Failed to parse WS response", "error", err)
					continue
				}
				ch <- resp
			}
		}
	}
}

func (c *WSUniversalClient) heartbeat() {
	interval := time.Duration(c.config.HeartbeatInterval) * time.Millisecond
	if interval == 0 {
		interval = 15 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for {
		select {
		case <-c.closeCh:
			return
		case <-ticker.C:
			c.mu.Lock()
			if c.conn != nil {
				c.conn.WriteMessage(websocket.PingMessage, nil)
			}
			c.mu.Unlock()
		}
	}
}

func (c *WSUniversalClient) reconnectLoop(ctx context.Context) {
	interval := time.Duration(c.config.ReconnectInterval) * time.Millisecond
	if interval == 0 {
		interval = 3 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for {
		select {
		case <-c.closeCh:
			return
		case <-ticker.C:
			c.mu.Lock()
			conn := c.conn
			c.mu.Unlock()
			if conn == nil {
				slog.Info("WS universal reconnecting", "url", c.wsURL)
				if err := c.connect(ctx); err != nil {
					slog.Warn("WS universal reconnect failed", "error", err)
				}
			}
		}
	}
}

func (c *WSUniversalClient) Call(ctx context.Context, req ActionRequest) (ActionResponse, error) {
	ch := make(chan ActionResponse, 1)

	c.mu.Lock()
	c.pending[req.Echo] = ch
	conn := c.conn
	c.mu.Unlock()

	payload := map[string]any{
		"action": req.Action,
		"params": req.Params,
		"echo":   req.Echo,
	}
	data, err := json.Marshal(payload)
	if err != nil {
		c.mu.Lock()
		delete(c.pending, req.Echo)
		c.mu.Unlock()
		return ActionResponse{}, err
	}

	c.mu.Lock()
	err = conn.WriteMessage(websocket.TextMessage, data)
	c.mu.Unlock()
	if err != nil {
		c.mu.Lock()
		delete(c.pending, req.Echo)
		c.mu.Unlock()
		return ActionResponse{}, fmt.Errorf("ws universal write: %w", err)
	}

	select {
	case <-ctx.Done():
		c.mu.Lock()
		delete(c.pending, req.Echo)
		c.mu.Unlock()
		return ActionResponse{}, ctx.Err()
	case resp := <-ch:
		return resp, nil
	}
}

func (c *WSUniversalClient) Events() <-chan []byte {
	return c.events
}
