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

/** Calls the engine-service and returns its MatchLog without interpreting or recomputing it. */
@Component
public class HttpGameEngine {

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
        return client.post()
                .uri("/v1/simulate")
                .bodyValue(input)
                .retrieve()
                .bodyToMono(MatchLog.class)
                .timeout(timeout)
                .switchIfEmpty(Mono.error(() -> new EngineUnavailableException("engine.service.empty_response")))
                .onErrorMap(HttpGameEngine::translate)
                .block();
    }

    private static Throwable translate(Throwable e) {
        if (e instanceof EngineUnavailableException || e instanceof EngineRejectedException) {
            return e;
        }
        if (e instanceof WebClientResponseException r) {
            return r.getStatusCode().is4xxClientError()
                    ? new EngineRejectedException("engine.service.rejected_request", r)
                    : new EngineUnavailableException("engine.service.error_response", r);
        }
        if (e instanceof TimeoutException) {
            return new EngineUnavailableException("engine.service.timeout", e);
        }
        return new EngineUnavailableException("engine.service.unavailable", e);
    }
}
