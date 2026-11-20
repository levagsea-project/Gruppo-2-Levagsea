package quorma.backend.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

/** Base of the tests that call the API over HTTP with the real security. Skipped when Docker is not running. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class ApiTest extends IntegrationTest {

    @LocalServerPort
    protected int port;

    private final HttpClient http = HttpClient.newHttpClient();

    /** A status and a JSON body (an empty object when there is none). */
    public record Reply(int status, JsonNode body) {
        public String code() {
            return body.path("code").asString(null);
        }
    }

    protected Reply call(String method, String path, String token, String body, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json");
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        headers.forEach(b::header);
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Reply(r.statusCode(), r.body().isBlank() ? BaseSet.MAPPER.createObjectNode() : BaseSet.MAPPER.readTree(r.body()));
    }

    protected Reply call(String method, String path, String token, String body) throws Exception {
        return call(method, path, token, body, Map.of());
    }
}
