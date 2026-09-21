package com.mednet.pacssocketoru.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Posts raw HL7 to ExternalPACS {@code /api/v1/report/receive/{pacsName}}.
 */
public final class PacsReceiverClient {

    private final String receiverApiUrl;

    public PacsReceiverClient(String receiverApiUrl) {
        this.receiverApiUrl = trimTrailingSlash(receiverApiUrl);
    }

    public void postRawHl7(String pacsName, String rawHl7) throws IOException {
        if (pacsName == null || pacsName.isBlank()) {
            throw new IOException("pacsName is blank");
        }
        if (rawHl7 == null || rawHl7.isBlank()) {
            throw new IOException("raw HL7 is blank");
        }

        URL url = new URL(receiverApiUrl + "/" + pacsName.trim());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");

            byte[] body = rawHl7.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }

            int status = connection.getResponseCode();
            String responseBody = readBody(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            System.out.println("PACS receiver API status : " + status);
            System.out.println("PACS receiver API body : " + responseBody);

            if (status < 200 || status >= 300) {
                throw new IOException("Receiver API failed with status " + status + " : " + responseBody);
            }
            if (looksUnsuccessful(responseBody)) {
                throw new IOException("Receiver API returned failure : " + extractErrorMessage(responseBody));
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean looksUnsuccessful(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }
        String compact = responseBody.replace(" ", "");
        return compact.contains("\"success\":false");
    }

    private static String extractErrorMessage(String responseBody) {
        if (responseBody == null) {
            return "unknown error";
        }
        String marker = "\"errorMessage\"";
        int key = responseBody.indexOf(marker);
        if (key < 0) {
            return responseBody;
        }
        int colon = responseBody.indexOf(':', key + marker.length());
        int firstQuote = responseBody.indexOf('"', colon + 1);
        int lastQuote = responseBody.indexOf('"', firstQuote + 1);
        if (firstQuote >= 0 && lastQuote > firstQuote) {
            return responseBody.substring(firstQuote + 1, lastQuote);
        }
        return responseBody;
    }

    private static String readBody(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String value = url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
