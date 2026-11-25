package quorma.backend.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import quorma.backend.config.AsyncConfig;

/**
 * Aggregates a series in the background. It starts only after the series committed (a series that rolls back leaves
 * no figures) and on its own thread (the response never waits). A failure is logged and never reaches the player.
 */
@Component
public class TelemetryListener {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryListener.class);

    private final TelemetryAggregator aggregator;

    public TelemetryListener(TelemetryAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Async(AsyncConfig.TELEMETRY_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeriesFinished(SeriesFinishedEvent event) {
        try {
            aggregator.aggregate(event);
        } catch (RuntimeException e) {
            LOG.warn("telemetry failed for series {}: {}", event.seriesId(), e.getMessage(), e);
        }
    }
}
