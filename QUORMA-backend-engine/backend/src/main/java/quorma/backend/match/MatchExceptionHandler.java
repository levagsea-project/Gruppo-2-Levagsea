package quorma.backend.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckException;
import quorma.backend.engine.EngineRejectedException;
import quorma.backend.engine.EngineUnavailableException;

/**
 * Errors of challenges, rooms, matchmaking and matches carry a language-neutral {@code code}; the frontend
 * translates it. The engine's own message goes to the log, never to the client.
 */
@RestControllerAdvice(basePackages = "quorma.backend.match")
class MatchExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MatchExceptionHandler.class);

    @ExceptionHandler(ChallengeException.class)
    ResponseEntity<ProblemDetail> challenge(ChallengeException e) {
        return respond(e.status(), e.code());
    }

    /** The deck rules ({@code deck.outdated}, {@code deck.defense_required}, {@code deck.not_found}). */
    @ExceptionHandler(DeckException.class)
    ResponseEntity<ProblemDetail> deck(DeckException e) {
        return respond(e.status(), e.code());
    }

    @ExceptionHandler(EngineUnavailableException.class)
    ResponseEntity<ProblemDetail> unavailable(EngineUnavailableException e) {
        LOG.warn("Engine service unavailable: {}", e.getMessage(), e.getCause());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.ENGINE_UNAVAILABLE);
    }

    @ExceptionHandler(EngineRejectedException.class)
    ResponseEntity<ProblemDetail> rejected(EngineRejectedException e) {
        LOG.warn("Engine rejected a match input: {}", e.getMessage());
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.ENGINE_INPUT_INVALID);
    }

    private static ResponseEntity<ProblemDetail> respond(HttpStatus status, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, code);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
