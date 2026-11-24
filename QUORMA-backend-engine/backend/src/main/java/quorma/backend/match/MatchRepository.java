package quorma.backend.match;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findBySeriesIdOrderByGameNumber(UUID seriesId);

    List<Match> findBySeriesIdInOrderBySeriesIdAscGameNumberAsc(Collection<UUID> seriesIds);

    /** Series with a game played before {@code before} that telemetry has not handled yet, newest first. */
    @Query("select m.seriesId from Match m where m.playedAt < :before and not exists"
            + " (select p.matchId from TelemetryProcessedMatch p where p.matchId = m.id)"
            + " group by m.seriesId order by max(m.playedAt) desc")
    List<UUID> findSeriesIdsWithoutTelemetry(@Param("before") Instant before, Pageable page);
}
