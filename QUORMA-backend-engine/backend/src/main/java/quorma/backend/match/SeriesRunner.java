package quorma.backend.match;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.engine.EngineUnavailableException;
import quorma.backend.engine.HttpGameEngine;
import quorma.backend.engine.MatchLog;
import quorma.backend.telemetry.GameLog;
import quorma.backend.telemetry.Result;
import quorma.backend.telemetry.TelemetryPublisher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plays a whole series (best of three, factions swapped every game) and stores the recipe of every game.
 * No player decides anything during a match, so the series runs from start to finish in one call: the seeds are
 * drawn here, the engine plays each game, and the games, the series result and the notifications are saved.
 *
 * <p>It always runs inside the caller's transaction: if the engine fails halfway (or the time budget runs out),
 * the caller's transaction rolls back and nothing of the series stays.
 */
@Service
public class SeriesRunner {

    /** Total time for all the games of a series (2026-11-19-accept-locks-challenge). */
    static final Duration TIME_BUDGET = Duration.ofSeconds(60);

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final HttpGameEngine engine;
    private final MatchSeriesRepository series;
    private final MatchRepository matches;
    private final MatchNotificationRepository notifications;
    private final PoolSnapshotRepository snapshots;
    private final TelemetryPublisher telemetry;
    private final SecureRandom random = new SecureRandom();

    public SeriesRunner(HttpGameEngine engine, MatchSeriesRepository series, MatchRepository matches,
                        MatchNotificationRepository notifications, PoolSnapshotRepository snapshots,
                        TelemetryPublisher telemetry) {
        this.engine = engine;
        this.series = series;
        this.matches = matches;
        this.notifications = notifications;
        this.snapshots = snapshots;
        this.telemetry = telemetry;
    }

    /** One game as played: the stored recipe row and the log the engine returned (not stored). */
    public record PlayedMatch(Match match, JsonNode log) {
    }

    public record SeriesResult(MatchSeries series, List<PlayedMatch> games) {
    }

    /**
     * @param firstFactionA the faction player A plays in game 1; B has the opposite one, and they swap every game
     * @param decksA        player A's deck for each faction
     * @param decksB        player B's deck for each faction
     * @param viewerId      the player who gets the result in the HTTP response: their notification is created
     *                      already read, the other one unread
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SeriesResult run(SeriesKind kind, UUID challengeId, UUID playerA, UUID playerB, Faction firstFactionA,
                            Map<Faction, LockedDeck> decksA, Map<Faction, LockedDeck> decksB, UUID viewerId) {
        MatchSeries current = series.saveAndFlush(
                new MatchSeries(kind, SeriesFormat.FACTION_SWAP, challengeId, playerA, playerB, SeriesStatus.IN_PROGRESS));

        long started = System.nanoTime();
        List<MatchOutcome> outcomes = new ArrayList<>();
        List<PlayedMatch> games = new ArrayList<>();
        while (SeriesPlanner.needsNextGame(outcomes)) {
            int number = outcomes.size() + 1;
            Faction factionA = SeriesPlanner.factionOfA(firstFactionA, number);
            LockedDeck deckA = decksA.get(factionA);
            LockedDeck deckB = decksB.get(factionA.opposite());

            Duration left = TIME_BUDGET.minus(Duration.ofNanos(System.nanoTime() - started));
            if (left.isZero() || left.isNegative()) {
                throw new EngineUnavailableException("the series ran out of time");
            }
            long seed = random.nextLong();
            String rulesetVersion = snapshots.findById(deckA.poolSnapshotId()).orElseThrow().getRulesetVersion();
            MatchLog log = engine.simulate(input(seed, rulesetVersion, deckA, deckB), left);
            PlayedGame played = PlayedGame.parse(log, seed);

            Match match = matches.saveAndFlush(new Match(current.getId(), number, playerA, playerB,
                    deckA.deck(), deckB.deck(), deckA.poolSnapshotId(), deckB.poolSnapshotId(), seed,
                    played.rulesetVersion(), played.rulesetHash(), played.engineVersion(), played.prngName(),
                    played.shuffleAlgorithm(), played.resultHash(), played.outcome()));
            outcomes.add(played.outcome());
            games.add(new PlayedMatch(match, played.log()));
        }

        Instant now = Instant.now();
        current.setOutcome(SeriesPlanner.seriesOutcome(outcomes));
        current.setStatus(SeriesStatus.FINISHED);
        current.setFinishedAt(now);
        current = series.saveAndFlush(current);

        for (PlayedMatch game : games) {
            for (UUID player : List.of(playerA, playerB)) {
                notifications.save(new MatchNotification(new MatchNotificationId(game.match().getId(), player),
                        player.equals(viewerId) ? now : null));
            }
        }

        // Telemetry reads the logs after this transaction commits, in the background; it never throws.
        telemetry.publish(current.getId(), playerA, playerB, Result.valueOf(current.getOutcome().name()),
                games.stream().map(g -> new GameLog(g.match().getId(), g.match().getPoolSnapshotAId(),
                        g.match().getPoolSnapshotBId(), g.log())).toList());
        return new SeriesResult(current, games);
    }

    /** The engine's match input (match-input.schema.json): the seed, the ruleset and the two decks. */
    static JsonNode input(long seed, String rulesetVersion, LockedDeck deckA, LockedDeck deckB) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schema_version", 1);
        root.put("seed", seed);
        root.put("ruleset_version", rulesetVersion);
        ObjectNode players = root.putObject("players");
        players.set("A", player(deckA));
        players.set("B", player(deckB));
        return root;
    }

    private static ObjectNode player(LockedDeck deck) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("faction", deck.faction().name());
        node.put("pool_snapshot_id", deck.poolSnapshotId().toString());
        node.set("deck", MAPPER.readTree(deck.deck()));
        return node;
    }
}
