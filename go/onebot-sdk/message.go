package onebot

import (
	"fmt"
	"regexp"
	"strings"
)

// CQ code regex: [CQ:type,key1=value1,key2=value2,...]
var cqCodeRegex = regexp.MustCompile(`\[CQ:([a-zA-Z0-9._-]+)((?:,[^,\]]*=[^,\]]*)*)\]`)

// Escape / unescape for CQ codes.

func EscapeText(s string) string {
	s = strings.ReplaceAll(s, "&", "&amp;")
	s = strings.ReplaceAll(s, "[", "&#91;")
	s = strings.ReplaceAll(s, "]", "&#93;")
	return s
}

func UnescapeText(s string) string {
	s = strings.ReplaceAll(s, "&#93;", "]")
	s = strings.ReplaceAll(s, "&#91;", "[")
	s = strings.ReplaceAll(s, "&amp;", "&")
	return s
}

func EscapeParam(s string) string {
	s = EscapeText(s)
	s = strings.ReplaceAll(s, ",", "&#44;")
	return s
}

func UnescapeParam(s string) string {
	s = strings.ReplaceAll(s, "&#44;", ",")
	return UnescapeText(s)
}

// ParseCQCode parses a CQ code string into message segments.
func ParseCQCode(raw string) []MessageSegment {
	if raw == "" {
		return nil
	}

	var segments []MessageSegment
	matches := cqCodeRegex.FindAllStringSubmatchIndex(raw, -1)

	if len(matches) == 0 {
		return []MessageSegment{TextSegment(UnescapeText(raw))}
	}

	lastEnd := 0
	for _, match := range matches {
		start := match[0]
		end := match[1]

		// Text before this CQ code
		if start > lastEnd {
			text := UnescapeText(raw[lastEnd:start])
			if text != "" {
				segments = append(segments, TextSegment(text))
			}
		}

		// Parse CQ code
		// group 2: type, group 4: params string (starting with ",")
		typeStart, typeEnd := match[2], match[3]
		cqType := raw[typeStart:typeEnd]

		params := make(map[string]string)
		paramsStart, paramsEnd := match[4], match[5]
		if paramsStart >= 0 && paramsEnd > paramsStart {
			paramStr := raw[paramsStart+1 : paramsEnd] // skip leading ","
			for _, kv := range strings.Split(paramStr, ",") {
				eqIdx := strings.Index(kv, "=")
				if eqIdx > 0 {
					key := kv[:eqIdx]
					val := UnescapeParam(kv[eqIdx+1:])
					params[key] = val
				}
			}
		}

		segments = append(segments, MessageSegment{Type: cqType, Data: params})
		lastEnd = end
	}

	// Trailing text after last CQ code
	if lastEnd < len(raw) {
		text := UnescapeText(raw[lastEnd:])
		if text != "" {
			segments = append(segments, TextSegment(text))
		}
	}

	return segments
}

// SerializeCQCode serializes message segments to a CQ code string.
func SerializeCQCode(segments []MessageSegment) string {
	var sb strings.Builder
	for _, seg := range segments {
		if seg.Type == "text" {
			sb.WriteString(EscapeText(seg.Data["text"]))
			continue
		}
		sb.WriteString(fmt.Sprintf("[CQ:%s", seg.Type))
		for k, v := range seg.Data {
			sb.WriteString(fmt.Sprintf(",%s=%s", k, EscapeParam(v)))
		}
		sb.WriteString("]")
	}
	return sb.String()
}
