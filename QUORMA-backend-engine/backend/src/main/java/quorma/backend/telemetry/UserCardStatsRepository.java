package quorma.backend.telemetry;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCardStatsRepository extends JpaRepository<UserCardStats, UserCardStatsId> {

    List<UserCardStats> findByIdUserId(UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO user_card_stats (user_id, card_id, matches_played, matches_won, times_drawn, damage_dealt,
                                         spill_damage, kills)
            VALUES (:userId, :cardId, :played, :won, :drawn, :damage, :spill, :kills)
            ON CONFLICT (user_id, card_id) DO UPDATE SET
                matches_played = user_card_stats.matches_played + EXCLUDED.matches_played,
                matches_won = user_card_stats.matches_won + EXCLUDED.matches_won,
                times_drawn = user_card_stats.times_drawn + EXCLUDED.times_drawn,
                damage_dealt = user_card_stats.damage_dealt + EXCLUDED.damage_dealt,
                spill_damage = user_card_stats.spill_damage + EXCLUDED.spill_damage,
                kills = user_card_stats.kills + EXCLUDED.kills,
                updated_at = now(),
                version = user_card_stats.version + 1
            """, nativeQuery = true)
    void add(@Param("userId") UUID userId, @Param("cardId") UUID cardId, @Param("played") long played,
             @Param("won") long won, @Param("drawn") long drawn, @Param("damage") long damage,
             @Param("spill") long spill, @Param("kills") long kills);
}
