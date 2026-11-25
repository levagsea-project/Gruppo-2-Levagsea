package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.PoolSnapshot;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.engine.EngineRejectedException;
import quorma.backend.engine.EngineUnavailableException;
import quorma.backend.engine.HttpGameEngine;
import quorma.backend.engine.MatchLog;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** The series loop with a fake engine: scoring, third game, draws, turn limit, engine faults and notifications. */
class SeriesRunnerTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String HASH = "a".repeat(64);

    private final HttpGameEngine engine = mock(HttpGameEngine.class);
    private final MatchSeriesRepository series = mock(MatchSeriesRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final MatchNotificationRepository notifications = mock(MatchNotificationRepository.class);
    private final PoolSnapshotRepository snapshots = mock(PoolSnapshotRepository.class);
    private final SeriesRunner runner = new SeriesRunner(engine, series, matches, notifications, snapshots,
            mock(quorma.backend.telemetry.TelemetryPublisher.class));

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final Map<Faction, LockedDeck> decksA = decks("A");
    private final Map<Faction, LockedDeck> decksB = decks("B");

    @BeforeEach
    void wire() {
        when(series.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(matches.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        PoolSnapshot snapshot = mock(PoolSnapshot.class);
        when(snapshot.getRulesetVersion()).thenReturn("1.0-alpha");
        when(snapshots.findById(any())).thenReturn(Optional.of(snapshot));
    }

    private static Map<Faction, LockedDeck> decks(String tag) {
        Map<Faction, LockedDeck> map = new EnumMap<>(Faction.class);
        for (Faction f : Faction.values()) {
            map.put(f, new LockedDeck(f, UUID.randomUUID(), "[{\"id\":\"" + tag + "-" + f.name() + "\"}]"));
        }
        return map;
    }

    /** A log as the engine writes it, reduced to what the backend reads. */
    private static MatchLog log(String outcome, String reason, int turns) {
        ObjectNode doc = MAPPER.createObjectNode();
        ObjectNode header = doc.putObject("header");
        header.put("ruleset_version", "1.0-alpha");
        header.put("ruleset_hash", "b".repeat(64));
        header.put("engine_version", "1.0.0");
        header.put("prng", "SplitMix64");
        header.put("shuffle", "FISHER_YATES");
        ObjectNode result = doc.putObject("result");
        result.put("outcome", outcome);
        result.put("reason", reason);
        result.put("turns_played", turns);
        doc.put("result_hash", HASH);
        return new MatchLog(doc);
    }

    private void engineReturns(MatchLog... logs) {
        var stub = when(engine.simulate(any(JsonNode.class), any(Duration.class)));
        for (MatchLog l : logs) {
            stub = stub.thenReturn(l);
        }
    }

    private SeriesRunner.SeriesResult run() {
        return runner.run(SeriesKind.CHALLENGE, UUID.randomUUID(), a, b, Faction.RESISTENZA_UMANA, decksA, decksB, b);
    }

    @Test
    void twoToZeroStopsAfterTwoGames() {
        engineReturns(log("A_WIN", "LIFE_ZERO", 9), log("A_WIN", "LIFE_ZERO", 11));

        SeriesRunner.SeriesResult result = run();

        assertEquals(2, result.games().size());
        assertEquals(MatchOutcome.A_WIN, result.series().getOutcome());
        assertEquals(SeriesStatus.FINISHED, result.series().getStatus());
        assertTrue(result.series().getFinishedAt() != null);
        verify(engine, times(2)).simulate(any(JsonNode.class), any(Duration.class));
    }

    @Test
    void oneAllPlaysAThirdGameWithTheFactionsOfGameOne() {
        engineReturns(log("A_WIN", "LIFE_ZERO", 9), log("B_WIN", "LIFE_ZERO", 7), log("B_WIN", "LIFE_ZERO", 12));

        SeriesRunner.SeriesResult result = run();

        assertEquals(3, result.games().size());
        assertEquals(MatchOutcome.B_WIN, result.series().getOutcome());
        ArgumentCaptor<JsonNode> inputs = ArgumentCaptor.forClass(JsonNode.class);
        verify(engine, times(3)).simulate(inputs.capture(), any(Duration.class));
        List<JsonNode> sent = inputs.getAllValues();
        assertEquals("RESISTENZA_UMANA", sent.get(0).path("players").path("A").path("faction").asString());
        assertEquals("POTENZE_ARTIFICIALI", sent.get(0).path("players").path("B").path("faction").asString());
        assertEquals("POTENZE_ARTIFICIALI", sent.get(1).path("players").path("A").path("faction").asString());
        assertEquals("RESISTENZA_UMANA", sent.get(1).path("players").path("B").path("faction").asString());
        assertEquals("RESISTENZA_UMANA", sent.get(2).path("players").path("A").path("faction").asString());
    }

    @Test
    void eachGameUsesItsOwnSeedAndTheOwnDeckOfThePlayer() {
        engineReturns(log("A_WIN", "LIFE_ZERO", 9), log("B_WIN", "LIFE_ZERO", 7), log("DRAW", "TURN_LIMIT", 50));

        SeriesRunner.SeriesResult result = run();

        assertEquals(3, result.games().size());
        Match first = result.games().get(0).match();
        Match second = result.games().get(1).match();
        assertEquals(1, first.getGameNumber());
        assertEquals(2, second.getGameNumber());
        assertEquals(a, first.getPlayerAId());
        assertEquals(a, second.getPlayerAId()); // the seats never change
        assertEquals(decksA.get(Faction.RESISTENZA_UMANA).deck(), first.getDeckA());
        assertEquals(decksB.get(Faction.POTENZE_ARTIFICIALI).deck(), first.getDeckB());
        assertEquals(decksA.get(Faction.POTENZE_ARTIFICIALI).deck(), second.getDeckA());
        assertEquals(decksB.get(Faction.RESISTENZA_UMANA).deck(), second.getDeckB());
        assertEquals(HASH, first.getResultHash());
        assertEquals("SplitMix64", first.getPrngName());
    }

    @Test
    void aDrawInEveryGameIsADrawSeries() {
        engineReturns(log("DRAW", "TURN_LIMIT", 50), log("DRAW", "TURN_LIMIT", 50), log("DRAW", "TURN_LIMIT", 50));

        SeriesRunner.SeriesResult result = run();

        assertEquals(3, result.games().size());
        assertEquals(MatchOutcome.DRAW, result.series().getOutcome());
    }

    @Test
    void theTurnLimitResultOfTheEngineIsKeptAsItIs() {
        // At turn 50 the engine decides (ruleset 11.4); the backend does not reinterpret what it returns.
        engineReturns(log("B_WIN", "TURN_LIMIT", 50), log("B_WIN", "TURN_LIMIT", 50));

        SeriesRunner.SeriesResult result = run();

        assertEquals(MatchOutcome.B_WIN, result.series().getOutcome());
        assertEquals(MatchOutcome.B_WIN, result.games().get(0).match().getOutcome());
    }

    @Test
    void theViewerGetsAReadNotificationAndTheOtherPlayerAnUnreadOne() {
        engineReturns(log("A_WIN", "LIFE_ZERO", 9), log("A_WIN", "LIFE_ZERO", 9));

        run();

        ArgumentCaptor<MatchNotification> saved = ArgumentCaptor.forClass(MatchNotification.class);
        verify(notifications, times(4)).save(saved.capture());
        for (MatchNotification n : saved.getAllValues()) {
            if (n.getId().userId().equals(b)) {
                assertTrue(n.getSeenAt() != null, "the player who gets the response has it read");
            } else {
                assertNull(n.getSeenAt(), "the other player finds it unread at the next login");
            }
        }
    }

    @Test
    void anEngineFaultStopsTheSeriesAndLeavesNoNotification() {
        when(engine.simulate(any(JsonNode.class), any(Duration.class)))
                .thenReturn(log("A_WIN", "LIFE_ZERO", 9))
                .thenThrow(new EngineUnavailableException("down"));

        assertThrows(EngineUnavailableException.class, this::run);
        verify(notifications, never()).save(any());
    }

    @Test
    void anEngineRejectionIsNotSwallowed() {
        when(engine.simulate(any(JsonNode.class), any(Duration.class)))
                .thenThrow(new EngineRejectedException("players.A.deck: bad", null));

        assertThrows(EngineRejectedException.class, this::run);
        verify(matches, never()).saveAndFlush(any());
    }

    @Test
    void aLogWithoutAValidResultHashIsRefused() {
        ObjectNode broken = ((ObjectNode) log("A_WIN", "LIFE_ZERO", 9).document()).put("result_hash", "XYZ");
        when(engine.simulate(any(JsonNode.class), any(Duration.class))).thenReturn(new MatchLog(broken));

        assertThrows(EngineUnavailableException.class, this::run);
        verify(matches, never()).saveAndFlush(any());
    }

    @Test
    void aLogWithAnUnknownOutcomeIsRefused() {
        engineReturns(log("NOBODY", "LIFE_ZERO", 9));

        assertThrows(EngineUnavailableException.class, this::run);
    }

    @Test
    void theEngineGetsOnlyTheTimeThatIsLeft() {
        engineReturns(log("A_WIN", "LIFE_ZERO", 9), log("A_WIN", "LIFE_ZERO", 9));

        run();

        ArgumentCaptor<Duration> limits = ArgumentCaptor.forClass(Duration.class);
        verify(engine, times(2)).simulate(any(JsonNode.class), limits.capture());
        for (Duration d : limits.getAllValues()) {
            assertTrue(d.compareTo(SeriesRunner.TIME_BUDGET) <= 0 && !d.isNegative() && !d.isZero());
        }
        assertTrue(limits.getAllValues().get(1).compareTo(limits.getAllValues().get(0)) <= 0);
    }

    @Test
    void theSeedSentToTheEngineIsTheOneStored() {
        engineReturns(log("A_WIN", "LIFE_ZERO", 9), log("A_WIN", "LIFE_ZERO", 9));

        SeriesRunner.SeriesResult result = run();

        ArgumentCaptor<JsonNode> inputs = ArgumentCaptor.forClass(JsonNode.class);
        verify(engine, times(2)).simulate(inputs.capture(), any(Duration.class));
        for (int i = 0; i < 2; i++) {
            assertEquals(result.games().get(i).match().getSeed(), inputs.getAllValues().get(i).path("seed").asLong());
            assertEquals("1.0-alpha", inputs.getAllValues().get(i).path("ruleset_version").asString());
        }
    }
}
