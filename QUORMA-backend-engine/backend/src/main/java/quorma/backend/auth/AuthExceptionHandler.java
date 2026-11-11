package quorma.backend.auth;

import jakarta.validation.ConstraintViolation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errors carry a language-neutral {@code code}; the frontend translates it.
 * {@code detail} repeats the code so a client that ignores {@code code} still gets something stable.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    /** Constraint attributes that are values for the message ({max}, {min}, {value}); the rest is internal. */
    private static final Set<String> MESSAGE_PARAMS = Set.of("min", "max", "value");

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handle(AuthException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(e.status(), e.code());
        pd.setProperty("code", e.code());
        return ResponseEntity.status(e.status()).body(pd);
    }

    /** 400 with a {@code fieldErrors} map: field name to {@code {code, params}}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException e) {
        Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.computeIfAbsent(f.getField(), name -> {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", f.getDefaultMessage());
            Map<String, Object> params = new LinkedHashMap<>();
            if (f.contains(ConstraintViolation.class)) {
                ConstraintViolation<?> violation = f.unwrap(ConstraintViolation.class);
                violation.getConstraintDescriptor().getAttributes().forEach((k, v) -> {
                    if (MESSAGE_PARAMS.contains(k)) {
                        params.put(k, v);
                    }
                });
            }
            error.put("params", params);
            return error;
        }));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED);
        pd.setProperty("code", ErrorCodes.VALIDATION_FAILED);
        pd.setProperty("fieldErrors", fields);
        return ResponseEntity.badRequest().body(pd);
    }
}
