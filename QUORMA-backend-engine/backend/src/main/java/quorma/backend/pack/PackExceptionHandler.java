package quorma.backend.pack;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import quorma.backend.common.ErrorCodes;

/**
 * Errors carry a language-neutral {@code code}; the frontend translates it.
 */
@RestControllerAdvice(assignableTypes = PackController.class)
public class PackExceptionHandler {

    @ExceptionHandler(PackException.class)
    public ResponseEntity<ProblemDetail> handle(PackException e) {
        return respond(e.status(), e.code());
    }

    /** The only header and the only converted parameter of the controller is the Idempotency-Key. */
    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> handleBadKey(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.PACK_IDEMPOTENCY_KEY_INVALID);
    }

    private static ResponseEntity<ProblemDetail> respond(HttpStatus status, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, code);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
