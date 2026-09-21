package com.mednet.pacssocketoru.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits an HL7 v2 payload into segments. Handles:
 * <ul>
 *   <li>standard messages with CR / LF separators</li>
 *   <li>report text that contains raw newlines inside OBX-5</li>
 *   <li>vendor payloads where segments are concatenated (PID glued to MSH, OBR glued to ORC, etc.)</li>
 *   <li>MLLP start (0x0B) and end (0x1C) bytes left on the wire</li>
 * </ul>
 */
public final class Hl7MessageNormalizer {

    private static final Pattern SEGMENT_START = Pattern.compile(
            "(MSH|PID|PD1|NK1|PV1|PV2|ORC|OBR|OBX|NTE|MSA|ERR|DSC|CTI|Z[A-Z0-9]{2})\\|");

    private Hl7MessageNormalizer() {
    }

    public static String stripMllp(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw;
        if (!value.isEmpty() && value.charAt(0) == 0x0B) {
            value = value.substring(1);
        }
        int fs = value.indexOf(0x1C);
        if (fs >= 0) {
            value = value.substring(0, fs);
        }
        return value.trim();
    }

    public static List<String> splitSegments(String rawMessage) {
        String payload = stripMllp(rawMessage);
        List<String> segments = new ArrayList<>();
        if (payload.isEmpty()) {
            return segments;
        }

        if (hasExplicitSegmentBreaks(payload)) {
            segments.addAll(splitKeepingContinuationLines(payload));
            if (hasSegment(segments, "MSH") && (hasSegment(segments, "PID") || hasSegment(segments, "OBX"))) {
                return segments;
            }
            payload = String.join("", segments);
            segments.clear();
        }

        Matcher matcher = SEGMENT_START.matcher(payload);
        int start = 0;
        boolean first = true;
        while (matcher.find()) {
            if (first) {
                first = false;
                start = matcher.start();
                continue;
            }
            String piece = payload.substring(start, matcher.start()).trim();
            if (!piece.isEmpty()) {
                segments.add(piece);
            }
            start = matcher.start();
        }
        if (start < payload.length()) {
            String piece = payload.substring(start).trim();
            if (!piece.isEmpty()) {
                segments.add(piece);
            }
        }
        if (segments.isEmpty() && payload.startsWith("MSH")) {
            segments.add(payload);
        }
        return segments;
    }

    public static String toPipeMessage(String rawMessage) {
        List<String> segments = splitSegments(rawMessage);
        return String.join("\r", segments);
    }

    public static String extractVersion(String rawMessage) {
        List<String> segments = splitSegments(rawMessage);
        if (segments.isEmpty()) {
            return "";
        }
        Hl7Encoding encoding = Hl7Encoding.fromMsh(segments.get(0));
        Hl7Segment msh = new Hl7Segment(segments.get(0), encoding);
        return msh.field(12);
    }

    /**
     * CR/LF is the HL7 segment separator, but PACS often puts real newlines inside OBX-5.
     * Only start a new segment when the line begins with a known segment ID.
     */
    static List<String> splitKeepingContinuationLines(String payload) {
        String[] lines = payload.split("\\r\\n|\\n|\\r", -1);
        List<String> segments = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!segments.isEmpty()) {
                    int last = segments.size() - 1;
                    segments.set(last, segments.get(last) + "\n");
                }
                continue;
            }
            if (isSegmentHeader(trimmed)) {
                segments.add(trimmed);
            } else if (!segments.isEmpty()) {
                int last = segments.size() - 1;
                String previous = segments.get(last);
                segments.set(last, previous + "\n" + line);
            }
        }
        return segments;
    }

    static boolean isSegmentHeader(String line) {
        return line != null && SEGMENT_START.matcher(line).lookingAt();
    }

    private static boolean hasSegment(List<String> segments, String id) {
        for (String segment : segments) {
            if (segment.startsWith(id + "|") || segment.startsWith(id + "^")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExplicitSegmentBreaks(String payload) {
        return payload.indexOf('\r') >= 0 || payload.indexOf('\n') >= 0;
    }
}
