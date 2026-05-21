package onebot

import (
	"reflect"
	"testing"
)

func TestParseCQCode_PlainText(t *testing.T) {
	segments := ParseCQCode("Hello World")
	if len(segments) != 1 {
		t.Fatalf("expected 1 segment, got %d", len(segments))
	}
	if segments[0].Type != "text" {
		t.Errorf("expected type 'text', got %q", segments[0].Type)
	}
	if segments[0].Data["text"] != "Hello World" {
		t.Errorf("expected text 'Hello World', got %q", segments[0].Data["text"])
	}
}

func TestParseCQCode_SingleCQCode(t *testing.T) {
	segments := ParseCQCode("[CQ:at,qq=123]")
	if len(segments) != 1 {
		t.Fatalf("expected 1 segment, got %d", len(segments))
	}
	if segments[0].Type != "at" {
		t.Errorf("expected type 'at', got %q", segments[0].Type)
	}
	if segments[0].Data["qq"] != "123" {
		t.Errorf("expected qq '123', got %q", segments[0].Data["qq"])
	}
}

func TestParseCQCode_MixedTextAndCQCode(t *testing.T) {
	segments := ParseCQCode("Hello [CQ:at,qq=123] World")
	if len(segments) != 3 {
		t.Fatalf("expected 3 segments, got %d", len(segments))
	}

	if segments[0].Type != "text" || segments[0].Data["text"] != "Hello " {
		t.Errorf("segment 0: expected text 'Hello ', got type=%q data=%v", segments[0].Type, segments[0].Data)
	}
	if segments[1].Type != "at" || segments[1].Data["qq"] != "123" {
		t.Errorf("segment 1: expected at qq=123, got type=%q data=%v", segments[1].Type, segments[1].Data)
	}
	if segments[2].Type != "text" || segments[2].Data["text"] != " World" {
		t.Errorf("segment 2: expected text ' World', got type=%q data=%v", segments[2].Type, segments[2].Data)
	}
}

func TestParseCQCode_EmptyString(t *testing.T) {
	segments := ParseCQCode("")
	if segments != nil {
		t.Errorf("expected nil for empty string, got %v", segments)
	}
}

func TestParseCQCode_MultipleCQCodes(t *testing.T) {
	segments := ParseCQCode("[CQ:at,qq=123][CQ:face,id=1]")
	if len(segments) != 2 {
		t.Fatalf("expected 2 segments, got %d", len(segments))
	}
	if segments[0].Type != "at" || segments[0].Data["qq"] != "123" {
		t.Errorf("segment 0: expected at qq=123")
	}
	if segments[1].Type != "face" || segments[1].Data["id"] != "1" {
		t.Errorf("segment 1: expected face id=1")
	}
}

func TestParseCQCode_EscapedCharacters(t *testing.T) {
	// In plain text, &amp; &#91; &#93; are unescaped by ParseCQCode
	segments := ParseCQCode("Hello &amp; &#91;text&#93; here")
	if len(segments) != 1 {
		t.Fatalf("expected 1 segment, got %d", len(segments))
	}
	expected := "Hello & [text] here"
	if segments[0].Data["text"] != expected {
		t.Errorf("expected %q, got %q", expected, segments[0].Data["text"])
	}
}

func TestParseCQCode_EscapedInParams(t *testing.T) {
	// In CQ code params, &#44; should be unescaped by UnescapeParam
	segments := ParseCQCode("[CQ:custom,key=val&#44;ue]")
	if len(segments) != 1 {
		t.Fatalf("expected 1 segment, got %d", len(segments))
	}
	if segments[0].Type != "custom" {
		t.Errorf("expected type 'custom', got %q", segments[0].Type)
	}
	if segments[0].Data["key"] != "val,ue" {
		t.Errorf("expected key value 'val,ue', got %q", segments[0].Data["key"])
	}
}

func TestSerializeCQCode_TextOnly(t *testing.T) {
	segments := []MessageSegment{
		TextSegment("Hello World"),
	}
	result := SerializeCQCode(segments)
	if result != "Hello World" {
		t.Errorf("expected 'Hello World', got %q", result)
	}
}

func TestSerializeCQCode_MultipleSegments(t *testing.T) {
	segments := []MessageSegment{
		TextSegment("Hello "),
		AtSegment("123"),
		TextSegment(" how are you?"),
		FaceSegment("1"),
	}
	result := SerializeCQCode(segments)
	expected := "Hello [CQ:at,qq=123] how are you?[CQ:face,id=1]"
	if result != expected {
		t.Errorf("expected %q, got %q", expected, result)
	}
}

func TestSerializeCQCode_EscapesSpecialChars(t *testing.T) {
	segments := []MessageSegment{
		TextSegment("Hello [world] & test"),
	}
	result := SerializeCQCode(segments)
	// & → &amp;, [ → &#91;, ] → &#93;
	expected := "Hello &#91;world&#93; &amp; test"
	if result != expected {
		t.Errorf("expected %q, got %q", expected, result)
	}
}

func TestSerializeCQCode_EscapesParams(t *testing.T) {
	segments := []MessageSegment{
		MessageSegment{Type: "custom", Data: map[string]string{"key": "a,b"}},
	}
	result := SerializeCQCode(segments)
	expected := "[CQ:custom,key=a&#44;b]"
	if result != expected {
		t.Errorf("expected %q, got %q", expected, result)
	}
}

func TestParseSerializeRoundTrip(t *testing.T) {
	input := "Hello [CQ:at,qq=123] World [CQ:face,id=1] end"
	parsed := ParseCQCode(input)
	serialized := SerializeCQCode(parsed)
	reparsed := ParseCQCode(serialized)

	if !reflect.DeepEqual(parsed, reparsed) {
		t.Errorf("round-trip mismatch:\n  parsed: %+v\n  reparsed: %+v", parsed, reparsed)
	}
}

func TestParseSerializeRoundTrip_ComplexParams(t *testing.T) {
	// Test with params containing special characters
	input := "[CQ:custom,key1=value1,key2=value2,key3=val&#44;ue3]"
	parsed := ParseCQCode(input)
	serialized := SerializeCQCode(parsed)
	reparsed := ParseCQCode(serialized)

	if !reflect.DeepEqual(parsed, reparsed) {
		t.Errorf("round-trip mismatch:\n  parsed: %+v\n  reparsed: %+v", parsed, reparsed)
	}
}

func TestEscapeUnescapeText_RoundTrip(t *testing.T) {
	tests := []struct {
		name  string
		input string
	}{
		{"plain text", "Hello World"},
		{"with ampersand", "Hello & World"},
		{"with brackets", "Hello [World]"},
		{"with all specials", "&[] test & test"},
		{"with amp entity in original", "AT&amp;T"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			escaped := EscapeText(tt.input)
			unescaped := UnescapeText(escaped)
			if unescaped != tt.input {
				t.Errorf("round-trip failed: input=%q escaped=%q unescaped=%q",
					tt.input, escaped, unescaped)
			}
		})
	}
}

func TestEscapeParamUnescapeParam_RoundTrip(t *testing.T) {
	tests := []struct {
		name  string
		input string
	}{
		{"plain text", "Hello World"},
		{"with comma", "a,b,c"},
		{"with ampersand and comma", "a,b & c"},
		{"with brackets and comma", "a,[b],c"},
		{"with all specials", "&[,] test"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			escaped := EscapeParam(tt.input)
			unescaped := UnescapeParam(escaped)
			if unescaped != tt.input {
				t.Errorf("round-trip failed: input=%q escaped=%q unescaped=%q",
					tt.input, escaped, unescaped)
			}
		})
	}
}

func TestEscapeParam_ContainsComma(t *testing.T) {
	result := EscapeParam("a,b")
	if result == "a,b" {
		t.Error("expected comma to be escaped")
	}
	// Should not contain raw comma
	for i := 0; i < len(result); i++ {
		if result[i] == ',' {
			t.Error("result contains raw comma")
		}
	}
}

func TestUnescapeParam_RestoresComma(t *testing.T) {
	result := UnescapeParam("a&#44;b")
	if result != "a,b" {
		t.Errorf("expected 'a,b', got %q", result)
	}
}

func TestParseCQCode_OnlyCQCode(t *testing.T) {
	segments := ParseCQCode("[CQ:image,file=http://example.com/img.jpg,type=show]")
	if len(segments) != 1 {
		t.Fatalf("expected 1 segment, got %d", len(segments))
	}
	if segments[0].Type != "image" {
		t.Errorf("expected type 'image', got %q", segments[0].Type)
	}
	if segments[0].Data["file"] != "http://example.com/img.jpg" {
		t.Errorf("expected file 'http://example.com/img.jpg', got %q", segments[0].Data["file"])
	}
	if segments[0].Data["type"] != "show" {
		t.Errorf("expected type 'show', got %q", segments[0].Data["type"])
	}
}

func TestParseCQCode_NoParams(t *testing.T) {
	segments := ParseCQCode("[CQ:rps]")
	if len(segments) != 1 {
		t.Fatalf("expected 1 segment, got %d", len(segments))
	}
	if segments[0].Type != "rps" {
		t.Errorf("expected type 'rps', got %q", segments[0].Type)
	}
	if len(segments[0].Data) != 0 {
		t.Errorf("expected empty data, got %v", segments[0].Data)
	}
}

func TestSerializeCQCode_EmptySegments(t *testing.T) {
	result := SerializeCQCode(nil)
	if result != "" {
		t.Errorf("expected empty string, got %q", result)
	}

	result = SerializeCQCode([]MessageSegment{})
	if result != "" {
		t.Errorf("expected empty string, got %q", result)
	}
}
