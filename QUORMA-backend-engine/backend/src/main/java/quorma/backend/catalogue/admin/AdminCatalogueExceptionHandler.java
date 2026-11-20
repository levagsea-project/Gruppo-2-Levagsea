package quorma.backend.catalogue.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import quorma.backend.catalogue.publish.CatalogueException;
import quorma.backend.common.ErrorCodes;

/**
 * Errors carry a language-neutral {@code code}; the admin tool translates it. A draft that failed its check also carries
 * its {@code report}; {@code reason}, when there is one, is English for developers. The engine service's own errors
 * (unavailable, rejected) are handled by {@code EngineExceptionHandler}, which also covers this controller.
 */
@RestControllerAdvice(assignableTypes = AdminCatalogueController.class)
public class AdminCatalogueExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AdminCatalogueExceptionHandler.class);

    @ExceptionHandler(CatalogueException.class)
    public ResponseEntity<ProblemDetail> handle(CatalogueException e) {
        if (e.reason() != null) {
            LOG.info("catalogue request refused: {} ({})", e.code(), e.reason());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(e.status(), e.code());
        pd.setProperty("code", e.code());
        if (e.reason() != null) {
            pd.setProperty("reason", e.reason());
        }
        if (e.report() != null) {
            pd.setProperty("report", e.report());
        }
        return ResponseEntity.status(e.status()).body(pd);
    }

    /** A body that is not JSON, or a path value that is not a UUID or an item type. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> badRequest(Exception e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_ITEM_INVALID);
        pd.setProperty("code", ErrorCodes.CATALOGUE_ITEM_INVALID);
        return ResponseEntity.badRequest().body(pd);
    }
}
