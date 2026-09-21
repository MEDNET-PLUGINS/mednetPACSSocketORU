package com.mednet.pacssocketoru.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Lower Layer Protocol framing used by HL7 v2 over TCP:
 * start block 0x0B, payload, end block 0x1C, carriage return 0x0D.
 */
public final class MllpFraming {

    public static final char START_OF_BLOCK = 0x0B;
    public static final char END_OF_BLOCK = 0x1C;
    public static final char CARRIAGE_RETURN = 0x0D;

    private MllpFraming() {
    }

    /**
     * Reads one MLLP (or FS-terminated) message. Returns null on EOF before any data.
     */
    public static String readMessage(InputStream in) throws IOException {
        int first = in.read();
        if (first == -1) {
            return null;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean started = first == START_OF_BLOCK;
        if (!started && first != END_OF_BLOCK) {
            buffer.write(first);
        }

        int previous = first;
        int current;
        while ((current = in.read()) != -1) {
            if (current == END_OF_BLOCK) {
                int maybeCr = in.read();
                if (maybeCr != -1 && maybeCr != CARRIAGE_RETURN) {
                    // FS without CR still ends the message; unread is not available, so drop the extra byte only if CR
                }
                break;
            }
            if (!started) {
                if (current == START_OF_BLOCK) {
                    started = true;
                    buffer.reset();
                } else {
                    buffer.write(current);
                }
            } else if (current != START_OF_BLOCK) {
                buffer.write(current);
            }
            previous = current;
        }

        if (buffer.size() == 0 && previous == -1) {
            return null;
        }
        return decodeUtf8(buffer.toByteArray());
    }

    /**
     * Decode raw HL7 bytes as UTF-8 (BOM stripped). If the result looks like UTF-8 read as Latin-1
     * ({@code Â«}, {@code â€}), recover the original Unicode.
     */
    static String decodeUtf8(byte[] bytes) {
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
        return decoded;
    }
}
