package quorma.backend.match;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChallengeDeckRepository extends JpaRepository<ChallengeDeck, ChallengeDeckId> {

    @Query("select d from ChallengeDeck d where d.id.challengeId = :challengeId")
    List<ChallengeDeck> findByChallengeId(@Param("challengeId") UUID challengeId);
}
