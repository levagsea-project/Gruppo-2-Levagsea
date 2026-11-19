package quorma.backend.catalogue.publish;

import org.springframework.http.HttpStatus;

/**
 * A catalogue request that cannot be served. The message is an {@code ErrorCodes} code. {@code report} is set when the
 * draft failed its check, so the caller can show every problem.
 */
public class CatalogueException extends RuntimeException {

    private final HttpStatus status;
    private final DraftReport report;

    public CatalogueException(HttpStatus status, String code) {
        this(status, code, null);
    }

    public CatalogueException(HttpStatus status, String code, DraftReport report) {
        super(code);
        this.status = status;
        this.report = report;
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
}
