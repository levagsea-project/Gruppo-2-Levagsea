package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import quorma.backend.auth.JwtService;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckException;
import quorma.backend.engine.EngineUnavailableException;
import quorma.backend.engine.HttpGameEngine;
import quorma.backend.engine.MatchLog;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The challenge flow against a real PostgreSQL (migrations V1 to V9, triggers, jsonb, row locks) with a fake engine:
 * the recipe that is stored, repeated and concurrent accepts, engine faults, draws and the turn limit, rooms,
 * matchmaking, delivery of unread results and the authorization of the endpoints. Needs Docker: without it the
 * class is skipped, and the CI runs it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChallengeFlowDockerTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("engine.service.url", () -> "http://localhost:1");
    }

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String RULESET = "1.0-alpha";
    private static final Map<Faction, UUID> SNAPSHOT = new EnumMap<>(Faction.class);
    private static final Map<Faction, List<UUID>> CARDS = new EnumMap<>(Faction.class);

    @MockitoBean
    HttpGameEngine engine;

    @Autowired ChallengeService challenges;
    @Autowired RoomService rooms;
    @Autowired MatchmakingService matchmaking;
    @Autowired MatchService matchService;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @LocalServerPort int port;

    // -- seed ----------------------------------------------------------------------------------------------

    /** One snapshot and two creatures per faction, created once, in one transaction (snapshots are sealed at commit). */
    private synchronized void seedCatalogue() {
        if (!SNAPSHOT.isEmpty()) {
            return;
        }
        tx.executeWithoutResult(status -> {
            for (Faction f : Faction.values()) {
                boolean human = f == Faction.RESISTENZA_UMANA;
                UUID snapshot = jdbc.queryForObject(
                        "insert into pool_snapshots (faction, version_label, ruleset_version, content_hash)"
                                + " values (?, 'v1', ?, ?) returning id",
                        UUID.class, f.name(), RULESET, (human ? "1" : "2").repeat(64));
                SNAPSHOT.put(f, snapshot);
                List<UUID> cards = new ArrayList<>();
                for (int rank : new int[] {2, 3}) {
                    UUID card = jdbc.queryForObject(
                            "insert into cards (card_code, faction, kind, subfaction, suit, rank)"
                                    + " values (?, ?, 'CREATURE', ?, 'VINCOLO', ?) returning id",
                            UUID.class, (human ? "RU-T" : "PA-T") + rank, f.name(), human ? "ADATTATI" : "SEKTAC", rank);
                    jdbc.update("insert into card_definitions (snapshot_id, card_id, faction, kind, name, atk, def, abilities)"
                            + " values (?, ?, ?, 'CREATURE', ?, 4000, 7000, '[]'::jsonb)", snapshot, card, f.name(), "Carta " + rank);
                    cards.add(card);
                }
                CARDS.put(f, cards);
            }
        });
    }

    private UUID newUser(String prefix) {
        String name = prefix + UUID.randomUUID().toString().substring(0, 8);
        return jdbc.queryForObject("insert into users (username, email, password_hash, role)"
                + " values (?, ?, ?, 'PLAYER') returning id", UUID.class, name, name + "@test.it", "x".repeat(60));
    }

    /** A player with one legal deck per faction, both of them defense decks. */
    private Map<Faction, UUID> withDecks(UUID user) {
        Map<Faction, UUID> decks = new EnumMap<>(Faction.class);
        for (Faction f : Faction.values()) {
            UUID deck = jdbc.queryForObject("insert into decks (owner_id, name, faction, snapshot_id, is_defense)"
                    + " values (?, ?, ?, ?, true) returning id", UUID.class, user, "mazzo-" + f.name(), f.name(), SNAPSHOT.get(f));
            int rank = 2;
            for (UUID card : CARDS.get(f)) {
                UUID instance = jdbc.queryForObject("insert into card_instances (owner_id, card_id, origin_type)"
                        + " values (?, ?, 'STARTER') returning id", UUID.class, user, card);
                jdbc.update("insert into deck_cards (deck_id, card_instance_id, owner_id, card_id, faction, snapshot_id, deck_slot)"
                        + " values (?, ?, ?, ?, ?, ?, ?)", deck, instance, user, card, f.name(), SNAPSHOT.get(f), "C:VINCOLO/" + rank++);
            }
            decks.put(f, deck);
        }
        return decks;
    }

    private static List<DeckChoice> choices(Map<Faction, UUID> decks) {
        return decks.entrySet().stream().map(e -> new DeckChoice(e.getKey().name(), e.getValue())).toList();
    }

    @BeforeEach
    void freshEngine() {
        seedCatalogue();
        reset(engine);
    }

    // -- fake engine ---------------------------------------------------------------------------------------

    private static MatchLog log(String outcome, String reason, int turns) {
        ObjectNode doc = MAPPER.createObjectNode();
        ObjectNode header = doc.putObject("header");
        header.put("ruleset_version", RULESET);
        header.put("ruleset_hash", "c".repeat(64));
        header.put("engine_version", "1.0.0-test");
        header.put("prng", "SplitMix64");
        header.put("shuffle", "FISHER_YATES");
        ObjectNode result = doc.putObject("result");
        result.put("outcome", outcome);
        result.put("reason", reason);
        result.put("turns_played", turns);
        doc.put("result_hash", "d".repeat(64));
        return new MatchLog(doc);
    }

    private void engineAlways(String outcome, String reason, int turns) {
        when(engine.simulate(any(JsonNode.class), any(Duration.class))).thenReturn(log(outcome, reason, turns));
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    // -- the real engine ---------------------------------------------------------------------------------------

    /**
     * Plays a real series on a running engine-service, to check that the input the backend builds (deck snapshots,
     * ruleset version, seeds) is accepted and that the log has what the backend reads. Skipped unless
     * {@code -Dengine.url=http://localhost:8081} is given (start it with {@code docker compose up engine}).
     */
    @Test
    void aSeriesPlaysOnTheRealEngineWhenOneIsRunning() {
        String url = System.getProperty("engine.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "no real engine: pass -Dengine.url=http://localhost:8081");
        HttpGameEngine real = new HttpGameEngine(url, Duration.ofSeconds(30), Duration.ofSeconds(2));
        when(engine.simulate(any(JsonNode.class), any(Duration.class)))
                .thenAnswer(i -> real.simulate(i.getArgument(0), i.getArgument(1)));
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        UUID id = challenges.create(luca, new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA", choices(lucaDecks))).id();

        SeriesResponse series = challenges.accept(mattia, id, choices(mattiaDecks));

        assertEquals(SeriesStatus.FINISHED, series.status());
        assertTrue(series.games().size() >= 2 && series.games().size() <= 3, "games: " + series.games().size());
        for (GameResponse game : series.games()) {
            assertTrue(game.resultHash().matches("^[0-9a-f]{64}$"), game.resultHash());
            assertEquals(game.resultHash(), game.log().path("result_hash").asString());
            assertNotNull(game.outcome());
        }
        Map<String, Object> row = jdbc.queryForMap("select * from matches where series_id = ? and game_number = 1", series.id());
        assertTrue(row.get("ruleset_hash").toString().matches("^[0-9a-f]{64}$"));
        assertFalse(row.get("engine_version").toString().isBlank());
        assertEquals("SplitMix64", row.get("prng_name"));
    }

    // -- accept: the recipe ----------------------------------------------------------------------------------

    @Test
    void acceptStoresTheSnapshotsAndTheHashAndTheChallengeIsAccepted() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        engineAlways("A_WIN", "LIFE_ZERO", 9);

        ChallengeResponse created = challenges.create(luca, new CreateChallengeRequest(
                username(mattia).toUpperCase(), "RESISTENZA_UMANA", choices(lucaDecks)));
        assertEquals(ChallengeStatus.PENDING, created.status());
        assertTrue(created.code().startsWith("ch-"));
        assertEquals(2, count("select count(*) from challenge_decks where challenge_id = ? and side = 'CHALLENGER'", created.id()));

        SeriesResponse series = challenges.accept(mattia, created.id(), choices(mattiaDecks));

        assertEquals(SeriesStatus.FINISHED, series.status());
        assertEquals(MatchOutcome.A_WIN, series.outcome());
        assertEquals(2, series.games().size());
        assertNotNull(series.games().get(0).log());
        assertEquals("B", series.mySeat());
        assertEquals(ChallengeStatus.ACCEPTED, challenges.get(luca, created.id()).status());
        assertEquals(series.id(), challenges.get(luca, created.id()).seriesId());

        // the recipe is in the database: snapshots, seed, versions and the engine's hash
        Map<String, Object> game = jdbc.queryForMap("select * from matches where series_id = ? and game_number = 1", series.id());
        assertEquals("d".repeat(64), game.get("result_hash"));
        assertEquals("c".repeat(64), game.get("ruleset_hash"));
        assertEquals("1.0.0-test", game.get("engine_version"));
        assertEquals("SplitMix64", game.get("prng_name"));
        assertEquals("FISHER_YATES", game.get("shuffle_algorithm"));
        assertEquals(RULESET, game.get("ruleset_version"));
        assertEquals(SNAPSHOT.get(Faction.RESISTENZA_UMANA), game.get("pool_snapshot_a_id")); // luca started with RU
        assertEquals(SNAPSHOT.get(Faction.POTENZE_ARTIFICIALI), game.get("pool_snapshot_b_id"));
        assertTrue(game.get("deck_a").toString().contains("RU-T2"), "the deck snapshot holds the card definitions");
        assertTrue(game.get("deck_a").toString().contains("\"atk\""), game.get("deck_a").toString());
        assertEquals(luca, game.get("player_a_id"));
        // game 2 swaps the factions, not the seats
        Map<String, Object> second = jdbc.queryForMap("select * from matches where series_id = ? and game_number = 2", series.id());
        assertEquals(luca, second.get("player_a_id"));
        assertEquals(SNAPSHOT.get(Faction.POTENZE_ARTIFICIALI), second.get("pool_snapshot_a_id"));

        // matches are immutable
        assertThrows(Exception.class, () -> jdbc.update("update matches set outcome = 'DRAW' where series_id = ?", series.id()));
        assertThrows(Exception.class, () -> jdbc.update("delete from matches where series_id = ?", series.id()));

        // both players have a notification: the one who answered read it, the challenger finds it unread
        assertEquals(2, count("select count(*) from match_notifications n join matches m on m.id = n.match_id"
                + " where m.series_id = ? and n.user_id = ? and n.seen_at is not null", series.id(), mattia));
        assertEquals(2, count("select count(*) from match_notifications n join matches m on m.id = n.match_id"
                + " where m.series_id = ? and n.user_id = ? and n.seen_at is null", series.id(), luca));
    }

    @Test
    void theChallengerReadsTheUnreadResultOnceAndBothSeeTheHistory() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        engineAlways("B_WIN", "LIFE_ZERO", 12);
        UUID challengeId = challenges.create(luca, new CreateChallengeRequest(username(mattia), "POTENZE_ARTIFICIALI",
                choices(lucaDecks))).id();
        SeriesResponse played = challenges.accept(mattia, challengeId, choices(mattiaDecks));

        List<SeriesResponse> unread = matchService.list(luca, true, 0, 20);
        assertEquals(1, unread.size());
        assertEquals(played.id(), unread.get(0).id());
        assertTrue(unread.get(0).unread());
        assertEquals("A", unread.get(0).mySeat());
        assertEquals(MatchOutcome.B_WIN, unread.get(0).outcome());
        assertEquals(0, matchService.list(mattia, true, 0, 20).size(), "the player who got the response has nothing unread");

        assertFalse(matchService.markSeen(luca, played.id()).unread());
        assertEquals(0, matchService.list(luca, true, 0, 20).size());
        assertEquals(1, matchService.list(luca, false, 0, 20).size());
        assertEquals(1, matchService.list(mattia, false, 0, 20).size());

        ChallengeException stranger = assertThrows(ChallengeException.class, () -> matchService.get(newUser("x"), played.id()));
        assertEquals(ErrorCodes.MATCH_NOT_FOUND, stranger.code());
    }

    // -- repeated and concurrent accepts ---------------------------------------------------------------------

    @Test
    void sixAcceptsAtTheSameTimePlayOneSeries() throws Exception {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        when(engine.simulate(any(JsonNode.class), any(Duration.class))).thenAnswer(i -> {
            Thread.sleep(150); // keep the lock for a while, so the other requests really wait
            return log("A_WIN", "LIFE_ZERO", 9);
        });
        UUID challengeId = challenges.create(luca, new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA",
                choices(lucaDecks))).id();

        int requests = 6;
        CyclicBarrier start = new CyclicBarrier(requests);
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                Callable<String> accept = () -> {
                    start.await();
                    try {
                        challenges.accept(mattia, challengeId, choices(mattiaDecks));
                        return "played";
                    } catch (ChallengeException e) {
                        return e.code();
                    }
                };
                results.add(pool.submit(accept));
            }
            int played = 0;
            for (Future<String> r : results) {
                String result = r.get();
                if (result.equals("played")) {
                    played++;
                } else {
                    assertEquals(ErrorCodes.CHALLENGE_NOT_PENDING, result);
                }
            }
            assertEquals(1, played);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, count("select count(*) from match_series where challenge_id = ?", challengeId));
        assertEquals(2, count("select count(*) from matches m join match_series s on s.id = m.series_id where s.challenge_id = ?", challengeId));
        assertEquals(2, count("select count(*) from challenge_decks where challenge_id = ? and side = 'OPPONENT'", challengeId));
    }

    // -- engine faults ---------------------------------------------------------------------------------------

    @Test
    void anEngineFaultInTheMiddleOfTheSeriesLeavesNothingAndTheChallengeCanBeAcceptedAgain() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        UUID challengeId = challenges.create(luca, new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA",
                choices(lucaDecks))).id();
        when(engine.simulate(any(JsonNode.class), any(Duration.class)))
                .thenReturn(log("B_WIN", "LIFE_ZERO", 9))
                .thenThrow(new EngineUnavailableException("engine down"));

        assertThrows(EngineUnavailableException.class, () -> challenges.accept(mattia, challengeId, choices(mattiaDecks)));

        assertEquals(ChallengeStatus.PENDING, challenges.get(luca, challengeId).status());
        assertEquals(0, count("select count(*) from match_series where challenge_id = ?", challengeId));
        assertEquals(0, count("select count(*) from matches where player_a_id = ?", luca));
        assertEquals(0, count("select count(*) from challenge_decks where challenge_id = ? and side = 'OPPONENT'", challengeId));
        assertEquals(0, count("select count(*) from match_notifications where user_id = ?", luca));

        reset(engine);
        engineAlways("A_WIN", "LIFE_ZERO", 9);
        assertEquals(MatchOutcome.A_WIN, challenges.accept(mattia, challengeId, choices(mattiaDecks)).outcome());
    }

    @Test
    void aLogWithoutAValidHashStopsTheSeries() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        UUID challengeId = challenges.create(luca, new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA",
                choices(lucaDecks))).id();
        ObjectNode broken = (ObjectNode) log("A_WIN", "LIFE_ZERO", 9).document();
        broken.remove("result_hash");
        when(engine.simulate(any(JsonNode.class), any(Duration.class))).thenReturn(new MatchLog(broken));

        assertThrows(EngineUnavailableException.class, () -> challenges.accept(mattia, challengeId, choices(mattiaDecks)));

        assertEquals(ChallengeStatus.PENDING, challenges.get(luca, challengeId).status());
        assertEquals(0, count("select count(*) from match_series where challenge_id = ?", challengeId));
    }

    // -- scores, draws, turn limit ----------------------------------------------------------------------------

    @Test
    void oneAllPlaysAThirdGameAndADrawEverywhereIsADrawSeries() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        UUID first = challenges.create(luca, new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA", choices(lucaDecks))).id();
        when(engine.simulate(any(JsonNode.class), any(Duration.class)))
                .thenReturn(log("A_WIN", "LIFE_ZERO", 9)).thenReturn(log("B_WIN", "LIFE_ZERO", 9)).thenReturn(log("A_WIN", "LIFE_ZERO", 9));
        SeriesResponse oneAll = challenges.accept(mattia, first, choices(mattiaDecks));
        assertEquals(3, oneAll.games().size());
        assertEquals(MatchOutcome.A_WIN, oneAll.outcome());
        assertEquals(oneAll.games().get(0).factionA(), oneAll.games().get(2).factionA(), "game 3 has the factions of game 1");
        assertFalse(oneAll.games().get(0).factionA().equals(oneAll.games().get(1).factionA()));

        reset(engine);
        UUID second = challenges.create(luca, new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA", choices(lucaDecks))).id();
        engineAlways("DRAW", "TURN_LIMIT", 50);
        SeriesResponse draws = challenges.accept(mattia, second, choices(mattiaDecks));
        assertEquals(3, draws.games().size());
        assertEquals(MatchOutcome.DRAW, draws.outcome());
        assertEquals("DRAW", jdbc.queryForObject("select outcome from match_series where id = ?", String.class, draws.id()));
    }

    @Test
    void theTurnLimitResultComesFromTheEngineAndTwoNilEndsTheSeries() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        UUID id = challenges.create(luca, new CreateChallengeRequest(username(mattia), "POTENZE_ARTIFICIALI", choices(lucaDecks))).id();
        engineAlways("B_WIN", "TURN_LIMIT", 50);

        SeriesResponse series = challenges.accept(mattia, id, choices(mattiaDecks));

        assertEquals(2, series.games().size());
        assertEquals(MatchOutcome.B_WIN, series.outcome());
    }

    // -- rules around the challenge ---------------------------------------------------------------------------

    @Test
    void aDeckThatIsNotYoursAnOutdatedDeckAndAMissingDefenseAreRefused() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        String name = username(mattia);

        // mattia's decks are not luca's
        DeckException notMine = assertThrows(DeckException.class,
                () -> challenges.create(luca, new CreateChallengeRequest(name, "RESISTENZA_UMANA", choices(mattiaDecks))));
        assertEquals(ErrorCodes.DECK_NOT_FOUND, notMine.code());

        // a deck of the wrong faction in a slot
        List<DeckChoice> swapped = List.of(new DeckChoice("RESISTENZA_UMANA", lucaDecks.get(Faction.POTENZE_ARTIFICIALI)),
                new DeckChoice("POTENZE_ARTIFICIALI", lucaDecks.get(Faction.RESISTENZA_UMANA)));
        assertEquals(ErrorCodes.CHALLENGE_DECKS_INVALID, assertThrows(ChallengeException.class,
                () -> challenges.create(luca, new CreateChallengeRequest(name, "RESISTENZA_UMANA", swapped))).code());

        // an outdated defense deck blocks the player
        jdbc.update("update decks set outdated = true where id = ?", lucaDecks.get(Faction.RESISTENZA_UMANA));
        assertEquals(ErrorCodes.DECK_DEFENSE_REQUIRED, assertThrows(DeckException.class,
                () -> challenges.create(luca, new CreateChallengeRequest(name, "RESISTENZA_UMANA", choices(lucaDecks)))).code());
    }

    @Test
    void anExpiredChallengeBecomesExpiredForGoodAndCannotBeAccepted() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        UUID id = challenges.create(luca, new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA", choices(lucaDecks))).id();
        jdbc.update("update challenges set created_at = now() - interval '3 days', expires_at = now() - interval '1 day' where id = ?", id);

        assertEquals(ErrorCodes.CHALLENGE_EXPIRED,
                assertThrows(ChallengeException.class, () -> challenges.accept(mattia, id, choices(mattiaDecks))).code());

        assertEquals("EXPIRED", jdbc.queryForObject("select status from challenges where id = ?", String.class, id),
                "the EXPIRED status is kept even though the request failed");
        assertEquals(ErrorCodes.CHALLENGE_NOT_PENDING,
                assertThrows(ChallengeException.class, () -> challenges.accept(mattia, id, choices(mattiaDecks))).code());
    }

    @Test
    void onlyOnePendingChallengePerPairAndDeclineFreesIt() {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        withDecks(mattia);
        String name = username(mattia);
        UUID id = challenges.create(luca, new CreateChallengeRequest(name, "RESISTENZA_UMANA", choices(lucaDecks))).id();

        assertEquals(ErrorCodes.CHALLENGE_ALREADY_PENDING, assertThrows(ChallengeException.class,
                () -> challenges.create(luca, new CreateChallengeRequest(name, "RESISTENZA_UMANA", choices(lucaDecks)))).code());

        assertEquals(ChallengeStatus.DECLINED, challenges.decline(mattia, id).status());
        assertNotNull(challenges.create(luca, new CreateChallengeRequest(name, "RESISTENZA_UMANA", choices(lucaDecks))).id());
    }

    // -- rooms ------------------------------------------------------------------------------------------------

    @Test
    void aRoomIsJoinedByOnePlayerOnlyAndBecomesAnAcceptedChallenge() throws Exception {
        UUID creator = newUser("creator");
        UUID first = newUser("first");
        UUID second = newUser("second");
        Map<Faction, UUID> creatorDecks = withDecks(creator);
        Map<Faction, UUID> firstDecks = withDecks(first);
        Map<Faction, UUID> secondDecks = withDecks(second);
        when(engine.simulate(any(JsonNode.class), any(Duration.class))).thenAnswer(i -> {
            Thread.sleep(100);
            return log("A_WIN", "LIFE_ZERO", 9);
        });
        RoomResponse room = rooms.open(creator, new OpenRoomRequest("RESISTENZA_UMANA", choices(creatorDecks)));
        assertTrue(room.code().startsWith("rnd-"));
        assertEquals(1, rooms.list(first).stream().filter(r -> r.code().equals(room.code())).count());
        assertEquals(0, rooms.list(creator).stream().filter(r -> r.code().equals(room.code())).count(), "not listed to its creator");
        assertEquals(ErrorCodes.ROOM_ALREADY_OPEN, assertThrows(ChallengeException.class,
                () -> rooms.open(creator, new OpenRoomRequest("RESISTENZA_UMANA", choices(creatorDecks)))).code());
        RoomResponse beforeJoin = rooms.mine(creator); // what the creator sees after a page reload
        assertEquals(room.code(), beforeJoin.code());
        assertEquals(RoomStatus.OPEN, beforeJoin.status());
        assertEquals(ErrorCodes.ROOM_OWN_ROOM, assertThrows(ChallengeException.class,
                () -> rooms.join(creator, room.code(), choices(creatorDecks))).code());

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger played = new AtomicInteger();
        try {
            List<Future<String>> results = new ArrayList<>();
            for (var joiner : List.of(Map.entry(first, firstDecks), Map.entry(second, secondDecks))) {
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        rooms.join(joiner.getKey(), room.code(), choices(joiner.getValue()));
                        played.incrementAndGet();
                        return "played";
                    } catch (ChallengeException e) {
                        return e.code();
                    }
                }));
            }
            List<String> outcomes = new ArrayList<>();
            for (Future<String> r : results) {
                outcomes.add(r.get());
            }
            assertTrue(outcomes.contains("played") && outcomes.contains(ErrorCodes.ROOM_NOT_OPEN), outcomes.toString());
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, played.get());
        assertEquals("MATCHED", jdbc.queryForObject("select status from rooms where code = ?", String.class, room.code()));
        assertEquals("ACCEPTED", jdbc.queryForObject("select status from challenges where code = ?", String.class, room.code()));
        assertEquals(1, count("select count(*) from match_series s join challenges c on c.id = s.challenge_id where c.code = ?", room.code()));
        assertEquals(0, rooms.list(second).stream().filter(r -> r.code().equals(room.code())).count());
        RoomResponse afterJoin = rooms.mine(creator);
        assertEquals(RoomStatus.MATCHED, afterJoin.status());
        assertNotNull(afterJoin.seriesId(), "the creator finds the series that was played while waiting");
        assertEquals(1, matchService.list(creator, true, 0, 20).size());
    }

    @Test
    void aRoomThatNobodyJoinsExpires() {
        UUID creator = newUser("creator");
        UUID joiner = newUser("joiner");
        Map<Faction, UUID> creatorDecks = withDecks(creator);
        Map<Faction, UUID> joinerDecks = withDecks(joiner);
        RoomResponse room = rooms.open(creator, new OpenRoomRequest("POTENZE_ARTIFICIALI", choices(creatorDecks)));
        jdbc.update("update rooms set created_at = now() - interval '1 hour', expires_at = now() - interval '50 minutes' where code = ?", room.code());

        assertEquals(0, rooms.list(joiner).stream().filter(r -> r.code().equals(room.code())).count());
        assertEquals(ErrorCodes.ROOM_EXPIRED, assertThrows(ChallengeException.class,
                () -> rooms.join(joiner, room.code(), choices(joinerDecks))).code());
        assertEquals("EXPIRED", jdbc.queryForObject("select status from rooms where code = ?", String.class, room.code()));
        // and the creator can open a new one
        assertNotNull(rooms.open(creator, new OpenRoomRequest("POTENZE_ARTIFICIALI", choices(creatorDecks))).id());
    }

    // -- matchmaking ------------------------------------------------------------------------------------------

    @Test
    void matchmakingPlaysAgainstAnOfflineDefenderWhoGetsTheResultAtLogin() {
        UUID attacker = newUser("attacker");
        UUID defender = newUser("defender");
        Map<Faction, UUID> attackerDecks = withDecks(attacker);
        withDecks(defender);
        jdbc.update("update users set last_login_at = now() - interval '6 days' where id = ?", defender);
        // the other players of the other tests must not be picked: only the defender logged in recently
        jdbc.update("update users set last_login_at = null where id <> ?", defender);
        engineAlways("A_WIN", "LIFE_ZERO", 9);

        SeriesResponse series = matchmaking.play(attacker, new MatchmakingRequest("RESISTENZA_UMANA", choices(attackerDecks)));

        assertEquals(SeriesKind.MATCHMAKING, series.kind());
        assertEquals(defender, series.playerB().id());
        assertEquals(2, series.games().size());
        assertEquals(0, count("select count(*) from match_series where kind = 'MATCHMAKING' and challenge_id is not null"));
        List<SeriesResponse> unread = matchService.list(defender, true, 0, 20);
        assertEquals(1, unread.size());
        assertEquals("B", unread.get(0).mySeat());
        assertEquals(0, matchService.list(attacker, true, 0, 20).size());
    }

    @Test
    void matchmakingSkipsPlayersWhoWereAwayForMoreThanSevenDays() {
        UUID attacker = newUser("attacker");
        UUID away = newUser("away");
        Map<Faction, UUID> attackerDecks = withDecks(attacker);
        withDecks(away);
        jdbc.update("update users set last_login_at = null where id <> ?", away);
        jdbc.update("update users set last_login_at = now() - interval '8 days' where id = ?", away);
        engineAlways("A_WIN", "LIFE_ZERO", 9);

        ChallengeException e = assertThrows(ChallengeException.class,
                () -> matchmaking.play(attacker, new MatchmakingRequest("RESISTENZA_UMANA", choices(attackerDecks))));

        assertEquals(ErrorCodes.MATCHMAKING_NO_OPPONENT, e.code());
        assertEquals(0, count("select count(*) from match_series where player_a_id = ?", attacker));
    }

    // -- authorization over HTTP ------------------------------------------------------------------------------

    private HttpResponse<String> http(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String token(UUID user) {
        return jwt.issue(users.findById(user).orElseThrow());
    }

    @Test
    void theEndpointsAreForPlayersOnlyAndAStrangerGets404() throws Exception {
        UUID luca = newUser("luca");
        UUID mattia = newUser("mattia");
        Map<Faction, UUID> lucaDecks = withDecks(luca);
        Map<Faction, UUID> mattiaDecks = withDecks(mattia);
        UUID admin = newUser("admin");
        jdbc.update("update users set role = 'ADMIN' where id = ?", admin);
        engineAlways("A_WIN", "LIFE_ZERO", 9);

        for (String path : List.of("/challenges", "/rooms", "/matches")) {
            assertEquals(401, http("GET", path, null, null).statusCode(), path + " without a token");
            assertEquals(403, http("GET", path, token(admin), null).statusCode(), path + " as an admin");
            assertEquals(200, http("GET", path, token(luca), null).statusCode(), path + " as a player");
        }
        assertEquals(403, http("POST", "/matchmaking", token(admin), "{}").statusCode());

        String create = MAPPER.writeValueAsString(new CreateChallengeRequest(username(mattia), "RESISTENZA_UMANA", choices(lucaDecks)));
        HttpResponse<String> created = http("POST", "/challenges", token(luca), create);
        assertEquals(201, created.statusCode(), created.body());
        String id = MAPPER.readTree(created.body()).path("id").asString();

        HttpResponse<String> mine = http("GET", "/challenges/" + id, token(mattia), null);
        assertEquals(200, mine.statusCode());
        assertEquals("PENDING", MAPPER.readTree(mine.body()).path("status").asString());
        UUID stranger = newUser("stranger");
        HttpResponse<String> hidden = http("GET", "/challenges/" + id, token(stranger), null);
        assertEquals(404, hidden.statusCode());
        assertEquals(ErrorCodes.CHALLENGE_NOT_FOUND, MAPPER.readTree(hidden.body()).path("code").asString());

        // the challenger cannot accept their own challenge
        String accept = MAPPER.writeValueAsString(new DecksRequest(choices(lucaDecks)));
        assertEquals(404, http("POST", "/challenges/" + id + "/accept", token(luca), accept).statusCode());

        HttpResponse<String> played = http("POST", "/challenges/" + id + "/accept", token(mattia),
                MAPPER.writeValueAsString(new DecksRequest(choices(mattiaDecks))));
        assertEquals(200, played.statusCode(), played.body());
        JsonNode body = MAPPER.readTree(played.body());
        assertEquals("A_WIN", body.path("outcome").asString());
        assertEquals(2, body.path("games").size());
        assertEquals("d".repeat(64), body.path("games").get(0).path("resultHash").asString());
        assertTrue(body.path("games").get(0).has("log"));

        assertEquals(409, http("POST", "/challenges/" + id + "/accept", token(mattia),
                MAPPER.writeValueAsString(new DecksRequest(choices(mattiaDecks)))).statusCode(), "a second accept");

        HttpResponse<String> unread = http("GET", "/matches?unread=true", token(luca), null);
        assertEquals(1, MAPPER.readTree(unread.body()).size());
        assertFalse(MAPPER.readTree(unread.body()).get(0).path("games").get(0).has("log"), "the log is not stored");
        assertEquals(404, http("GET", "/matches/" + body.path("id").asString(), token(stranger), null).statusCode());

        HttpResponse<String> invalid = http("POST", "/challenges", token(luca), "{\"opponentUsername\":\"\"}");
        assertEquals(400, invalid.statusCode());
        assertEquals(ErrorCodes.VALIDATION_FAILED, MAPPER.readTree(invalid.body()).path("code").asString());
    }

    @Test
    void theOpenApiDocumentHasExplicitUniqueOperationIds() throws Exception {
        HttpResponse<String> response = http("GET", "/api-docs", null, null);
        assertEquals(200, response.statusCode());
        JsonNode paths = MAPPER.readTree(response.body()).path("paths");
        List<String> ids = new ArrayList<>();
        for (var path : paths.properties()) {
            for (var operation : path.getValue().properties()) {
                if (operation.getValue().has("operationId")) {
                    ids.add(operation.getValue().path("operationId").asString());
                }
            }
        }
        assertEquals(ids.size(), ids.stream().distinct().count(), "operationIds must be unique: " + ids);
        for (String expected : List.of("createChallenge", "listChallenges", "getChallenge", "acceptChallenge", "declineChallenge",
                "cancelChallenge", "openRoom", "listRooms", "getMyRoom", "joinRoom", "cancelRoom", "playMatchmaking",
                "listMatchSeries", "getMatchSeries", "markMatchSeriesSeen")) {
            assertTrue(ids.contains(expected), expected + " is missing from " + ids);
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "quorma-openapi.json"), response.body());
    }

    private String username(UUID user) {
        return jdbc.queryForObject("select username from users where id = ?", String.class, user);
    }
}
