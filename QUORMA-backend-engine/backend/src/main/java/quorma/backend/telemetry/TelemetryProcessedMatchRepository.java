package quorma.backend.telemetry;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelemetryProcessedMatchRepository extends JpaRepository<TelemetryProcessedMatch, UUID> {

    /** Marks a match as aggregated; returns 0 when it already was (another run got there first). */
    @Modifying
    @Query(value = "INSERT INTO telemetry_processed_matches (match_id) VALUES (:matchId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int markProcessed(@Param("matchId") UUID matchId);
}
