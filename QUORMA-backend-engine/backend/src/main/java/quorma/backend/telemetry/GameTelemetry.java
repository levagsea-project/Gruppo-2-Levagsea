package quorma.backend.telemetry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What telemetry keeps from one game: the figures the engine summed in the match log, nothing derived by the
 * backend. The factions and hand counts per side are read from the log header and the turns.
 *
 * @param handsA turns in which player A had each hand type (the engine's constant names)
 * @param rulesetVersion the ruleset the game was played on (the log header); the key of {@code synergy_stats}
 * @param turnsTotal     the turns played ({@code totals.turns_total}), 0 when the log has no synergy figures
 * @param synergies      {@code totals.synergies}: empty for a log of an engine that did not write them
 */
public record GameTelemetry(UUID matchId, UUID snapshotIdA, UUID snapshotIdB, Result result, String factionA,
                            String factionB, SideTotals totalsA, SideTotals totalsB, Map<String, Integer> handsA,
                            Map<String, Integer> handsB, List<CardTotals> cards, String rulesetVersion,
                            long turnsTotal, List<SynergyTotals> synergies) {
}
