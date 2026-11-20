package quorma.backend.match;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * For every controller of the match package. A replay that cannot run on this engine is a 409: the request is fine, the
 * engine is the wrong one. The body carries the language-neutral {@code code} and what the match recorded
 * ({@code stored}) next to what is running ({@code running}), so whoever deploys can start the right engine version.
 */
@RestControllerAdvice(basePackages = "quorma.backend.match")
public class ReplayExceptionHandler {

    @ExceptionHandler(ReplayUnavailableException.class)
    public ResponseEntity<ProblemDetail> handle(ReplayUnavailableException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.code());
        pd.setProperty("code", e.code());
        pd.setProperty("stored", e.stored());
        pd.setProperty("running", e.running());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }
}
