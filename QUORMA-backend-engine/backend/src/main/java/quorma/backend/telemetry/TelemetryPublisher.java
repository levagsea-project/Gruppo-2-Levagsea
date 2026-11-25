package quorma.backend.telemetry;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Hands a finished series to telemetry. It runs inside the transaction that stores the series, so it must never
 * throw: a log telemetry cannot read is logged and skipped (the catch-up recovers it), and the series is saved anyway.
 */
@Component
public class TelemetryPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryPublisher.class);

    private final ApplicationEventPublisher events;

    public TelemetryPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void publish(UUID seriesId, UUID playerAId, UUID playerBId, Result seriesResult, List<GameLog> games) {
        try {
            events.publishEvent(TelemetryExtractor.series(seriesId, playerAId, playerBId, seriesResult, games));
        } catch (RuntimeException e) {
            LOG.warn("telemetry skipped for series {}: {}", seriesId, e.getMessage());
        }
    }
}
