package quorma.backend.auth;

import org.springframework.http.HttpStatus;
import quorma.backend.common.ErrorCodes;

/** An authentication failure. The message is an {@link ErrorCodes} code. */
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(HttpStatus status, String code) {
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
