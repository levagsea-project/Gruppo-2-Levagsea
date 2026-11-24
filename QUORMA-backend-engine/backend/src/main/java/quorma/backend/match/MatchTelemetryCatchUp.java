package quorma.backend.match;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import quorma.backend.engine.EngineUnavailableException;
import quorma.backend.telemetry.GameLog;
import quorma.backend.telemetry.Result;
import quorma.backend.telemetry.SeriesFinishedEvent;
import quorma.backend.telemetry.TelemetryAggregator;
import quorma.backend.telemetry.TelemetryExtractor;

/**
 * Safety net of the telemetry: the log of a game is not stored, so a series whose in-memory event was lost (a restart,
 * a full queue, a failure) is rebuilt by replaying its stored recipe on the engine. It lives in the {@code match}
 * package because it needs the matches and the replay, and {@code telemetry} must not depend on {@code match}.
 *
 * <p>Not one transaction: the engine calls must not hold a connection (the aggregator opens its own). Aggregating is
 * idempotent, so running at the same time as the live path, or on two instances, never counts a game twice.
 * <ul>
 *   <li>A game played by another engine build or rule pack can never be replayed: the series is marked handled with
 *       no figures, so it is not tried again every run.</li>
 *   <li>The engine being down stops the run; the next run tries again.</li>
 *   <li>Any other failure skips the series and is logged; the newest series come first so one bad series never
 *       starves the others.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "telemetry.catch-up.enabled", havingValue = "true", matchIfMissing = true)
public class MatchTelemetryCatchUp {

    private static final Logger LOG = LoggerFactory.getLogger(MatchTelemetryCatchUp.class);

    private final MatchRepository matches;
    private final MatchSeriesRepository series;
    private final ReplayService replays;
    private final TelemetryAggregator aggregator;
    private final Duration grace;
    private final int batchSize;

    public MatchTelemetryCatchUp(MatchRepository matches, MatchSeriesRepository series, ReplayService replays,
                                 TelemetryAggregator aggregator,
                                 @Value("${telemetry.catch-up.grace:PT2M}") Duration grace,
                                 @Value("${telemetry.catch-up.batch-size:10}") int batchSize) {
        this.matches = matches;
        this.series = series;
        this.replays = replays;
        this.aggregator = aggregator;
        this.grace = grace;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${telemetry.catch-up.interval:PT5M}",
            initialDelayString = "${telemetry.catch-up.interval:PT5M}")
    public void run() {
        List<UUID> pending = matches.findSeriesIdsWithoutTelemetry(Instant.now().minus(grace),
                PageRequest.of(0, batchSize));
        for (UUID seriesId : pending) {
            try {
                catchUp(seriesId);
            } catch (EngineUnavailableException e) {
                LOG.warn("telemetry catch-up stopped, the engine is not available: {}", e.getMessage());
                return;
            } catch (RuntimeException e) {
                LOG.warn("telemetry catch-up skipped series {}: {}", seriesId, e.getMessage());
            }
        }
    }

    void catchUp(UUID seriesId) {
        MatchSeries s = series.findById(seriesId).orElse(null);
        List<Match> games = matches.findBySeriesIdOrderByGameNumber(seriesId);
        if (s == null || s.getStatus() != SeriesStatus.FINISHED || games.isEmpty()) {
            return;
        }
        List<GameLog> logs;
        try {
            logs = games.stream()
                    .map(m -> new GameLog(m.getId(), m.getPoolSnapshotAId(), m.getPoolSnapshotBId(),
                            replays.rebuild(m).log()))
                    .toList();
        } catch (ReplayUnavailableException e) {
            LOG.info("telemetry catch-up: series {} was played by another engine build or rule pack ({}), marked as handled",
                    seriesId, e.code());
            aggregator.markSkipped(games.stream().map(Match::getId).toList());
            return;
        }
        SeriesFinishedEvent event = TelemetryExtractor.series(seriesId, s.getPlayerAId(), s.getPlayerBId(),
                Result.valueOf(s.getOutcome().name()), logs);
        int added = aggregator.aggregate(event);
        if (added > 0) {
            LOG.info("telemetry catch-up: series {} aggregated ({} games)", seriesId, added);
        }
    }
}
