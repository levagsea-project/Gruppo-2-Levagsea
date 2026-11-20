package quorma.backend.match;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    /** SELECT ... FOR UPDATE: a second accept of the same challenge waits and then finds it no longer PENDING. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Challenge c where c.id = :id")
    Optional<Challenge> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCode(String code);

    boolean existsByChallengerIdAndOpponentIdAndStatus(UUID challengerId, UUID opponentId, ChallengeStatus status);

    /** The challenges a player sent or received, newest first. */
    @Query("select c from Challenge c where c.challengerId = :userId or c.opponentId = :userId order by c.createdAt desc")
    List<Challenge> findOfPlayer(@Param("userId") UUID userId);
}
