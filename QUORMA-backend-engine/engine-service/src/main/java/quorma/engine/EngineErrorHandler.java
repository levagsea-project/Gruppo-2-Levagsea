package quorma.engine;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import quorma.gameengine.input.MatchInputException;

@RestControllerAdvice
class EngineErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidInput(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MatchInputException.class)
    ProblemDetail invalidMatchInput(MatchInputException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
