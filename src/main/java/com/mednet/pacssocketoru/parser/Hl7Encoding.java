package com.mednet.pacssocketoru.parser;

import java.nio.charset.StandardCharsets;

/**
 * Encoding characters from MSH-1 / MSH-2.
 * Default HL7: field '|', component '^', repetition '~', escape '\', subcomponent '&amp;'.
 */
public class Hl7Encoding {

    public static final char DEFAULT_FIELD = '|';
    public static final char DEFAULT_COMPONENT = '^';
    public static final char DEFAULT_REPETITION = '~';
    public static final char DEFAULT_ESCAPE = '\\';
    public static final char DEFAULT_SUBCOMPONENT = '&';

    private final char fieldSeparator;
    private final char componentSeparator;
    private final char repetitionSeparator;
    private final char escapeCharacter;
    private final char subcomponentSeparator;

    public Hl7Encoding(char fieldSeparator, char componentSeparator, char repetitionSeparator,
                       char escapeCharacter, char subcomponentSeparator) {
        this.fieldSeparator = fieldSeparator;
        this.componentSeparator = componentSeparator;
        this.repetitionSeparator = repetitionSeparator;
        this.escapeCharacter = escapeCharacter;
        this.subcomponentSeparator = subcomponentSeparator;
    }

    public static Hl7Encoding hl7Default() {
        return new Hl7Encoding(DEFAULT_FIELD, DEFAULT_COMPONENT, DEFAULT_REPETITION,
                DEFAULT_ESCAPE, DEFAULT_SUBCOMPONENT);
    }

    public static Hl7Encoding fromMsh(String mshSegment) {
        if (mshSegment == null || mshSegment.length() < 8 || !mshSegment.startsWith("MSH")) {
            return hl7Default();
        }
        char field = mshSegment.charAt(3);
        String encoding = "";
        int nextField = mshSegment.indexOf(field, 4);
        if (nextField > 4) {
            encoding = mshSegment.substring(4, nextField);
        } else if (mshSegment.length() >= 8) {
            encoding = mshSegment.substring(4, 8);
        }
        char component = encoding.length() > 0 ? encoding.charAt(0) : DEFAULT_COMPONENT;
        char repetition = encoding.length() > 1 ? encoding.charAt(1) : DEFAULT_REPETITION;
        char escape = encoding.length() > 2 ? encoding.charAt(2) : DEFAULT_ESCAPE;
        char sub = encoding.length() > 3 ? encoding.charAt(3) : DEFAULT_SUBCOMPONENT;
        return new Hl7Encoding(field, component, repetition, escape, sub);
    }

    public char fieldSeparator() {
        return fieldSeparator;
    }

    public char componentSeparator() {
        return componentSeparator;
    }

    public char repetitionSeparator() {
        return repetitionSeparator;
    }

    public char escapeCharacter() {
        return escapeCharacter;
    }

    public char subcomponentSeparator() {
        return subcomponentSeparator;
    }

    /**
     * Unescape HL7 escape sequences used in OBX observation text (\X0D\, \X0A\, \F\, \S\, \T\, \R\, \E\).
     */
    public String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        char esc = escapeCharacter;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != esc) {
                out.append(c);
                continue;
            }
            int close = value.indexOf(esc, i + 1);
            if (close < 0) {
                out.append(c);
                continue;
            }
            String token = value.substring(i + 1, close);
            if ("F".equals(token)) {
                out.append(fieldSeparator);
            } else if ("S".equals(token)) {
                out.append(componentSeparator);
            } else if ("T".equals(token)) {
                out.append(subcomponentSeparator);
            } else if ("R".equals(token)) {
                out.append(repetitionSeparator);
            } else if ("E".equals(token)) {
                out.append(esc);
            } else if (token.length() >= 3 && (token.charAt(0) == 'X' || token.charAt(0) == 'x')) {
                try {
                    int codePoint = Integer.parseInt(token.substring(1), 16);
                    out.append((char) codePoint);
                } catch (NumberFormatException ex) {
                    out.append(c).append(token).append(esc);
                }
            } else {
                out.append(c).append(token).append(esc);
            }
            i = close;
        }
        return out.toString();
    }

    /**
     * Decode raw HL7 bytes as UTF-8 (BOM stripped). If the result looks like UTF-8 read as Latin-1
     * ({@code Â«}, {@code â€}), recover the original Unicode.
     */
    public static String decodeUtf8(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        int offset = 0;
        int length = bytes.length;
        if (length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
            length -= 3;
        }
        String decoded = new String(bytes, offset, length, StandardCharsets.UTF_8);
        return repairUtf8Mojibake(decoded);
    }

    static String repairUtf8Mojibake(String value) {
        if (value == null || value.isEmpty() || !looksLikeUtf8Mojibake(value)) {
            return value;
        }
        return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    static boolean looksLikeUtf8Mojibake(String value) {
        return value.contains("Â«")
                || value.contains("Â»")
                || value.contains("â€");
    }
}
