package quorma.backend.deck;

import java.util.List;
import org.springframework.http.HttpStatus;

/** A deck request that cannot be served. The message is an {@code ErrorCodes} code; {@code errors} lists every broken rule. */
public class DeckException extends RuntimeException {

    private final HttpStatus status;
    private final List<DeckError> errors;

    public DeckException(HttpStatus status, String code) {
        this(status, code, List.of());
    }

    public DeckException(HttpStatus status, String code, List<DeckError> errors) {
        super(code);
        this.status = status;
        this.errors = List.copyOf(errors);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return getMessage();
    }

    public List<DeckError> errors() {
        return errors;
    }
}
