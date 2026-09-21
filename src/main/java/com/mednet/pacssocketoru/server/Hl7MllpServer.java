package com.mednet.pacssocketoru.server;

import com.mednet.pacssocketoru.client.PacsReceiverClient;
import com.mednet.pacssocketoru.config.PacsSocketProperties;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;

/**
 * TCP / MLLP listener for inbound PACS ORU^R01 reports. Default port is 6161.
 * Forwards raw HL7 to ExternalPACS receive API with configured pacsName.
 */
public class Hl7MllpServer {

    public static final int DEFAULT_PORT = PacsSocketProperties.DEFAULT_PORT;

    private final int port;
    private final String pacsName;
    private final PacsReceiverClient receiverClient;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public Hl7MllpServer(PacsSocketProperties properties) {
        this.port = properties.mllpPort();
        this.pacsName = properties.pacsName();
        this.receiverClient = new PacsReceiverClient(properties.receiverApiUrl());
    }

    public static void main(String[] args) throws IOException {
        PacsSocketProperties properties = PacsSocketProperties.load();
        log("Loaded config from " + properties.loadedFrom());
        log("pacs.name=" + properties.pacsName());
        log("pacs.receiver.api.url=" + properties.receiverApiUrl());
        log("pacs.oru.mllp.port=" + properties.mllpPort());
        new Hl7MllpServer(properties).listen();
    }

    public void listen() {
        running = true;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new java.net.InetSocketAddress(port));
            log("HL7 MLLP server listening on 0.0.0.0:" + port);
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "pacs-oru-mllp-shutdown"));
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    log("Client connected: " + client.getInetAddress().getHostAddress()
                            + ":" + client.getPort());
                    handleClient(client);
                } catch (IOException ex) {
                    if (running) {
                        log("Accept failed: " + ex.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            log("Failed to bind MLLP port " + port + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        log("HL7 MLLP server stopped");
    }

    private void handleClient(Socket client) {
        try (Socket socket = client) {
            socket.setSoTimeout(120000);
            while (running && !socket.isClosed()) {
                String raw = MllpFraming.readMessage(socket.getInputStream());
                if (raw == null) {
                    break;
                }
                if (raw.isBlank()) {
                    continue;
                }
                log("Received " + raw.length() + " chars");
                process(raw);
            }
        } catch (IOException ex) {
            log("Connection closed: " + ex.getMessage());
        }
    }

    void process(String rawHl7) {
        try {
            log("Raw HL7 before forward (" + (rawHl7 == null ? 0 : rawHl7.length()) + " chars):\n" + forLog(rawHl7));
            receiverClient.postRawHl7(pacsName, rawHl7);
            log("Forwarded to receiver API");
        } catch (Exception ex) {
            log("Receiver API error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void log(String message) {
        System.out.println(LocalDateTime.now() + " [HL7] " + message);
    }

    static String forLog(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\n", "<LF>\n")
                .replace("\r", "<CR>\n");
    }
}
