package quorma.backend.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import quorma.backend.common.ErrorCodes;

class EngineExceptionHandlerTest {

    private final EngineExceptionHandler handler = new EngineExceptionHandler();

    @Test
    void rejectedInputIsA400WithACodeAndNoEngineText() {
        ResponseEntity<ProblemDetail> r = handler.rejected(new EngineRejectedException("input.seed: required", null));

        assertEquals(400, r.getStatusCode().value());
        assertEquals(ErrorCodes.ENGINE_INPUT_INVALID, r.getBody().getProperties().get("code"));
        assertEquals(ErrorCodes.ENGINE_INPUT_INVALID, r.getBody().getDetail());
    }

    @Test
    void unavailableEngineIsA503WithACodeAndNoTechnicalReason() {
        ResponseEntity<ProblemDetail> r = handler.unavailable(new EngineUnavailableException("the engine service timed out"));

        assertEquals(503, r.getStatusCode().value());
        assertEquals(ErrorCodes.ENGINE_UNAVAILABLE, r.getBody().getProperties().get("code"));
        assertEquals(ErrorCodes.ENGINE_UNAVAILABLE, r.getBody().getDetail());
    }
}
