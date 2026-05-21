package onebot

import (
	"context"
	"fmt"
	"log/slog"
	"time"
)

// Connection manages the transport layer for the Bot.
type Connection struct {
	config         Config
	actionTransport ActionTransport
	eventTransport  interface {
		Events() <-chan []byte
		Start(ctx context.Context) error
		Stop(ctx context.Context) error
	}
	echoTracker    *EchoTracker
	running        bool
}

func NewConnection(config Config) *Connection {
	return &Connection{
		config:      config,
		echoTracker: NewEchoTracker(),
	}
}

func (c *Connection) SetActionTransport(t ActionTransport) {
	c.actionTransport = t
}

func (c *Connection) SetEventTransport(t interface {
	Events() <-chan []byte
	Start(ctx context.Context) error
	Stop(ctx context.Context) error
}) {
	c.eventTransport = t
}

// Start initializes transports.
func (c *Connection) Start(ctx context.Context) error {
	if c.actionTransport != nil {
		if err := c.actionTransport.Start(ctx); err != nil {
			return fmt.Errorf("start action transport: %w", err)
		}
	}
	if c.eventTransport != nil {
		if err := c.eventTransport.Start(ctx); err != nil {
			return fmt.Errorf("start event transport: %w", err)
		}
	}
	c.running = true
	slog.Info("connection started")
	return nil
}

// Stop closes transports and cancels pending requests.
func (c *Connection) Stop(ctx context.Context) error {
	c.running = false
	c.echoTracker.CancelAll()

	if c.actionTransport != nil {
		if err := c.actionTransport.Stop(ctx); err != nil {
			slog.Warn("error stopping action transport", "error", err)
		}
	}
	if c.eventTransport != nil {
		if err := c.eventTransport.Stop(ctx); err != nil {
			slog.Warn("error stopping event transport", "error", err)
		}
	}
	slog.Info("connection stopped")
	return nil
}

// Call sends an action and waits for the response.
func (c *Connection) Call(ctx context.Context, action string, params map[string]any) (ActionResponse, error) {
	if !c.running {
		return ActionResponse{}, fmt.Errorf("connection not started")
	}
	if c.actionTransport == nil {
		return ActionResponse{}, fmt.Errorf("no action transport configured")
	}

	echo := c.echoTracker.GenerateEcho()
	timeout := time.Duration(c.config.Timeout) * time.Millisecond

	req := ActionRequest{Action: action, Params: params, Echo: echo}

	// For HTTP transports, call synchronously
	_, isWS := c.actionTransport.(*WSActionClient); _, isUniversal := c.actionTransport.(*WSUniversalClient); if !isWS && !isUniversal {
		return c.actionTransport.Call(ctx, req)
	}

	// For WS, register echo and wait for async response
	respCh := c.echoTracker.Register(echo, timeout)
	if _, err := c.actionTransport.Call(ctx, req); err != nil {
		c.echoTracker.Resolve(echo, NewFailedResponse(-1, echo))
		return ActionResponse{}, err
	}

	select {
	case <-ctx.Done():
		return ActionResponse{}, ctx.Err()
	case resp := <-respCh:
		return resp, nil
	}
}

// Events returns the raw event channel from the event transport.
func (c *Connection) Events() <-chan []byte {
	if c.eventTransport != nil {
		return c.eventTransport.Events()
	}
	return nil
}

// BuildConnection creates the appropriate transports based on config.
func BuildConnection(config Config) (*Connection, error) {
	conn := NewConnection(config)

	// Action transport
	switch {
	case config.WSReverseURL != "" || config.WSReverseAPIURL != "":
		wsURL := config.WSReverseURL
		if wsURL == "" {
			wsURL = config.WSReverseAPIURL
		}
		if wsURL == "" {
			wsURL = "ws://127.0.0.1:8080/ws"
		}
		if config.WSReverseUseUniversal {
			universal := NewWSUniversalClient(config, wsURL)
			conn.SetActionTransport(universal)
			conn.SetEventTransport(universal)
		} else {
			wsClient := NewWSActionClient(config, wsURL, "API")
			conn.SetActionTransport(wsClient)
		}
	case config.WSHost != "":
		wsClient := NewWSActionClient(config, fmt.Sprintf("ws://%s:%d/api", config.WSHost, config.WSPort), "API")
		conn.SetActionTransport(wsClient)
	case config.HTTPHost != "":
		httpClient := NewHTTPActionClient(config)
		conn.SetActionTransport(httpClient)
	}

	// Event transport
	if config.HTTPPostURL != "" {
		// In HTTP POST mode, events are pushed by the implementation
		eventServer := NewHTTPEventServer(config)
		conn.SetEventTransport(eventServer)
	} else if config.WSReverseEventURL != "" {
		eventClient := NewWSEventClient(config, config.WSReverseEventURL)
		conn.SetEventTransport(eventClient)
	}

	return conn, nil
}
