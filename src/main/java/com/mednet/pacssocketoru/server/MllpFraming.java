package com.mednet.pacssocketoru.server;

import com.mednet.pacssocketoru.parser.Hl7Encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    public static String wrap(String hl7Payload) {
        String payload = hl7Payload == null ? "" : hl7Payload;
        return START_OF_BLOCK + payload + END_OF_BLOCK + CARRIAGE_RETURN;
    }

    public static byte[] wrapBytes(String hl7Payload) {
        return wrap(hl7Payload).getBytes(StandardCharsets.UTF_8);
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
        return Hl7Encoding.decodeUtf8(buffer.toByteArray());
    }

    public static void writeMessage(OutputStream out, String hl7Payload) throws IOException {
        out.write(wrapBytes(hl7Payload));
        out.flush();
    }
}
