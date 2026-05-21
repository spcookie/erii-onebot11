package onebot

// Config holds all configuration for a OneBot connection.
type Config struct {
	// HTTP server mode
	HTTPHost string
	HTTPPort int

	// HTTP POST mode
	HTTPPostURL string
	HTTPPostHost string
	HTTPPostPort int

	// WebSocket mode
	WSHost string
	WSPort int

	// Reverse WebSocket mode
	WSReverseURL           string
	WSReverseAPIURL        string
	WSReverseEventURL      string
	WSReverseUseUniversal  bool

	// Authentication
	AccessToken string
	Secret      string

	// Identity
	SelfID  int64
	AppName string
	AppVersion string

	// Connection
	Timeout           int // milliseconds
	ReconnectInterval int // milliseconds
	HeartbeatInterval int // milliseconds
	RateLimitInterval int // milliseconds
}

// DefaultConfig returns a Config with sensible defaults.
func DefaultConfig() Config {
	return Config{
		HTTPHost:          "0.0.0.0",
		HTTPPort:          5700,
		WSHost:            "0.0.0.0",
		WSPort:            6700,
		Timeout:           30000,
		ReconnectInterval: 3000,
		HeartbeatInterval: 15000,
	}
}
