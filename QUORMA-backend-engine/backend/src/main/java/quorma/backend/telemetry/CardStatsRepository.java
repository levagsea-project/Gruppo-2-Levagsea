package quorma.backend.telemetry;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardStatsRepository extends JpaRepository<CardStats, CardStatsId> {

    List<CardStats> findByIdSnapshotId(UUID snapshotId);

    @Modifying
    @Query(value = """
            INSERT INTO card_stats (snapshot_id, card_id, matches_played, matches_won, times_drawn, damage_dealt,
                                    spill_damage, kills)
            VALUES (:snapshotId, :cardId, :played, :won, :drawn, :damage, :spill, :kills)
            ON CONFLICT (snapshot_id, card_id) DO UPDATE SET
                matches_played = card_stats.matches_played + EXCLUDED.matches_played,
                matches_won = card_stats.matches_won + EXCLUDED.matches_won,
                times_drawn = card_stats.times_drawn + EXCLUDED.times_drawn,
                damage_dealt = card_stats.damage_dealt + EXCLUDED.damage_dealt,
                spill_damage = card_stats.spill_damage + EXCLUDED.spill_damage,
                kills = card_stats.kills + EXCLUDED.kills,
                updated_at = now(),
                version = card_stats.version + 1
            """, nativeQuery = true)
    void add(@Param("snapshotId") UUID snapshotId, @Param("cardId") UUID cardId, @Param("played") long played,
             @Param("won") long won, @Param("drawn") long drawn, @Param("damage") long damage,
             @Param("spill") long spill, @Param("kills") long kills);
}
