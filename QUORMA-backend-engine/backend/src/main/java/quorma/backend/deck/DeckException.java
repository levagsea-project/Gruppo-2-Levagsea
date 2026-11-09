package quorma.backend.deck;

import java.util.List;
import org.springframework.http.HttpStatus;

/** A deck request that cannot be served. {@code errors} lists every broken rule. */
public class DeckException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> errors;

    public DeckException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public DeckException(HttpStatus status, String message, List<String> errors) {
        super(message);
        this.status = status;
        this.errors = List.copyOf(errors);
    }

    public HttpStatus status() {
        return status;
    }

    public List<String> errors() {
        return errors;
    }
}
