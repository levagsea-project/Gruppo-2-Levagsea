package quorma.backend.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class EngineExceptionHandlerTest {

    private final EngineExceptionHandler handler = new EngineExceptionHandler();

    @Test
    void rejectedInputIsA400WithTheEngineDetail() {
        ProblemDetail p = handler.rejected(new EngineRejectedException("input.seed: required", null));

        assertEquals(400, p.getStatus());
        assertEquals("input.seed: required", p.getDetail());
    }

    @Test
    void unavailableEngineIsA503() {
        ProblemDetail p = handler.unavailable(new EngineUnavailableException("engine.service.timeout"));

        assertEquals(503, p.getStatus());
        assertEquals("engine.service.timeout", p.getDetail());
    }
}
