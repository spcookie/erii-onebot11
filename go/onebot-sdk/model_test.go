package onebot

import (
	"encoding/json"
	"testing"
)

func TestParseEvent_PrivateMessageEvent(t *testing.T) {
	data := []byte(`{
		"time": 1234567890,
		"self_id": 10001,
		"post_type": "message",
		"message_type": "private",
		"sub_type": "friend",
		"message_id": 12345,
		"user_id": 10002,
		"message": [{"type": "text", "data": {"text": "Hello"}}],
		"raw_message": "Hello",
		"font": 0,
		"sender": {"user_id": 10002, "nickname": "TestUser", "sex": "male", "age": 20}
	}`)

	event, err := ParseEvent(data)
	if err != nil {
		t.Fatalf("ParseEvent failed: %v", err)
	}

	priv, ok := event.(PrivateMessageEvent)
	if !ok {
		t.Fatalf("expected PrivateMessageEvent, got %T", event)
	}

	if priv.GetPostType() != "message" {
		t.Errorf("expected post_type 'message', got %q", priv.GetPostType())
	}
	if priv.GetTime() != 1234567890 {
		t.Errorf("expected time 1234567890, got %d", priv.GetTime())
	}
	if priv.GetSelfID() != 10001 {
		t.Errorf("expected self_id 10001, got %d", priv.GetSelfID())
	}
	if priv.UserID != 10002 {
		t.Errorf("expected user_id 10002, got %d", priv.UserID)
	}
	if priv.MessageType != "private" {
		t.Errorf("expected message_type 'private', got %q", priv.MessageType)
	}
	if priv.SubType != "friend" {
		t.Errorf("expected sub_type 'friend', got %q", priv.SubType)
	}
	if priv.RawMessage != "Hello" {
		t.Errorf("expected raw_message 'Hello', got %q", priv.RawMessage)
	}
	if priv.Sender.UserID != 10002 {
		t.Errorf("expected sender user_id 10002, got %d", priv.Sender.UserID)
	}
	if priv.Sender.Nickname != "TestUser" {
		t.Errorf("expected sender nickname 'TestUser', got %q", priv.Sender.Nickname)
	}
}

func TestParseEvent_GroupMessageEvent(t *testing.T) {
	data := []byte(`{
		"time": 1234567890,
		"self_id": 10001,
		"post_type": "message",
		"message_type": "group",
		"sub_type": "normal",
		"message_id": 12345,
		"group_id": 10010,
		"user_id": 10002,
		"message": [{"type": "text", "data": {"text": "Hello Group"}}],
		"raw_message": "Hello Group",
		"font": 0,
		"sender": {
			"user_id": 10002,
			"nickname": "TestUser",
			"card": "CardName",
			"sex": "male",
			"age": 20,
			"area": "Earth",
			"level": "1",
			"role": "member",
			"title": "Title"
		}
	}`)

	event, err := ParseEvent(data)
	if err != nil {
		t.Fatalf("ParseEvent failed: %v", err)
	}

	group, ok := event.(GroupMessageEvent)
	if !ok {
		t.Fatalf("expected GroupMessageEvent, got %T", event)
	}

	if group.GetPostType() != "message" {
		t.Errorf("expected post_type 'message', got %q", group.GetPostType())
	}
	if group.GroupID != 10010 {
		t.Errorf("expected group_id 10010, got %d", group.GroupID)
	}
	if group.Sender.Card != "CardName" {
		t.Errorf("expected card 'CardName', got %q", group.Sender.Card)
	}
	if group.Sender.Role != "member" {
		t.Errorf("expected role 'member', got %q", group.Sender.Role)
	}
}

func TestParseEvent_NoticeEvents(t *testing.T) {
	tests := []struct {
		name     string
		json     string
		wantType string // notice_type field value
	}{
		{
			name: "GroupUpload",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "group_upload",
				"group_id": 10010,
				"user_id": 10002,
				"file": {"id": "file123", "name": "test.txt", "size": 1024, "busid": 1}
			}`,
			wantType: "group_upload",
		},
		{
			name: "GroupAdmin",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "group_admin",
				"sub_type": "set",
				"group_id": 10010,
				"user_id": 10002
			}`,
			wantType: "group_admin",
		},
		{
			name: "GroupDecrease",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "group_decrease",
				"sub_type": "leave",
				"group_id": 10010,
				"user_id": 10002,
				"operator_id": 10003
			}`,
			wantType: "group_decrease",
		},
		{
			name: "GroupIncrease",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "group_increase",
				"sub_type": "approve",
				"group_id": 10010,
				"user_id": 10002,
				"operator_id": 10003
			}`,
			wantType: "group_increase",
		},
		{
			name: "GroupBan",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "group_ban",
				"sub_type": "ban",
				"group_id": 10010,
				"user_id": 10002,
				"operator_id": 10003,
				"duration": 3600
			}`,
			wantType: "group_ban",
		},
		{
			name: "FriendAdd",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "friend_add",
				"user_id": 10002
			}`,
			wantType: "friend_add",
		},
		{
			name: "GroupRecall",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "group_recall",
				"group_id": 10010,
				"user_id": 10002,
				"operator_id": 10003,
				"message_id": 12345
			}`,
			wantType: "group_recall",
		},
		{
			name: "FriendRecall",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "friend_recall",
				"user_id": 10002,
				"message_id": 12345
			}`,
			wantType: "friend_recall",
		},
		{
			name: "Poke",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "notify",
				"sub_type": "poke",
				"group_id": 10010,
				"user_id": 10002,
				"target_id": 10001
			}`,
			wantType: "notify",
		},
		{
			name: "LuckyKing",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "notify",
				"sub_type": "lucky_king",
				"group_id": 10010,
				"user_id": 10002,
				"target_id": 10001
			}`,
			wantType: "notify",
		},
		{
			name: "Honor",
			json: `{
				"time": 1234567890,
				"self_id": 10001,
				"post_type": "notice",
				"notice_type": "notify",
				"sub_type": "honor",
				"group_id": 10010,
				"honor_type": "talkative",
				"user_id": 10002
			}`,
			wantType: "notify",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			event, err := ParseEvent([]byte(tt.json))
			if err != nil {
				t.Fatalf("ParseEvent failed: %v", err)
			}
			if event.GetPostType() != "notice" {
				t.Errorf("expected post_type 'notice', got %q", event.GetPostType())
			}
		})
	}
}

func TestParseEvent_NoticeEventFields(t *testing.T) {
	t.Run("GroupUpload file info", func(t *testing.T) {
		data := []byte(`{
			"time": 1234567890, "self_id": 10001,
			"post_type": "notice", "notice_type": "group_upload",
			"group_id": 10010, "user_id": 10002,
			"file": {"id": "file123", "name": "test.txt", "size": 1024, "busid": 1}
		}`)
		event, err := ParseEvent(data)
		if err != nil {
			t.Fatalf("ParseEvent failed: %v", err)
		}
		evt := event.(GroupUploadEvent)
		if evt.File.ID != "file123" {
			t.Errorf("expected file id 'file123', got %q", evt.File.ID)
		}
		if evt.File.Size != 1024 {
			t.Errorf("expected file size 1024, got %d", evt.File.Size)
		}
	})

	t.Run("GroupAdmin sub_type", func(t *testing.T) {
		data := []byte(`{
			"time": 1234567890, "self_id": 10001,
			"post_type": "notice", "notice_type": "group_admin",
			"sub_type": "unset", "group_id": 10010, "user_id": 10002
		}`)
		event, err := ParseEvent(data)
		if err != nil {
			t.Fatalf("ParseEvent failed: %v", err)
		}
		evt := event.(GroupAdminEvent)
		if evt.SubType != "unset" {
			t.Errorf("expected sub_type 'unset', got %q", evt.SubType)
		}
	})

	t.Run("GroupBan duration", func(t *testing.T) {
		data := []byte(`{
			"time": 1234567890, "self_id": 10001,
			"post_type": "notice", "notice_type": "group_ban",
			"sub_type": "ban", "group_id": 10010, "user_id": 10002,
			"operator_id": 10003, "duration": 3600
		}`)
		event, err := ParseEvent(data)
		if err != nil {
			t.Fatalf("ParseEvent failed: %v", err)
		}
		evt := event.(GroupBanEvent)
		if evt.Duration != 3600 {
			t.Errorf("expected duration 3600, got %d", evt.Duration)
		}
	})
}

func TestParseEvent_RequestEvents(t *testing.T) {
	t.Run("FriendRequest", func(t *testing.T) {
		data := []byte(`{
			"time": 1234567890,
			"self_id": 10001,
			"post_type": "request",
			"request_type": "friend",
			"comment": "hello",
			"flag": "abc123",
			"user_id": 10002
		}`)
		event, err := ParseEvent(data)
		if err != nil {
			t.Fatalf("ParseEvent failed: %v", err)
		}
		evt, ok := event.(FriendRequestEvent)
		if !ok {
			t.Fatalf("expected FriendRequestEvent, got %T", event)
		}
		if evt.GetPostType() != "request" {
			t.Errorf("expected post_type 'request', got %q", evt.GetPostType())
		}
		if evt.UserID != 10002 {
			t.Errorf("expected user_id 10002, got %d", evt.UserID)
		}
		if evt.Comment != "hello" {
			t.Errorf("expected comment 'hello', got %q", evt.Comment)
		}
	})

	t.Run("GroupRequest", func(t *testing.T) {
		data := []byte(`{
			"time": 1234567890,
			"self_id": 10001,
			"post_type": "request",
			"request_type": "group",
			"sub_type": "add",
			"comment": "please let me in",
			"flag": "abc123",
			"group_id": 10010,
			"user_id": 10002
		}`)
		event, err := ParseEvent(data)
		if err != nil {
			t.Fatalf("ParseEvent failed: %v", err)
		}
		evt, ok := event.(GroupRequestEvent)
		if !ok {
			t.Fatalf("expected GroupRequestEvent, got %T", event)
		}
		if evt.GetPostType() != "request" {
			t.Errorf("expected post_type 'request', got %q", evt.GetPostType())
		}
		if evt.GroupID != 10010 {
			t.Errorf("expected group_id 10010, got %d", evt.GroupID)
		}
		if evt.SubType != "add" {
			t.Errorf("expected sub_type 'add', got %q", evt.SubType)
		}
	})
}

func TestParseEvent_MetaEvents(t *testing.T) {
	t.Run("Lifecycle", func(t *testing.T) {
		data := []byte(`{
			"time": 1234567890,
			"self_id": 10001,
			"post_type": "meta_event",
			"meta_event_type": "lifecycle",
			"sub_type": "enable"
		}`)
		event, err := ParseEvent(data)
		if err != nil {
			t.Fatalf("ParseEvent failed: %v", err)
		}
		evt, ok := event.(LifecycleMetaEvent)
		if !ok {
			t.Fatalf("expected LifecycleMetaEvent, got %T", event)
		}
		if evt.GetPostType() != "meta_event" {
			t.Errorf("expected post_type 'meta_event', got %q", evt.GetPostType())
		}
		if evt.SubType != "enable" {
			t.Errorf("expected sub_type 'enable', got %q", evt.SubType)
		}
	})

	t.Run("Heartbeat", func(t *testing.T) {
		data := []byte(`{
			"time": 1234567890,
			"self_id": 10001,
			"post_type": "meta_event",
			"meta_event_type": "heartbeat",
			"status": {"online": true, "good": true},
			"interval": 5000
		}`)
		event, err := ParseEvent(data)
		if err != nil {
			t.Fatalf("ParseEvent failed: %v", err)
		}
		evt, ok := event.(HeartbeatMetaEvent)
		if !ok {
			t.Fatalf("expected HeartbeatMetaEvent, got %T", event)
		}
		if evt.GetPostType() != "meta_event" {
			t.Errorf("expected post_type 'meta_event', got %q", evt.GetPostType())
		}
		if evt.Interval != 5000 {
			t.Errorf("expected interval 5000, got %d", evt.Interval)
		}
		if !evt.Status.Online {
			t.Error("expected status online to be true")
		}
		if !evt.Status.Good {
			t.Error("expected status good to be true")
		}
	})
}

func TestParseEvent_UnknownPostType(t *testing.T) {
	data := []byte(`{
		"time": 1234567890,
		"self_id": 10001,
		"post_type": "unknown_type"
	}`)
	event, err := ParseEvent(data)
	if err == nil {
		t.Errorf("expected error for unknown post_type, got event %v", event)
	}
}

func TestParseEvent_InvalidJSON(t *testing.T) {
	data := []byte(`not json`)
	_, err := ParseEvent(data)
	if err == nil {
		t.Error("expected error for invalid JSON")
	}
}

func TestMessageSegment_FactoryFunctions(t *testing.T) {
	tests := []struct {
		name     string
		segment  MessageSegment
		wantType string
		wantData map[string]string
	}{
		{
			name:     "TextSegment",
			segment:  TextSegment("hello"),
			wantType: "text",
			wantData: map[string]string{"text": "hello"},
		},
		{
			name:     "ImageSegment",
			segment:  ImageSegment("http://example.com/img.jpg"),
			wantType: "image",
			wantData: map[string]string{"file": "http://example.com/img.jpg"},
		},
		{
			name:     "AtSegment",
			segment:  AtSegment("12345"),
			wantType: "at",
			wantData: map[string]string{"qq": "12345"},
		},
		{
			name:     "FaceSegment",
			segment:  FaceSegment("1"),
			wantType: "face",
			wantData: map[string]string{"id": "1"},
		},
		{
			name:     "ReplySegment",
			segment:  ReplySegment("12345"),
			wantType: "reply",
			wantData: map[string]string{"id": "12345"},
		},
		{
			name:     "RecordSegment",
			segment:  RecordSegment("audio.mp3"),
			wantType: "record",
			wantData: map[string]string{"file": "audio.mp3"},
		},
		{
			name:     "VideoSegment",
			segment:  VideoSegment("video.mp4"),
			wantType: "video",
			wantData: map[string]string{"file": "video.mp4"},
		},
		{
			name:     "RpsSegment",
			segment:  RpsSegment(),
			wantType: "rps",
			wantData: map[string]string{},
		},
		{
			name:     "DiceSegment",
			segment:  DiceSegment(),
			wantType: "dice",
			wantData: map[string]string{},
		},
		{
			name:     "ShakeSegment",
			segment:  ShakeSegment(),
			wantType: "shake",
			wantData: map[string]string{},
		},
		{
			name:     "PokeSegment",
			segment:  PokeSegment(),
			wantType: "poke",
			wantData: map[string]string{},
		},
		{
			name:     "AnonymousSegment",
			segment:  AnonymousSegment(),
			wantType: "anonymous",
			wantData: map[string]string{},
		},
		{
			name:     "ContactSegment",
			segment:  ContactSegment("10001"),
			wantType: "contact",
			wantData: map[string]string{"id": "10001"},
		},
		{
			name:     "XMLSegment",
			segment:  XMLSegment("<xml></xml>"),
			wantType: "xml",
			wantData: map[string]string{"data": "<xml></xml>"},
		},
		{
			name:     "JSONSegment",
			segment:  JSONSegment(`{"key":"value"}`),
			wantType: "json",
			wantData: map[string]string{"data": `{"key":"value"}`},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if tt.segment.Type != tt.wantType {
				t.Errorf("expected type %q, got %q", tt.wantType, tt.segment.Type)
			}
			if !mapEqual(tt.segment.Data, tt.wantData) {
				t.Errorf("expected data %v, got %v", tt.wantData, tt.segment.Data)
			}
		})
	}
}

func TestMessageSegment_ComplexFactoryFunctions(t *testing.T) {
	t.Run("ShareSegment", func(t *testing.T) {
		seg := ShareSegment("http://example.com", "Title", "Content", "http://example.com/img.jpg")
		if seg.Type != "share" {
			t.Errorf("expected type 'share', got %q", seg.Type)
		}
		if seg.Data["url"] != "http://example.com" {
			t.Errorf("expected url field")
		}
		if seg.Data["title"] != "Title" {
			t.Errorf("expected title field")
		}
		if seg.Data["content"] != "Content" {
			t.Errorf("expected content field")
		}
		if seg.Data["image"] != "http://example.com/img.jpg" {
			t.Errorf("expected image field")
		}
	})

	t.Run("LocationSegment", func(t *testing.T) {
		seg := LocationSegment("30.0", "120.0", "Test", "Test Content")
		if seg.Type != "location" {
			t.Errorf("expected type 'location', got %q", seg.Type)
		}
		if seg.Data["lat"] != "30.0" {
			t.Errorf("expected lat field")
		}
		if seg.Data["lon"] != "120.0" {
			t.Errorf("expected lon field")
		}
	})

	t.Run("MusicSegment", func(t *testing.T) {
		seg := MusicSegment("qq", "123", "http://url", "http://audio", "Music", "Content", "http://img")
		if seg.Type != "music" {
			t.Errorf("expected type 'music', got %q", seg.Type)
		}
		if seg.Data["type"] != "qq" {
			t.Errorf("expected type field")
		}
		if seg.Data["id"] != "123" {
			t.Errorf("expected id field")
		}
	})

	t.Run("NodeSegment", func(t *testing.T) {
		seg := NodeSegment("10001", "Nick", "[CQ:at,qq=123]")
		if seg.Type != "node" {
			t.Errorf("expected type 'node', got %q", seg.Type)
		}
		if seg.Data["user_id"] != "10001" {
			t.Errorf("expected user_id field")
		}
		if seg.Data["nickname"] != "Nick" {
			t.Errorf("expected nickname field")
		}
		if seg.Data["content"] != "[CQ:at,qq=123]" {
			t.Errorf("expected content field")
		}
	})
}

func TestParseEvent_GroupMessageEventWithAnonymous(t *testing.T) {
	data := []byte(`{
		"time": 1234567890,
		"self_id": 10001,
		"post_type": "message",
		"message_type": "group",
		"sub_type": "normal",
		"message_id": 12345,
		"group_id": 10010,
		"user_id": 0,
		"anonymous": {"id": 999, "name": "Anon", "flag": "flag123"},
		"message": [{"type": "text", "data": {"text": "Anon message"}}],
		"raw_message": "Anon message",
		"font": 0,
		"sender": {
			"user_id": 0,
			"nickname": "Anon",
			"card": "",
			"sex": "unknown",
			"age": 0,
			"area": "",
			"level": "",
			"role": "",
			"title": ""
		}
	}`)

	event, err := ParseEvent(data)
	if err != nil {
		t.Fatalf("ParseEvent failed: %v", err)
	}
	evt := event.(GroupMessageEvent)
	if evt.Anonymous == nil {
		t.Fatal("expected anonymous to be non-nil")
	}
	if evt.Anonymous.ID != 999 {
		t.Errorf("expected anonymous id 999, got %d", evt.Anonymous.ID)
	}
	if evt.Anonymous.Name != "Anon" {
		t.Errorf("expected anonymous name 'Anon', got %q", evt.Anonymous.Name)
	}
}

func TestParseEvent_MessageContent(t *testing.T) {
	// Verify message segments are parsed into MessageContent type
	data := []byte(`{
		"time": 1234567890,
		"self_id": 10001,
		"post_type": "message",
		"message_type": "group",
		"sub_type": "normal",
		"message_id": 12345,
		"group_id": 10010,
		"user_id": 10002,
		"message": [
			{"type": "text", "data": {"text": "Hello "}},
			{"type": "at", "data": {"qq": "12345"}},
			{"type": "text", "data": {"text": " !"}}
		],
		"raw_message": "Hello [CQ:at,qq=12345] !",
		"font": 0,
		"sender": {
			"user_id": 10002, "nickname": "Test", "card": "",
			"sex": "unknown", "age": 0, "area": "", "level": "", "role": "", "title": ""
		}
	}`)

	event, err := ParseEvent(data)
	if err != nil {
		t.Fatalf("ParseEvent failed: %v", err)
	}
	evt := event.(GroupMessageEvent)
	if len(evt.Message) != 3 {
		t.Fatalf("expected 3 message segments, got %d", len(evt.Message))
	}
	if evt.Message[0].Type != "text" || evt.Message[0].Data["text"] != "Hello " {
		t.Errorf("segment 0 mismatch")
	}
	if evt.Message[1].Type != "at" || evt.Message[1].Data["qq"] != "12345" {
		t.Errorf("segment 1 mismatch")
	}
}

func TestActionResponseHelpers(t *testing.T) {
	resp := NewOKResponse(map[string]any{"message_id": 123}, "echo-1")
	if resp.Status != "ok" {
		t.Errorf("expected status 'ok', got %q", resp.Status)
	}
	if resp.RetCode != 0 {
		t.Errorf("expected retcode 0, got %d", resp.RetCode)
	}
	if resp.Echo != "echo-1" {
		t.Errorf("expected echo 'echo-1', got %q", resp.Echo)
	}

	resp2 := NewFailedResponse(404, "echo-2")
	if resp2.Status != "failed" {
		t.Errorf("expected status 'failed', got %q", resp2.Status)
	}
	if resp2.RetCode != 404 {
		t.Errorf("expected retcode 404, got %d", resp2.RetCode)
	}

	// Verify JSON serialization
	data, _ := json.Marshal(resp2)
	if string(data) == "" {
		t.Error("expected non-empty JSON")
	}
}

func mapEqual(a, b map[string]string) bool {
	if len(a) != len(b) {
		return false
	}
	for k, v := range a {
		if b[k] != v {
			return false
		}
	}
	return true
}
