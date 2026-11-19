package quorma.backend.match;

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
}
