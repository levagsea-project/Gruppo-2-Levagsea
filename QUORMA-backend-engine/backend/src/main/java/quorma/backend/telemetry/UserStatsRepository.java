package quorma.backend.telemetry;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {

    /**
     * Adds figures to a player's row, creating it if needed. One SQL statement adds to what the database holds, so
     * two series of the same player aggregating at once never lose an update (no read-modify-write).
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_stats (user_id, matches_played, wins, losses, draws, damage_dealt, spill_damage, kills,
                                    series_played, series_wins, series_losses, series_draws)
            VALUES (:userId, :played, :wins, :losses, :draws, :damage, :spill, :kills,
                    :seriesPlayed, :seriesWins, :seriesLosses, :seriesDraws)
            ON CONFLICT (user_id) DO UPDATE SET
                matches_played = user_stats.matches_played + EXCLUDED.matches_played,
                wins = user_stats.wins + EXCLUDED.wins,
                losses = user_stats.losses + EXCLUDED.losses,
                draws = user_stats.draws + EXCLUDED.draws,
                damage_dealt = user_stats.damage_dealt + EXCLUDED.damage_dealt,
                spill_damage = user_stats.spill_damage + EXCLUDED.spill_damage,
                kills = user_stats.kills + EXCLUDED.kills,
                series_played = user_stats.series_played + EXCLUDED.series_played,
                series_wins = user_stats.series_wins + EXCLUDED.series_wins,
                series_losses = user_stats.series_losses + EXCLUDED.series_losses,
                series_draws = user_stats.series_draws + EXCLUDED.series_draws,
                updated_at = now(),
                version = user_stats.version + 1
            """, nativeQuery = true)
    void add(@Param("userId") UUID userId, @Param("played") long played, @Param("wins") long wins,
             @Param("losses") long losses, @Param("draws") long draws, @Param("damage") long damage,
             @Param("spill") long spill, @Param("kills") long kills, @Param("seriesPlayed") long seriesPlayed,
             @Param("seriesWins") long seriesWins, @Param("seriesLosses") long seriesLosses,
             @Param("seriesDraws") long seriesDraws);
}
