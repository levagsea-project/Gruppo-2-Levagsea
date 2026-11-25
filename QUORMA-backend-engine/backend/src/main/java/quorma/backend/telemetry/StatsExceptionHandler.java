package quorma.backend.telemetry;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errors of the statistics endpoints (and of the profile of another player, which embeds them) carry a language-neutral {@code code}; the frontend translates it. */
@RestControllerAdvice(basePackages = {"quorma.backend.telemetry", "quorma.backend.profile"})
class StatsExceptionHandler {

    @ExceptionHandler(StatsException.class)
    ResponseEntity<ProblemDetail> stats(StatsException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(e.status(), e.code());
        pd.setProperty("code", e.code());
        return ResponseEntity.status(e.status()).body(pd);
    }
}
