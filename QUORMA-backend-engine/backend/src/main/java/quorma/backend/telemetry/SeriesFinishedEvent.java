package quorma.backend.telemetry;

import java.util.List;
import java.util.UUID;

/**
 * A series that was played and stored. Published inside the transaction that stores it; the aggregation runs after
 * it commits (so a series that rolls back leaves no figures). It carries the figures, not the logs.
 */
public record SeriesFinishedEvent(UUID seriesId, UUID playerAId, UUID playerBId, Result seriesResult,
                                  List<GameTelemetry> games) {
}
