package onebot

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"time"
)

// HTTPActionClient sends actions via HTTP POST/GET to an implementation.
type HTTPActionClient struct {
	config    Config
	client    *http.Client
	baseURL   string
}

func NewHTTPActionClient(config Config) *HTTPActionClient {
	baseURL := fmt.Sprintf("http://%s:%d", config.HTTPHost, config.HTTPPort)
	return &HTTPActionClient{
		config:  config,
		client:  &http.Client{Timeout: time.Duration(config.Timeout) * time.Millisecond},
		baseURL: baseURL,
	}
}

func (c *HTTPActionClient) Start(ctx context.Context) error {
	return nil
}

func (c *HTTPActionClient) Stop(ctx context.Context) error {
	return nil
}

func (c *HTTPActionClient) Call(ctx context.Context, req ActionRequest) (ActionResponse, error) {
	// Build URL with action name
	u, err := url.Parse(c.baseURL + "/" + req.Action)
	if err != nil {
		return ActionResponse{}, err
	}

	q := u.Query()
	if c.config.AccessToken != "" {
		q.Set("access_token", c.config.AccessToken)
	}
	if req.Echo != "" {
		q.Set("echo", req.Echo)
	}
	u.RawQuery = q.Encode()

	// Serialize params as JSON body
	bodyJSON, err := json.Marshal(req.Params)
	if err != nil {
		return ActionResponse{}, fmt.Errorf("marshal params: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, u.String(), bytes.NewReader(bodyJSON))
	if err != nil {
		return ActionResponse{}, err
	}
	httpReq.Header.Set("Content-Type", "application/json")
	httpReq.Header.Set("X-Self-ID", strconv.FormatInt(c.config.SelfID, 10))

	resp, err := c.client.Do(httpReq)
	if err != nil {
		return ActionResponse{}, fmt.Errorf("http call: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return ActionResponse{}, fmt.Errorf("read response: %w", err)
	}

	var ar ActionResponse
	if err := json.Unmarshal(body, &ar); err != nil {
		return ActionResponse{}, fmt.Errorf("unmarshal response: %w", err)
	}
	if ar.Echo == "" {
		ar.Echo = req.Echo
	}
	return ar, nil
}
