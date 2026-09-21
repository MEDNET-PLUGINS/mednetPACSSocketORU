package com.mednet.pacssocketoru.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * One HL7 v2 segment (MSH, PID, OBX, ...). Field numbers are 1-based as in the spec.
 */
public class Hl7Segment {

    private final String id;
    private final String raw;
    private final Hl7Encoding encoding;
    private final String[] fields;

    public Hl7Segment(String raw, Hl7Encoding encoding) {
        this.raw = raw == null ? "" : raw;
        this.encoding = encoding == null ? Hl7Encoding.hl7Default() : encoding;
        if (this.raw.length() >= 3) {
            this.id = this.raw.substring(0, 3);
        } else {
            this.id = this.raw;
        }
        this.fields = "OBX".equals(this.id)
                ? splitObxFields(this.raw, this.encoding.fieldSeparator())
                : splitFields(this.raw, this.encoding.fieldSeparator());
    }

    public String getId() {
        return id;
    }

    public String getRaw() {
        return raw;
    }

    public Hl7Encoding getEncoding() {
        return encoding;
    }

    /**
     * @param fieldNumber 1-based HL7 field number (MSH-12 = 12, PID-3 = 3)
     */
    public String field(int fieldNumber) {
        if (fieldNumber < 1) {
            return "";
        }
        if ("MSH".equals(id)) {
            if (fieldNumber == 1) {
                return String.valueOf(encoding.fieldSeparator());
            }
            int index = fieldNumber - 1;
            return index < fields.length ? nullToEmpty(fields[index]) : "";
        }
        return fieldNumber < fields.length ? nullToEmpty(fields[fieldNumber]) : "";
    }

    public String component(int fieldNumber, int componentNumber) {
        String field = field(fieldNumber);
        if (field.isEmpty() || componentNumber < 1) {
            return "";
        }
        String[] parts = split(field, encoding.componentSeparator());
        return componentNumber <= parts.length ? nullToEmpty(parts[componentNumber - 1]) : "";
    }

    public String[] repetitions(int fieldNumber) {
        String field = field(fieldNumber);
        if (field.isEmpty()) {
            return new String[0];
        }
        return split(field, encoding.repetitionSeparator());
    }

    public int fieldCount() {
        if ("MSH".equals(id)) {
            return Math.max(fields.length, 1);
        }
        return Math.max(fields.length - 1, 0);
    }

    /**
     * Fenix and other PACS vendors put HTML (and sometimes '|') in OBX-5.
     * Take field 5 as everything until the last HL7 result-status trailer ||||||F|.
     */
    private static String[] splitObxFields(String raw, char fieldSeparator) {
        int pipes = 0;
        int index = 0;
        while (index < raw.length() && pipes < 5) {
            if (raw.charAt(index) == fieldSeparator) {
                pipes++;
            }
            index++;
        }
        if (pipes < 5) {
            return splitFields(raw, fieldSeparator);
        }
        String header = raw.substring(0, index - 1);
        String rest = raw.substring(index);
        int trailerAt = lastStatusTrailer(rest, fieldSeparator);
        List<String> parts = new ArrayList<>();
        for (String piece : splitFields(header, fieldSeparator)) {
            parts.add(piece);
        }
        if (trailerAt < 0) {
            parts.add(rest);
            return parts.toArray(new String[0]);
        }
        parts.add(rest.substring(0, trailerAt));
        String trailer = rest.substring(trailerAt);
        String[] trailerFields = splitFields(trailer, fieldSeparator);
        int start = trailer.startsWith(String.valueOf(fieldSeparator)) ? 1 : 0;
        for (int i = start; i < trailerFields.length; i++) {
            parts.add(trailerFields[i]);
        }
        return parts.toArray(new String[0]);
    }

    private static int lastStatusTrailer(String fromField5, char fieldSeparator) {
        if (fromField5 == null || fromField5.length() < 7) {
            return -1;
        }
        String sixPipes = String.valueOf(fieldSeparator).repeat(6);
        int found = -1;
        int search = 0;
        while (search <= fromField5.length() - 7) {
            int at = fromField5.indexOf(sixPipes, search);
            if (at < 0) {
                break;
            }
            int statusAt = at + 6;
            if (statusAt < fromField5.length()) {
                char status = fromField5.charAt(statusAt);
                boolean endOrPipe = statusAt + 1 == fromField5.length()
                        || fromField5.charAt(statusAt + 1) == fieldSeparator;
                if (status >= 'A' && status <= 'Z' && endOrPipe) {
                    found = at;
                }
            }
            search = at + 1;
        }
        return found;
    }

    private static String[] splitFields(String raw, char fieldSeparator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == fieldSeparator) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static String[] split(String value, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == separator) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
