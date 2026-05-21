package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"gopkg.in/yaml.v3"

	onebot "github.com/erii/onebot-sdk"
)

type AppConfig struct {
	ReverseWS struct {
		URL          string `yaml:"url"`
		UseUniversal bool   `yaml:"use_universal"`
	} `yaml:"reverse_ws"`
	AccessToken string `yaml:"access_token"`
	Secret      string `yaml:"secret"`
	Timeout     int    `yaml:"timeout"`
	SelfID      int64  `yaml:"self_id"`
}

func main() {
	slog.SetLogLoggerLevel(slog.LevelDebug)
	logger := slog.Default().With("component", "onebot-app")
	logger.Info("starting Go OneBot application")

	// Load config
	cfg, err := loadConfig("config.yaml")
	if err != nil {
		logger.Error("failed to load config", "error", err)
		os.Exit(1)
	}

	// Build bot config
	botCfg := onebot.DefaultConfig()
	botCfg.WSReverseURL = cfg.ReverseWS.URL
	botCfg.WSReverseUseUniversal = cfg.ReverseWS.UseUniversal
	botCfg.AccessToken = cfg.AccessToken
	botCfg.Secret = cfg.Secret
	botCfg.Timeout = cfg.Timeout
	botCfg.SelfID = cfg.SelfID

	bot, err := onebot.NewBot(botCfg)
	if err != nil {
		logger.Error("failed to create bot", "error", err)
		os.Exit(1)
	}

	// Register built-in middleware
	bot.Use(&onebot.LoggingMiddleware{})

	// Register event handlers
	registerHandlers(bot, logger)

	// Start bot
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	if err := bot.Start(ctx); err != nil {
		logger.Error("failed to start bot", "error", err)
		os.Exit(1)
	}

	logger.Info("bot is running, press Ctrl+C to stop")

	// Wait for shutdown signal
	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	<-sigCh

	logger.Info("shutting down...")
	if err := bot.Stop(ctx); err != nil {
		logger.Error("error during shutdown", "error", err)
	}
}

func loadConfig(path string) (*AppConfig, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config: %w", err)
	}

	var cfg AppConfig
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, fmt.Errorf("parse config: %w", err)
	}

	if cfg.Timeout == 0 {
		cfg.Timeout = 30000
	}
	return &cfg, nil
}
