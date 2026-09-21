package com.mednet.pacssocketoru.ack;

import com.mednet.pacssocketoru.parser.Hl7Encoding;
import com.mednet.pacssocketoru.parser.Hl7MessageNormalizer;
import com.mednet.pacssocketoru.parser.Hl7Segment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds an HL7 ACK for a received ORU. Sending/receiving apps are swapped.
 */
public final class Hl7AckBuilder {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private Hl7AckBuilder() {
    }

    public static String accept(String rawHl7) {
        return build(mshFromRaw(rawHl7), "AA", "Message accepted");
    }

    public static String error(String rawHl7, String text) {
        return build(mshFromRaw(rawHl7), "AE", text == null ? "Application error" : text);
    }

    private static String build(MshInfo original, String ackCode, String text) {
        String version = blankToDefault(original.versionId, "2.3");
        String controlId = blankToDefault(original.messageControlId, "UNKNOWN");
        String now = LocalDateTime.now().format(TS);
        String trigger = blankToDefault(original.triggerEvent, "R01");

        String msh = String.join("|",
                "MSH",
                "^~\\&",
                blankToDefault(original.receivingApplication, "HIS"),
                blankToDefault(original.receivingFacility, "HIS"),
                blankToDefault(original.sendingApplication, "RIS"),
                blankToDefault(original.sendingFacility, "RIS"),
                now,
                "",
                "ACK^" + trigger,
                controlId,
                "P",
                version
        );
        String msa = String.join("|",
                "MSA",
                ackCode,
                controlId,
                sanitize(text)
        );
        return msh + "\r" + msa;
    }

    private static MshInfo mshFromRaw(String rawHl7) {
        MshInfo msh = new MshInfo();
        List<String> segments = Hl7MessageNormalizer.splitSegments(rawHl7 == null ? "" : rawHl7);
        if (segments.isEmpty()) {
            return msh;
        }
        Hl7Encoding encoding = Hl7Encoding.fromMsh(segments.get(0));
        Hl7Segment segment = new Hl7Segment(segments.get(0), encoding);
        msh.sendingApplication = segment.field(3);
        msh.sendingFacility = segment.field(4);
        msh.receivingApplication = segment.field(5);
        msh.receivingFacility = segment.field(6);
        msh.messageControlId = segment.field(10);
        msh.versionId = segment.field(12);
        msh.triggerEvent = segment.component(9, 2);
        return msh;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('|', '/').replace('\r', ' ').replace('\n', ' ');
    }

    private static final class MshInfo {
        private String sendingApplication;
        private String sendingFacility;
        private String receivingApplication;
        private String receivingFacility;
        private String messageControlId;
        private String versionId;
        private String triggerEvent;
    }
}
