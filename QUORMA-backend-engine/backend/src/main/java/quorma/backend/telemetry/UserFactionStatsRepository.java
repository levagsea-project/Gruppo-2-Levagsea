package quorma.backend.telemetry;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFactionStatsRepository extends JpaRepository<UserFactionStats, UserFactionStatsId> {

    @Modifying
    @Query(value = """
            INSERT INTO user_faction_stats (user_id, faction, games_played, games_won)
            VALUES (:userId, :faction, :played, :won)
            ON CONFLICT (user_id, faction) DO UPDATE SET
                games_played = user_faction_stats.games_played + EXCLUDED.games_played,
                games_won = user_faction_stats.games_won + EXCLUDED.games_won,
                updated_at = now(),
                version = user_faction_stats.version + 1
            """, nativeQuery = true)
    void add(@Param("userId") UUID userId, @Param("faction") String faction, @Param("played") long played,
             @Param("won") long won);
}
