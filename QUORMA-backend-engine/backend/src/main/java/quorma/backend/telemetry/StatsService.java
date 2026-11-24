package quorma.backend.telemetry;

import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a player's statistics from the aggregation tables. Reading never waits for the background service. */
@Service
public class StatsService {

    private final UserStatsRepository userStats;
    private final UserHandStatsRepository handStats;
    private final UserFactionStatsRepository factionStats;

    public StatsService(UserStatsRepository userStats, UserHandStatsRepository handStats,
                        UserFactionStatsRepository factionStats) {
        this.userStats = userStats;
        this.handStats = handStats;
        this.factionStats = factionStats;
    }

    @Transactional(readOnly = true)
    public StatsResponse forUser(UUID userId) {
        UserStats s = userStats.findById(userId).orElse(null);
        var hands = handStats.findByIdUserId(userId).stream()
                .filter(h -> h.getTurns() > 0)
                .map(h -> new StatsResponse.HandCount(h.getId().handType(), h.getTurns()))
                .sorted(Comparator.comparingLong(StatsResponse.HandCount::turns).reversed()
                        .thenComparing(StatsResponse.HandCount::hand))
                .toList();
        var factions = factionStats.findByIdUserId(userId).stream()
                .map(f -> new StatsResponse.FactionRecord(f.getId().faction(), f.getGamesPlayed(), f.getGamesWon()))
                .sorted(Comparator.comparing(StatsResponse.FactionRecord::faction))
                .toList();
        if (s == null) {
            return new StatsResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, hands, factions);
        }
        return new StatsResponse(s.getSeriesPlayed(), s.getSeriesWins(), s.getSeriesLosses(), s.getSeriesDraws(),
                s.getMatchesPlayed(), s.getWins(), s.getDamageDealt(), s.getSpillDamage(), s.getKills(), hands, factions);
    }
}
