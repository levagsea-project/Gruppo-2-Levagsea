package quorma.backend.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class HttpGameEngineTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private HttpServer server;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void reply(int status, String body, long delayMillis) {
        server.createContext("/v1/simulate", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
    }

    private HttpGameEngine engine(Duration timeout) {
        return new HttpGameEngine("http://127.0.0.1:" + server.getAddress().getPort(), timeout, Duration.ofSeconds(2));
    }

    private static JsonNode json(String s) {
        return MAPPER.readTree(s);
    }

    @Test
    void returnsTheEngineLogUnchangedAndSendsTheInput() {
        String log = "{\"engine_version\":\"1.0\",\"events\":[{\"n\":1},{\"n\":2}]}";
        reply(200, log, 0);

        MatchLog result = engine(Duration.ofSeconds(5)).simulate(json("{\"seed\":42}"));

        assertEquals(json(log), result.document());
        assertEquals(json("{\"seed\":42}"), json(receivedBody.get()));
    }

    @Test
    void engine400BecomesRejectedWithTheEngineDetail() {
        reply(400, "{\"status\":400,\"detail\":\"input.seed: required\"}", 0);

        EngineRejectedException e = assertThrows(EngineRejectedException.class,
                () -> engine(Duration.ofSeconds(5)).simulate(json("{}")));

        assertEquals("input.seed: required", e.getMessage());
    }

    @Test
    void engine400WithoutProblemBodyFallsBackToGenericCode() {
        reply(400, "not json", 0);

        EngineRejectedException e = assertThrows(EngineRejectedException.class,
                () -> engine(Duration.ofSeconds(5)).simulate(json("{}")));

        assertEquals("engine.service.rejected_request", e.getMessage());
    }

    @Test
    void engine5xxBecomesUnavailable() {
        reply(500, "{\"status\":500}", 0);

        EngineUnavailableException e = assertThrows(EngineUnavailableException.class,
                () -> engine(Duration.ofSeconds(5)).simulate(json("{}")));

        assertEquals("engine.service.error_response", e.getMessage());
    }

    @Test
    void otherEngine4xxIsABackendBugNotACallerError() {
        reply(415, "", 0);

        assertThrows(EngineUnavailableException.class,
                () -> engine(Duration.ofSeconds(5)).simulate(json("{}")));
    }

    @Test
    void emptyBodyBecomesUnavailable() {
        reply(200, "", 0);

        EngineUnavailableException e = assertThrows(EngineUnavailableException.class,
                () -> engine(Duration.ofSeconds(5)).simulate(json("{}")));

        assertEquals("engine.service.empty_response", e.getMessage());
    }

    @Test
    void slowEngineTimesOut() {
        reply(200, "{}", 1500);

        EngineUnavailableException e = assertThrows(EngineUnavailableException.class,
                () -> engine(Duration.ofMillis(200)).simulate(json("{}")));

        assertEquals("engine.service.timeout", e.getMessage());
    }

    @Test
    void unreachableEngineBecomesUnavailable() {
        int port = server.getAddress().getPort();
        server.stop(0);
        HttpGameEngine engine = new HttpGameEngine("http://127.0.0.1:" + port, Duration.ofSeconds(5),
                Duration.ofSeconds(1));

        EngineUnavailableException e = assertThrows(EngineUnavailableException.class,
                () -> engine.simulate(json("{}")));

        assertTrue(e.getMessage().startsWith("engine.service."));
    }
}
