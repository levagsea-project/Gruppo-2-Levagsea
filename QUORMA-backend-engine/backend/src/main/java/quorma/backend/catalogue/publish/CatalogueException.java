package quorma.backend.catalogue.publish;

import org.springframework.http.HttpStatus;

/**
 * A catalogue request that cannot be served. The message is an {@code ErrorCodes} code. {@code report} is set when the
 * draft failed its check, so the caller can show every problem; {@code reason} is an English explanation for the admin
 * tool's developers (never shown to a player), set for the errors that need one.
 */
public class CatalogueException extends RuntimeException {

    private final HttpStatus status;
    private final DraftReport report;
    private final String reason;

    public CatalogueException(HttpStatus status, String code) {
        this(status, code, null, null);
    }

    public CatalogueException(HttpStatus status, String code, String reason) {
        this(status, code, null, reason);
    }

    public CatalogueException(HttpStatus status, String code, DraftReport report) {
        this(status, code, report, null);
    }

    private CatalogueException(HttpStatus status, String code, DraftReport report, String reason) {
        super(code);
        this.status = status;
        this.report = report;
        this.reason = reason;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return getMessage();
    }

    /** The check report behind a {@code catalogue.draft_invalid}, or null. */
    public DraftReport report() {
        return report;
    }

    /** English explanation for developers, or null. */
    public String reason() {
        return reason;
    }
}
