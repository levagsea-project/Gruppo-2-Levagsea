package quorma.backend.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import quorma.backend.catalogue.admin.AdminCatalogueController;
import quorma.backend.common.ErrorCodes;

/**
 * Errors carry a language-neutral {@code code}; the frontend translates it. The engine's own message (English,
 * for developers) and the technical reason go to the log, never to the client.
 */
@RestControllerAdvice(assignableTypes = {SimulationController.class, AdminCatalogueController.class},
        basePackages = "quorma.backend.match")
class EngineExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EngineExceptionHandler.class);

    @ExceptionHandler(EngineUnavailableException.class)
    ResponseEntity<ProblemDetail> unavailable(EngineUnavailableException e) {
        LOG.warn("Engine service unavailable: {}", e.getMessage(), e.getCause());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.ENGINE_UNAVAILABLE);
    }

    @ExceptionHandler(EngineRejectedException.class)
    ResponseEntity<ProblemDetail> rejected(EngineRejectedException e) {
        LOG.info("Engine rejected the input: {}", e.getMessage());
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.ENGINE_INPUT_INVALID);
    }

    private static ResponseEntity<ProblemDetail> respond(HttpStatus status, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, code);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
