package quorma.backend.engine;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Calls the engine-service and returns its MatchLog without interpreting or recomputing it. */
@Component
public class HttpGameEngine {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final WebClient client;
    private final Duration timeout;

    public HttpGameEngine(
            @Value("${engine.service.url}") String baseUrl,
            @Value("${engine.service.timeout:30s}") Duration timeout,
            @Value("${engine.service.connect-timeout:2s}") Duration connectTimeout) {
        this.timeout = timeout;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public MatchLog simulate(JsonNode input) {
        return simulate(input, timeout);
    }

    /** Same call with a shorter limit, for a caller that has a budget for several calls (never longer than the default). */
    public MatchLog simulate(JsonNode input, Duration limit) {
        Duration effective = limit.compareTo(timeout) < 0 ? limit : timeout;
        return client.post()
                .uri("/v1/simulate")
                .bodyValue(input)
                .retrieve()
                .bodyToMono(MatchLog.class)
                .timeout(effective)
                .switchIfEmpty(Mono.error(() -> new EngineUnavailableException("empty response from the engine service")))
                .onErrorMap(HttpGameEngine::translate)
                .block();
    }

    /** The engine's validation message (ProblemDetail.detail), so the caller can fix its input. */
    private static String detail(WebClientResponseException r) {
        try {
            JsonNode detail = MAPPER.readTree(r.getResponseBodyAsString()).path("detail");
            if (detail.isString() && !detail.asString().isBlank()) {
                return detail.asString();
            }
        } catch (RuntimeException ignored) {
            // not a ProblemDetail body: fall through to the generic code
        }
        return "the engine service rejected the request";
    }

    private static Throwable translate(Throwable e) {
        if (e instanceof EngineUnavailableException || e instanceof EngineRejectedException) {
            return e;
        }
        if (e instanceof WebClientResponseException r) {
            int status = r.getStatusCode().value();
            return status == 400 || status == 422
                    ? new EngineRejectedException(detail(r), r)
                    : new EngineUnavailableException("the engine service answered with an error", r);
        }
        if (e instanceof TimeoutException) {
            return new EngineUnavailableException("the engine service timed out", e);
        }
        return new EngineUnavailableException("the engine service could not be reached", e);
    }
}
