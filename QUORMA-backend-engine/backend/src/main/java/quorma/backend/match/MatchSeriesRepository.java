package quorma.backend.match;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchSeriesRepository extends JpaRepository<MatchSeries, UUID> {

    Optional<MatchSeries> findByChallengeId(UUID challengeId);

    /** The finished series a player took part in, newest first. */
    @Query(value = "select s from MatchSeries s where (s.playerAId = :userId or s.playerBId = :userId)"
            + " and s.status = quorma.backend.match.SeriesStatus.FINISHED order by s.finishedAt desc, s.id",
            countQuery = "select count(s) from MatchSeries s where (s.playerAId = :userId or s.playerBId = :userId)"
                    + " and s.status = quorma.backend.match.SeriesStatus.FINISHED")
    Page<MatchSeries> findFinishedOfPlayer(@Param("userId") UUID userId, Pageable pageable);

    /** The finished series with a result the player has not read yet (at least one unread notification), newest first. */
    @Query("select distinct s from MatchSeries s, Match m, MatchNotification n where m.seriesId = s.id"
            + " and n.id.matchId = m.id and n.id.userId = :userId and n.seenAt is null"
            + " and s.status = quorma.backend.match.SeriesStatus.FINISHED order by s.finishedAt desc")
    List<MatchSeries> findUnreadOfPlayer(@Param("userId") UUID userId);
}
