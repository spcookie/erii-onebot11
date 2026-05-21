package onebot

import (
	"encoding/json"
	"fmt"
)

// ==================== Message Segment ====================

// MessageSegment represents a single segment in a message chain.
type MessageSegment struct {
	Type string            `json:"type"`
	Data map[string]string `json:"data"`
}

// MessageContent is a list of message segments.
type MessageContent []MessageSegment

// Helper functions for creating message segments.

func TextSegment(text string) MessageSegment {
	return MessageSegment{Type: "text", Data: map[string]string{"text": text}}
}

func ImageSegment(file string) MessageSegment {
	return MessageSegment{Type: "image", Data: map[string]string{"file": file}}
}

func AtSegment(qq string) MessageSegment {
	return MessageSegment{Type: "at", Data: map[string]string{"qq": qq}}
}

func FaceSegment(id string) MessageSegment {
	return MessageSegment{Type: "face", Data: map[string]string{"id": id}}
}

func ReplySegment(id string) MessageSegment {
	return MessageSegment{Type: "reply", Data: map[string]string{"id": id}}
}

func RecordSegment(file string) MessageSegment {
	return MessageSegment{Type: "record", Data: map[string]string{"file": file}}
}

func VideoSegment(file string) MessageSegment {
	return MessageSegment{Type: "video", Data: map[string]string{"file": file}}
}

func RpsSegment() MessageSegment {
	return MessageSegment{Type: "rps", Data: map[string]string{}}
}

func DiceSegment() MessageSegment {
	return MessageSegment{Type: "dice", Data: map[string]string{}}
}

func ShakeSegment() MessageSegment {
	return MessageSegment{Type: "shake", Data: map[string]string{}}
}

func PokeSegment() MessageSegment {
	return MessageSegment{Type: "poke", Data: map[string]string{}}
}

func AnonymousSegment() MessageSegment {
	return MessageSegment{Type: "anonymous", Data: map[string]string{}}
}

func ShareSegment(url, title, content, image string) MessageSegment {
	return MessageSegment{Type: "share", Data: map[string]string{
		"url": url, "title": title, "content": content, "image": image,
	}}
}

func ContactSegment(userID string) MessageSegment {
	return MessageSegment{Type: "contact", Data: map[string]string{"id": userID}}
}

func LocationSegment(lat, lon, title, content string) MessageSegment {
	return MessageSegment{Type: "location", Data: map[string]string{
		"lat": lat, "lon": lon, "title": title, "content": content,
	}}
}

func MusicSegment(musicType, id, url, audio, title, content, image string) MessageSegment {
	return MessageSegment{Type: "music", Data: map[string]string{
		"type": musicType, "id": id, "url": url, "audio": audio,
		"title": title, "content": content, "image": image,
	}}
}

func NodeSegment(userID, nickname, content string) MessageSegment {
	return MessageSegment{Type: "node", Data: map[string]string{
		"user_id": userID, "nickname": nickname, "content": content,
	}}
}

func XMLSegment(data string) MessageSegment {
	return MessageSegment{Type: "xml", Data: map[string]string{"data": data}}
}

func JSONSegment(data string) MessageSegment {
	return MessageSegment{Type: "json", Data: map[string]string{"data": data}}
}

// ==================== Common Types ====================

type Sender struct {
	UserID   int64  `json:"user_id"`
	Nickname string `json:"nickname"`
	Sex      string `json:"sex"`
	Age      int32  `json:"age"`
}

type GroupSender struct {
	UserID   int64  `json:"user_id"`
	Nickname string `json:"nickname"`
	Card     string `json:"card"`
	Sex      string `json:"sex"`
	Age      int32  `json:"age"`
	Area     string `json:"area"`
	Level    string `json:"level"`
	Role     string `json:"role"`
	Title    string `json:"title"`
}

type Anonymous struct {
	ID   int64  `json:"id"`
	Name string `json:"name"`
	Flag string `json:"flag"`
}

type FileInfo struct {
	ID    string `json:"id"`
	Name  string `json:"name"`
	Size  int64  `json:"size"`
	BusID int64  `json:"busid"`
}

type GroupInfo struct {
	GroupID        int64  `json:"group_id"`
	GroupName      string `json:"group_name"`
	MemberCount    int    `json:"member_count"`
	MaxMemberCount int    `json:"max_member_count"`
}

type GroupMemberInfo struct {
	GroupID         int64  `json:"group_id"`
	UserID          int64  `json:"user_id"`
	Nickname        string `json:"nickname"`
	Card            string `json:"card"`
	Sex             string `json:"sex"`
	Age             int32  `json:"age"`
	Area            string `json:"area"`
	JoinTime        int64  `json:"join_time"`
	LastSentTime    int64  `json:"last_sent_time"`
	Level           string `json:"level"`
	Role            string `json:"role"`
	Title           string `json:"title"`
	TitleExpireTime int64  `json:"title_expire_time"`
	CardChangeable  bool   `json:"card_changeable"`
	Unfriendly      bool   `json:"unfriendly"`
}

type FriendInfo struct {
	UserID   int64  `json:"user_id"`
	Nickname string `json:"nickname"`
	Remark   string `json:"remark"`
}

type StrangerInfo struct {
	UserID   int64  `json:"user_id"`
	Nickname string `json:"nickname"`
	Sex      string `json:"sex"`
	Age      int32  `json:"age"`
}

type LoginInfo struct {
	UserID   int64  `json:"user_id"`
	Nickname string `json:"nickname"`
}

type VersionInfo struct {
	AppName         string `json:"app_name"`
	AppVersion      string `json:"app_version"`
	ProtocolVersion string `json:"protocol_version"`
}

type StatusInfo struct {
	Online bool `json:"online"`
	Good   bool `json:"good"`
}

// ==================== Action / Response ====================

type ActionRequest struct {
	Action string         `json:"action"`
	Params map[string]any `json:"params"`
	Echo   string         `json:"echo,omitempty"`
}

type ActionResponse struct {
	Status  string `json:"status"`
	RetCode int    `json:"retcode"`
	Data    any    `json:"data,omitempty"`
	Echo    string `json:"echo,omitempty"`
}

func NewOKResponse(data any, echo string) ActionResponse {
	return ActionResponse{Status: "ok", RetCode: 0, Data: data, Echo: echo}
}

func NewFailedResponse(retCode int, echo string) ActionResponse {
	return ActionResponse{Status: "failed", RetCode: retCode, Echo: echo}
}

// ==================== Events ====================

// OneBotEvent is the interface all events implement.
type OneBotEvent interface {
	GetTime() int64
	GetSelfID() int64
	GetPostType() string
}

// EventBase provides common fields.
type EventBase struct {
	Time   int64  `json:"time"`
	SelfID int64  `json:"self_id"`
}

// ==================== Message Events ====================

type PrivateMessageEvent struct {
	EventBase
	PostType    string          `json:"post_type"`
	MessageType string          `json:"message_type"`
	SubType     string          `json:"sub_type"`
	MessageID   int64           `json:"message_id"`
	UserID      int64           `json:"user_id"`
	Message     MessageContent  `json:"message"`
	RawMessage  string          `json:"raw_message"`
	Font        int             `json:"font"`
	Sender      Sender          `json:"sender"`
}

func (e PrivateMessageEvent) GetTime() int64     { return e.Time }
func (e PrivateMessageEvent) GetSelfID() int64   { return e.SelfID }
func (e PrivateMessageEvent) GetPostType() string { return e.PostType }

type GroupMessageEvent struct {
	EventBase
	PostType    string         `json:"post_type"`
	MessageType string         `json:"message_type"`
	SubType     string         `json:"sub_type"`
	MessageID   int64          `json:"message_id"`
	GroupID     int64          `json:"group_id"`
	UserID      int64          `json:"user_id"`
	Anonymous   *Anonymous     `json:"anonymous,omitempty"`
	Message     MessageContent `json:"message"`
	RawMessage  string         `json:"raw_message"`
	Font        int            `json:"font"`
	Sender      GroupSender    `json:"sender"`
}

func (e GroupMessageEvent) GetTime() int64     { return e.Time }
func (e GroupMessageEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupMessageEvent) GetPostType() string { return e.PostType }

// ==================== Notice Events ====================

type GroupUploadEvent struct {
	EventBase
	PostType   string   `json:"post_type"`
	NoticeType string   `json:"notice_type"`
	GroupID    int64    `json:"group_id"`
	UserID     int64    `json:"user_id"`
	File       FileInfo `json:"file"`
}

func (e GroupUploadEvent) GetTime() int64     { return e.Time }
func (e GroupUploadEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupUploadEvent) GetPostType() string { return e.PostType }

type GroupAdminEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	SubType    string `json:"sub_type"`
	GroupID    int64  `json:"group_id"`
	UserID     int64  `json:"user_id"`
}

func (e GroupAdminEvent) GetTime() int64     { return e.Time }
func (e GroupAdminEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupAdminEvent) GetPostType() string { return e.PostType }

type GroupDecreaseEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	SubType    string `json:"sub_type"`
	GroupID    int64  `json:"group_id"`
	UserID     int64  `json:"user_id"`
	OperatorID int64  `json:"operator_id"`
}

func (e GroupDecreaseEvent) GetTime() int64     { return e.Time }
func (e GroupDecreaseEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupDecreaseEvent) GetPostType() string { return e.PostType }

type GroupIncreaseEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	SubType    string `json:"sub_type"`
	GroupID    int64  `json:"group_id"`
	UserID     int64  `json:"user_id"`
	OperatorID int64  `json:"operator_id"`
}

func (e GroupIncreaseEvent) GetTime() int64     { return e.Time }
func (e GroupIncreaseEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupIncreaseEvent) GetPostType() string { return e.PostType }

type GroupBanEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	SubType    string `json:"sub_type"`
	GroupID    int64  `json:"group_id"`
	UserID     int64  `json:"user_id"`
	OperatorID int64  `json:"operator_id"`
	Duration   int64  `json:"duration"`
}

func (e GroupBanEvent) GetTime() int64     { return e.Time }
func (e GroupBanEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupBanEvent) GetPostType() string { return e.PostType }

type FriendAddEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	UserID     int64  `json:"user_id"`
}

func (e FriendAddEvent) GetTime() int64     { return e.Time }
func (e FriendAddEvent) GetSelfID() int64   { return e.SelfID }
func (e FriendAddEvent) GetPostType() string { return e.PostType }

type GroupRecallEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	GroupID    int64  `json:"group_id"`
	UserID     int64  `json:"user_id"`
	OperatorID int64  `json:"operator_id"`
	MessageID  int64  `json:"message_id"`
}

func (e GroupRecallEvent) GetTime() int64     { return e.Time }
func (e GroupRecallEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupRecallEvent) GetPostType() string { return e.PostType }

type FriendRecallEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	UserID     int64  `json:"user_id"`
	MessageID  int64  `json:"message_id"`
}

func (e FriendRecallEvent) GetTime() int64     { return e.Time }
func (e FriendRecallEvent) GetSelfID() int64   { return e.SelfID }
func (e FriendRecallEvent) GetPostType() string { return e.PostType }

type PokeEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	SubType    string `json:"sub_type"`
	GroupID    int64  `json:"group_id"`
	UserID     int64  `json:"user_id"`
	TargetID   int64  `json:"target_id"`
}

func (e PokeEvent) GetTime() int64     { return e.Time }
func (e PokeEvent) GetSelfID() int64   { return e.SelfID }
func (e PokeEvent) GetPostType() string { return e.PostType }

type LuckyKingEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	SubType    string `json:"sub_type"`
	GroupID    int64  `json:"group_id"`
	UserID     int64  `json:"user_id"`
	TargetID   int64  `json:"target_id"`
}

func (e LuckyKingEvent) GetTime() int64     { return e.Time }
func (e LuckyKingEvent) GetSelfID() int64   { return e.SelfID }
func (e LuckyKingEvent) GetPostType() string { return e.PostType }

type HonorEvent struct {
	EventBase
	PostType   string `json:"post_type"`
	NoticeType string `json:"notice_type"`
	SubType    string `json:"sub_type"`
	GroupID    int64  `json:"group_id"`
	HonorType  string `json:"honor_type"`
	UserID     int64  `json:"user_id"`
}

func (e HonorEvent) GetTime() int64     { return e.Time }
func (e HonorEvent) GetSelfID() int64   { return e.SelfID }
func (e HonorEvent) GetPostType() string { return e.PostType }

// ==================== Request Events ====================

type FriendRequestEvent struct {
	EventBase
	PostType    string `json:"post_type"`
	RequestType string `json:"request_type"`
	Comment     string `json:"comment"`
	Flag        string `json:"flag"`
	UserID      int64  `json:"user_id"`
}

func (e FriendRequestEvent) GetTime() int64     { return e.Time }
func (e FriendRequestEvent) GetSelfID() int64   { return e.SelfID }
func (e FriendRequestEvent) GetPostType() string { return e.PostType }

type GroupRequestEvent struct {
	EventBase
	PostType    string `json:"post_type"`
	RequestType string `json:"request_type"`
	SubType     string `json:"sub_type"`
	Comment     string `json:"comment"`
	Flag        string `json:"flag"`
	GroupID     int64  `json:"group_id"`
	UserID      int64  `json:"user_id"`
}

func (e GroupRequestEvent) GetTime() int64     { return e.Time }
func (e GroupRequestEvent) GetSelfID() int64   { return e.SelfID }
func (e GroupRequestEvent) GetPostType() string { return e.PostType }

// ==================== Meta Events ====================

type LifecycleMetaEvent struct {
	EventBase
	PostType      string `json:"post_type"`
	MetaEventType string `json:"meta_event_type"`
	SubType       string `json:"sub_type"`
}

func (e LifecycleMetaEvent) GetTime() int64     { return e.Time }
func (e LifecycleMetaEvent) GetSelfID() int64   { return e.SelfID }
func (e LifecycleMetaEvent) GetPostType() string { return e.PostType }

type HeartbeatMetaEvent struct {
	EventBase
	PostType      string     `json:"post_type"`
	MetaEventType string     `json:"meta_event_type"`
	Status        StatusInfo `json:"status"`
	Interval      int64      `json:"interval"`
}

func (e HeartbeatMetaEvent) GetTime() int64     { return e.Time }
func (e HeartbeatMetaEvent) GetSelfID() int64   { return e.SelfID }
func (e HeartbeatMetaEvent) GetPostType() string { return e.PostType }

// ==================== Polymorphic Event Parsing ====================

// eventDiscrim carries all possible discriminator fields parsed in one pass.
type eventDiscrim struct {
	PostType      string `json:"post_type"`
	MessageType   string `json:"message_type"`
	NoticeType    string `json:"notice_type"`
	RequestType   string `json:"request_type"`
	MetaEventType string `json:"meta_event_type"`
	SubType       string `json:"sub_type"`
}

// ParseEvent parses a JSON byte slice into the correct event type.
func ParseEvent(data []byte) (OneBotEvent, error) {
	var d eventDiscrim
	if err := json.Unmarshal(data, &d); err != nil {
		return nil, fmt.Errorf("failed to parse event: %w", err)
	}

	switch d.PostType {
	case "message":
		if d.MessageType == "group" {
			var evt GroupMessageEvent
			if err := json.Unmarshal(data, &evt); err != nil {
				return nil, err
			}
			return evt, nil
		}
		var evt PrivateMessageEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "notice":
		return parseNoticeEvent(data, &d)
	case "request":
		if d.RequestType == "group" {
			var evt GroupRequestEvent
			if err := json.Unmarshal(data, &evt); err != nil {
				return nil, err
			}
			return evt, nil
		}
		var evt FriendRequestEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "meta_event":
		if d.MetaEventType == "heartbeat" {
			var evt HeartbeatMetaEvent
			if err := json.Unmarshal(data, &evt); err != nil {
				return nil, err
			}
			return evt, nil
		}
		var evt LifecycleMetaEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	default:
		return nil, fmt.Errorf("unknown post_type: %s", d.PostType)
	}
}

func parseNoticeEvent(data []byte, d *eventDiscrim) (OneBotEvent, error) {
	switch d.NoticeType {
	case "group_upload":
		var evt GroupUploadEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "group_admin":
		var evt GroupAdminEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "group_decrease":
		var evt GroupDecreaseEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "group_increase":
		var evt GroupIncreaseEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "group_ban":
		var evt GroupBanEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "friend_add":
		var evt FriendAddEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "group_recall":
		var evt GroupRecallEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "friend_recall":
		var evt FriendRecallEvent
		if err := json.Unmarshal(data, &evt); err != nil {
			return nil, err
		}
		return evt, nil
	case "notify":
		switch d.SubType {
		case "poke":
			var evt PokeEvent
			if err := json.Unmarshal(data, &evt); err != nil {
				return nil, err
			}
			return evt, nil
		case "lucky_king":
			var evt LuckyKingEvent
			if err := json.Unmarshal(data, &evt); err != nil {
				return nil, err
			}
			return evt, nil
		case "honor":
			var evt HonorEvent
			if err := json.Unmarshal(data, &evt); err != nil {
				return nil, err
			}
			return evt, nil
		default:
			return nil, fmt.Errorf("unknown notify sub_type: %s", d.SubType)
		}
	default:
		return nil, fmt.Errorf("unknown notice_type: %s", d.NoticeType)
	}
}
