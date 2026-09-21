# mednetPACSSocketORU

MLLP listener that forwards raw PACS ORU HL7 to the mednetExternalPACS receive API.

## 1. Run the jar

```bash
java -jar mednetPACSSocketORU.jar
```

Build it first with `mvn clean package`, which produces `target/mednetPACSSocketORU.jar`.

## 2. Set the properties file

Create `MednetPACSSocketORU.properties` at:

```
/usr/local/mednet/mednetFiles/config/propertiesFile/MednetPACSSocketORU.properties
```

```properties
pacs.name=Fuji
pacs.receiver.api.url=https://stage.mednetlabs.com/mednetExternalPACS/api/v1/report/receive
pacs.oru.mllp.port=6161
```

| Key                     | Required | Default | Description                                                  |
|-------------------------|----------|---------|--------------------------------------------------------------|
| `pacs.name`             | yes      | —       | Vendor name, appended as the path segment of the receive URL |
| `pacs.receiver.api.url` | yes      | —       | ExternalPACS receive base URL, without the PACS name         |
| `pacs.oru.mllp.port`    | no       | `6161`  | TCP port this listener binds on `0.0.0.0`                    |

`pacs.name` must be one of: `Centricity - GE`, `MedSynaptics`, `ImageBytes`, `Deeptek`, `Fuji`,
`Radspa`, `Puru PACS`.

To use a different file, pass `-Dpacs.socket.config=/path/to/file`.
