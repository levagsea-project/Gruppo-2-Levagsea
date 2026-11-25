package quorma.backend.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SynergyStatsRepository extends JpaRepository<SynergyStats, SynergyStatsId> {

    @Modifying
    @Query(value = """
            INSERT INTO synergy_stats (ruleset_version, synergy_id, turns_total, turns_active, decisive_turns)
            VALUES (:rulesetVersion, :synergyId, :turnsTotal, :turnsActive, :decisiveTurns)
            ON CONFLICT (ruleset_version, synergy_id) DO UPDATE SET
                turns_total = synergy_stats.turns_total + EXCLUDED.turns_total,
                turns_active = synergy_stats.turns_active + EXCLUDED.turns_active,
                decisive_turns = synergy_stats.decisive_turns + EXCLUDED.decisive_turns,
                updated_at = now(),
                version = synergy_stats.version + 1
            """, nativeQuery = true)
    void add(@Param("rulesetVersion") String rulesetVersion, @Param("synergyId") String synergyId,
             @Param("turnsTotal") long turnsTotal, @Param("turnsActive") long turnsActive,
             @Param("decisiveTurns") long decisiveTurns);
}
