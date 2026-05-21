package onebot

import "context"

// Plugin can be loaded into a Bot.
type Plugin interface {
	Name() string
	Version() string
	OnLoad(ctx context.Context, bot *Bot) error
	OnUnload(ctx context.Context, bot *Bot) error
}
