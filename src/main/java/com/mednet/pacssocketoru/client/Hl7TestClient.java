package com.mednet.pacssocketoru.client;

import com.mednet.pacssocketoru.sample.SampleHl7Messages;
import com.mednet.pacssocketoru.server.Hl7MllpServer;
import com.mednet.pacssocketoru.server.MllpFraming;

import java.io.IOException;
import java.net.Socket;

/**
 * Sends the sample ORU^R01 to localhost:6161 and prints the ACK.
 *
 * <pre>
 * mvn -q exec:java -Dexec.mainClass=com.mednet.pacssocketoru.client.Hl7TestClient
 * </pre>
 */
public final class Hl7TestClient {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Hl7MllpServer.DEFAULT_PORT;

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(15000);
            System.out.println("Connected to " + host + ":" + port);
            MllpFraming.writeMessage(socket.getOutputStream(), SampleHl7Messages.ORU_R01_V23_CONCATENATED);
            String ack = MllpFraming.readMessage(socket.getInputStream());
            System.out.println("ACK received:\n" + (ack == null ? "(empty)" : ack.replace('\r', '\n')));
        }
    }
}
