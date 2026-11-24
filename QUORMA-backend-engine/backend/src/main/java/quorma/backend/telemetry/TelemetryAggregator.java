package quorma.backend.telemetry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardRepository;

/**
 * Adds the figures of a finished series to the statistics tables, globally and per player. One transaction per
 * series, and it is safe to run twice: a game is marked in {@code telemetry_processed_matches} first, and only the
 * games this run marked are added. Every counter is added in SQL ({@code x = x + n}), never read and written back.
 * If anything fails the whole series rolls back, so the tables never hold half a series.
 */
@Service
public class TelemetryAggregator {

    private final TelemetryProcessedMatchRepository processed;
    private final UserStatsRepository userStats;
    private final UserHandStatsRepository handStats;
    private final UserFactionStatsRepository factionStats;
    private final CardStatsRepository cardStats;
    private final UserCardStatsRepository userCardStats;
    private final CardRepository cards;

    public TelemetryAggregator(TelemetryProcessedMatchRepository processed, UserStatsRepository userStats,
                               UserHandStatsRepository handStats, UserFactionStatsRepository factionStats,
                               CardStatsRepository cardStats, UserCardStatsRepository userCardStats,
                               CardRepository cards) {
        this.processed = processed;
        this.userStats = userStats;
        this.handStats = handStats;
        this.factionStats = factionStats;
        this.cardStats = cardStats;
        this.userCardStats = userCardStats;
        this.cards = cards;
    }

    /** The running sums of one player in this series. */
    private static final class Figures {
        long played;
        long wins;
        long losses;
        long draws;
        long damage;
        long spill;
        long kills;
        final Map<String, Long> hands = new LinkedHashMap<>();
        final Map<String, long[]> factions = new LinkedHashMap<>(); // faction -> {played, won}
    }

    /** @return the number of games added (0 if the series had already been aggregated) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int aggregate(SeriesFinishedEvent event) {
        List<GameTelemetry> fresh = event.games().stream()
                .filter(g -> processed.markProcessed(g.matchId()) == 1)
                .toList();
        if (fresh.isEmpty()) {
            return 0;
        }
        boolean wholeSeries = fresh.size() == event.games().size();
        Map<String, UUID> cardIds = resolveCards(fresh);

        Figures a = new Figures();
        Figures b = new Figures();
        for (GameTelemetry game : fresh) {
            add(a, game, Side.A);
            add(b, game, Side.B);
            for (CardTotals card : game.cards()) {
                boolean isA = card.side() == Side.A;
                UUID player = isA ? event.playerAId() : event.playerBId();
                UUID snapshot = isA ? game.snapshotIdA() : game.snapshotIdB();
                UUID cardId = cardIds.get(card.cardCode());
                long won = game.result() == (isA ? Result.A_WIN : Result.B_WIN) ? 1 : 0;
                cardStats.add(snapshot, cardId, 1, won, card.timesDrawn(), card.damageDealt(), card.spillDamage(),
                        card.kills());
                userCardStats.add(player, cardId, 1, won, card.timesDrawn(), card.damageDealt(), card.spillDamage(),
                        card.kills());
            }
        }
        save(event.playerAId(), a, wholeSeries, event.seriesResult(), Side.A);
        save(event.playerBId(), b, wholeSeries, event.seriesResult(), Side.B);
        return fresh.size();
    }

    private static void add(Figures f, GameTelemetry game, Side side) {
        boolean isA = side == Side.A;
        SideTotals totals = isA ? game.totalsA() : game.totalsB();
        Result mine = isA ? Result.A_WIN : Result.B_WIN;
        f.played++;
        if (game.result() == Result.DRAW) {
            f.draws++;
        } else if (game.result() == mine) {
            f.wins++;
        } else {
            f.losses++;
        }
        f.damage += totals.damageDealt();
        f.spill += totals.spillDamage();
        f.kills += totals.kills();
        (isA ? game.handsA() : game.handsB()).forEach((hand, turns) -> f.hands.merge(hand, (long) turns, Long::sum));
        long[] faction = f.factions.computeIfAbsent(isA ? game.factionA() : game.factionB(), k -> new long[2]);
        faction[0]++;
        faction[1] += game.result() == mine ? 1 : 0;
    }

    private void save(UUID player, Figures f, boolean wholeSeries, Result seriesResult, Side side) {
        Result mine = side == Side.A ? Result.A_WIN : Result.B_WIN;
        long series = wholeSeries ? 1 : 0;
        long seriesWin = wholeSeries && seriesResult == mine ? 1 : 0;
        long seriesDraw = wholeSeries && seriesResult == Result.DRAW ? 1 : 0;
        long seriesLoss = series - seriesWin - seriesDraw;
        userStats.add(player, f.played, f.wins, f.losses, f.draws, f.damage, f.spill, f.kills, series, seriesWin,
                seriesLoss, seriesDraw);
        f.hands.forEach((hand, turns) -> handStats.add(player, hand, turns));
        f.factions.forEach((faction, n) -> factionStats.add(player, faction, n[0], n[1]));
    }

    /** card code -> cards.id for every card the games mention, in one query. */
    private Map<String, UUID> resolveCards(List<GameTelemetry> games) {
        Set<String> codes = new HashSet<>();
        games.forEach(g -> g.cards().forEach(c -> codes.add(c.cardCode())));
        Map<String, UUID> found = cards.findByCardCodeIn(codes).stream()
                .collect(Collectors.toMap(Card::getCardCode, Card::getId, (x, y) -> x, HashMap::new));
        for (String code : codes) {
            if (!found.containsKey(code)) {
                throw new TelemetryException("the match log names a card the catalogue does not have: " + code);
            }
        }
        return found;
    }
}
