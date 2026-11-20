package quorma.backend.match;

import org.springframework.http.HttpStatus;

/** A challenge, room, matchmaking or match request that cannot be served. The message is an {@code ErrorCodes} code. */
public class ChallengeException extends RuntimeException {

    private final HttpStatus status;

    public ChallengeException(HttpStatus status, String code) {
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
