package quorma.backend.telemetry;

import org.springframework.http.HttpStatus;

/** A statistics request that cannot be served. The message is an {@code ErrorCodes} code. */
public class StatsException extends RuntimeException {

    private final HttpStatus status;

    public StatsException(HttpStatus status, String code) {
        super(code);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return getMessage();
    }
}
