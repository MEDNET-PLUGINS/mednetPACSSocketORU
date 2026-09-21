package com.mednet.pacssocketoru.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads pacs name, receiver API URL, and MLLP port.
 * Search order: {@code -Dpacs.socket.config}, {@code MEDNET_PACS_SOCKET_CONFIG},
 * {@code /usr/local/mednet/mednetFiles/config/propertiesFile/MednetPACSSocketORU.properties},
 * then classpath {@code MednetPACSSocketORU.properties}.
 */
public final class PacsSocketProperties {

    public static final String DEFAULT_EXTERNAL_FILE =
            "/usr/local/mednet/mednetFiles/config/propertiesFile/MednetPACSSocketORU.properties";

    public static final int DEFAULT_PORT = 6161;

    private final String pacsName;
    private final String receiverApiUrl;
    private final int mllpPort;
    private final String loadedFrom;

    private PacsSocketProperties(String pacsName, String receiverApiUrl, int mllpPort, String loadedFrom) {
        this.pacsName = pacsName;
        this.receiverApiUrl = receiverApiUrl;
        this.mllpPort = mllpPort;
        this.loadedFrom = loadedFrom;
    }

    public static PacsSocketProperties load() throws IOException {
        Properties properties = new Properties();
        String loadedFrom = loadInto(properties);

        String pacsName = firstNonBlank(
                properties.getProperty("pacs.name"),
                properties.getProperty("pacsName"));
        String receiverApiUrl = firstNonBlank(
                properties.getProperty("pacs.receiver.api.url"),
                properties.getProperty("pacs.api.url"),
                properties.getProperty("api.url"));
        if (isBlank(pacsName)) {
            throw new IOException("pacs.name is required in " + loadedFrom);
        }
        if (isBlank(receiverApiUrl)) {
            throw new IOException("pacs.receiver.api.url is required in " + loadedFrom);
        }

        String portRaw = firstNonBlank(
                properties.getProperty("pacs.oru.mllp.port"),
                properties.getProperty("pacs.mllp.port"));
        int port = DEFAULT_PORT;
        if (!isBlank(portRaw)) {
            try {
                port = Integer.parseInt(portRaw.trim());
            } catch (NumberFormatException ex) {
                throw new IOException("Invalid pacs.oru.mllp.port: " + portRaw);
            }
        }
        return new PacsSocketProperties(pacsName.trim(), receiverApiUrl.trim(), port, loadedFrom);
    }

    private static String loadInto(Properties properties) throws IOException {
        String explicit = firstNonBlank(
                System.getProperty("pacs.socket.config"),
                System.getenv("MEDNET_PACS_SOCKET_CONFIG"));
        if (!isBlank(explicit)) {
            loadFile(properties, Path.of(explicit));
            return explicit;
        }
        Path defaultExternal = Path.of(DEFAULT_EXTERNAL_FILE);
        if (Files.isRegularFile(defaultExternal)) {
            loadFile(properties, defaultExternal);
            return DEFAULT_EXTERNAL_FILE;
        }
        try (InputStream in = PacsSocketProperties.class.getClassLoader()
                .getResourceAsStream("MednetPACSSocketORU.properties")) {
            if (in == null) {
                throw new IOException("MednetPACSSocketORU.properties not found on classpath");
            }
            properties.load(in);
            return "classpath:MednetPACSSocketORU.properties";
        }
    }

    private static void loadFile(Properties properties, Path path) throws IOException {
        try (InputStream in = new FileInputStream(path.toFile())) {
            properties.load(in);
        }
    }

    public String pacsName() {
        return pacsName;
    }

    public String receiverApiUrl() {
        return receiverApiUrl;
    }

    public int mllpPort() {
        return mllpPort;
    }

    public String loadedFrom() {
        return loadedFrom;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
