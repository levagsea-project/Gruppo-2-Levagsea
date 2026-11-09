package quorma.backend.deck;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DeckController.class)
public class DeckExceptionHandler {

    @ExceptionHandler(DeckException.class)
    public ResponseEntity<ProblemDetail> handle(DeckException e) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
        if (!e.errors().isEmpty()) {
            body.setProperty("errors", e.errors());
        }
        return ResponseEntity.status(e.status()).body(body);
    }
}
