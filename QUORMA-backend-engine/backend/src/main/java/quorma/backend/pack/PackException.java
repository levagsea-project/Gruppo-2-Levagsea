package quorma.backend.pack;

import org.springframework.http.HttpStatus;

/** A pack request that cannot be served. */
public class PackException extends RuntimeException {

    private final HttpStatus status;

    public PackException(HttpStatus status, String code) {
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
