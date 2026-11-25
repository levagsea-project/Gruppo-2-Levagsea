package quorma.backend.telemetry;

/** A match log that lacks something telemetry needs. */
public class TelemetryException extends RuntimeException {

    public TelemetryException(String message) {
        super(message);
    }
}
