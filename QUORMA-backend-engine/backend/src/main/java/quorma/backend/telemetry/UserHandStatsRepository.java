package quorma.backend.telemetry;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserHandStatsRepository extends JpaRepository<UserHandStats, UserHandStatsId> {

    List<UserHandStats> findByIdUserId(UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO user_hand_stats (user_id, hand_type, turns) VALUES (:userId, :handType, :turns)
            ON CONFLICT (user_id, hand_type) DO UPDATE SET
                turns = user_hand_stats.turns + EXCLUDED.turns,
                updated_at = now(),
                version = user_hand_stats.version + 1
            """, nativeQuery = true)
    void add(@Param("userId") UUID userId, @Param("handType") String handType, @Param("turns") long turns);
}
