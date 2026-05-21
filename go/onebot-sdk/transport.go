package onebot

import "context"

// ActionTransport sends API actions and receives responses.
type ActionTransport interface {
	// Call sends an action and returns the response.
	Call(ctx context.Context, req ActionRequest) (ActionResponse, error)
	// Start initializes the transport.
	Start(ctx context.Context) error
	// Stop closes the transport.
	Stop(ctx context.Context) error
}

// EventTransport receives events from the implementation.
type EventTransport interface {
	// Events returns a channel that receives raw event JSON.
	Events() <-chan []byte
	// Start initializes the transport.
	Start(ctx context.Context) error
	// Stop closes the transport.
	Stop(ctx context.Context) error
}

// EventPushTransport pushes events to the application side.
type EventPushTransport interface {
	// PushEvent pushes a raw event JSON payload.
	PushEvent(ctx context.Context, data []byte) error
	// Start initializes the transport.
	Start(ctx context.Context) error
	// Stop closes the transport.
	Stop(ctx context.Context) error
}
