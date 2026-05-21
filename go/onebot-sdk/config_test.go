package onebot

import (
	"testing"
)

func TestDefaultConfig_HostDefaults(t *testing.T) {
	cfg := DefaultConfig()

	if cfg.HTTPHost != "0.0.0.0" {
		t.Errorf("expected HTTPHost '0.0.0.0', got %q", cfg.HTTPHost)
	}
	if cfg.WSHost != "0.0.0.0" {
		t.Errorf("expected WSHost '0.0.0.0', got %q", cfg.WSHost)
	}
}

func TestDefaultConfig_PortDefaults(t *testing.T) {
	cfg := DefaultConfig()

	if cfg.HTTPPort != 5700 {
		t.Errorf("expected HTTPPort 5700, got %d", cfg.HTTPPort)
	}
	if cfg.WSPort != 6700 {
		t.Errorf("expected WSPort 6700, got %d", cfg.WSPort)
	}
}

func TestDefaultConfig_TimeoutDefaults(t *testing.T) {
	cfg := DefaultConfig()

	if cfg.Timeout != 30000 {
		t.Errorf("expected Timeout 30000, got %d", cfg.Timeout)
	}
	if cfg.ReconnectInterval != 3000 {
		t.Errorf("expected ReconnectInterval 3000, got %d", cfg.ReconnectInterval)
	}
	if cfg.HeartbeatInterval != 15000 {
		t.Errorf("expected HeartbeatInterval 15000, got %d", cfg.HeartbeatInterval)
	}
}

func TestDefaultConfig_ZeroValueDefaults(t *testing.T) {
	cfg := DefaultConfig()

	// These should default to zero values (empty string or 0)
	if cfg.AccessToken != "" {
		t.Errorf("expected AccessToken to be empty by default, got %q", cfg.AccessToken)
	}
	if cfg.Secret != "" {
		t.Errorf("expected Secret to be empty by default, got %q", cfg.Secret)
	}
	if cfg.SelfID != 0 {
		t.Errorf("expected SelfID to be 0 by default, got %d", cfg.SelfID)
	}
	if cfg.RateLimitInterval != 0 {
		t.Errorf("expected RateLimitInterval to be 0 by default, got %d", cfg.RateLimitInterval)
	}
}

func TestConfig_FieldAssignment(t *testing.T) {
	cfg := Config{
		HTTPHost:          "127.0.0.1",
		HTTPPort:          8080,
		WSHost:            "127.0.0.1",
		WSPort:            8081,
		Timeout:           5000,
		ReconnectInterval: 1000,
		HeartbeatInterval: 5000,
		AccessToken:       "test-token",
		Secret:            "test-secret",
		SelfID:            10001,
		AppName:           "test-app",
		AppVersion:        "1.0.0",
	}

	if cfg.HTTPHost != "127.0.0.1" {
		t.Errorf("expected HTTPHost '127.0.0.1', got %q", cfg.HTTPHost)
	}
	if cfg.HTTPPort != 8080 {
		t.Errorf("expected HTTPPort 8080, got %d", cfg.HTTPPort)
	}
	if cfg.AccessToken != "test-token" {
		t.Errorf("expected AccessToken 'test-token', got %q", cfg.AccessToken)
	}
	if cfg.SelfID != 10001 {
		t.Errorf("expected SelfID 10001, got %d", cfg.SelfID)
	}
	if cfg.AppName != "test-app" {
		t.Errorf("expected AppName 'test-app', got %q", cfg.AppName)
	}
	if cfg.AppVersion != "1.0.0" {
		t.Errorf("expected AppVersion '1.0.0', got %q", cfg.AppVersion)
	}
}

func TestConfig_WebSocketReverseDefaults(t *testing.T) {
	cfg := DefaultConfig()

	if cfg.WSReverseUseUniversal != false {
		t.Errorf("expected WSReverseUseUniversal to be false by default")
	}
	if cfg.WSReverseURL != "" {
		t.Errorf("expected WSReverseURL to be empty by default, got %q", cfg.WSReverseURL)
	}
	if cfg.WSReverseAPIURL != "" {
		t.Errorf("expected WSReverseAPIURL to be empty by default, got %q", cfg.WSReverseAPIURL)
	}
	if cfg.WSReverseEventURL != "" {
		t.Errorf("expected WSReverseEventURL to be empty by default, got %q", cfg.WSReverseEventURL)
	}
}

func TestConfig_HTTPPostDefaults(t *testing.T) {
	cfg := DefaultConfig()

	if cfg.HTTPPostURL != "" {
		t.Errorf("expected HTTPPostURL to be empty by default, got %q", cfg.HTTPPostURL)
	}
	if cfg.HTTPPostHost != "" {
		t.Errorf("expected HTTPPostHost to be empty by default, got %q", cfg.HTTPPostHost)
	}
	if cfg.HTTPPostPort != 0 {
		t.Errorf("expected HTTPPostPort to be 0 by default, got %d", cfg.HTTPPostPort)
	}
}
