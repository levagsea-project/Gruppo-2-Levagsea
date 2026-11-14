package quorma.gameengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.input.MatchInputException;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Target;
import quorma.gameengine.rng.Shuffle;
import quorma.gameengine.rng.SplitMix64;
import quorma.gameengine.turn.TurnReport;

/** A whole match from its input JSON: checks, shuffle, setup and turns. Expected values worked out by hand. */
class GameEngineTest {

    private static final GameEngine ENGINE = new GameEngine();

    private static String creature(String id, int rank, String suit, String subfaction, int atk, int def, String abilities) {
        return """
                {"id": "%s", "kind": "CREATURE", "rank": %d, "suit": "%s", "subfaction": "%s", "atk": %d, "def": %d,
                 "abilities": [%s]}""".formatted(id, rank, suit, subfaction, atk, def, abilities);
    }

    private static String input(long seed, String version, String deckA, String deckB) {
        return """
                {"schema_version": 1, "seed": %d, "ruleset_version": "%s", "players": {
                  "A": {"faction": "RESISTENZA_UMANA", "deck": [%s]},
                  "B": {"faction": "POTENZE_ARTIFICIALI", "deck": [%s]}}}""".formatted(seed, version, deckA, deckB);
    }

    private static final String ACE = creature("RU-VIN-A", 1, "VINCOLO", "ADATTATI", 4000, 7000,
            "{\"id\": \"K_INSAZIABILE\"}, {\"id\": \"K_TRABOCCANTE\"}, {\"id\": \"K_PERFORANTE\", \"params\": {\"x\": 30}}");
    private static final String KING = creature("PA-SPI-K", 13, "SPIRA", "SEKTAC", 3000, 5000,
            "{\"id\": \"K_SCUDO\", \"params\": {\"x\": 25}}");

    private static String refused(String json) {
        return assertThrows(MatchInputException.class, () -> ENGINE.simulate(json)).getMessage();
    }

    /**
     * The scenario of {@code schemas/match-log.example.json}. One creature each, both High card (1000), no
     * advantage between Adattati and SEKTAC. A hits through Scudo 25 with Perforante 30: mitigation 825, 3300
     * damage. Turn 1: B 5000 -> 1700, A 7000 -> 4000. Turn 2: A 4000 -> 1000; B 1700 -> -1600, killed; spill
     * multiplier 1000 x 1.2 = 1200, spill (3300 - 1700) x 1.2 = 1920; Insaziabile 4000 -> 4600. B has no creature
     * left: A wins.
     */
    @Test
    void theExampleMatch() {
        Simulation sim = ENGINE.simulate(input(42, "1.0-alpha", ACE, KING));

        assertEquals(12000, sim.startingLife());
        assertEquals(List.of("RU-VIN-A"), sim.drawOrder().get(Side.A));
        assertEquals(List.of(
                new CardDrawn(Side.A, 1, "RU-VIN-A"), new LaneLeftEmpty(Side.A, 2), new LaneLeftEmpty(Side.A, 3),
                new LaneLeftEmpty(Side.A, 4), new LaneLeftEmpty(Side.A, 5),
                new CardDrawn(Side.B, 1, "PA-SPI-K"), new LaneLeftEmpty(Side.B, 2), new LaneLeftEmpty(Side.B, 3),
                new LaneLeftEmpty(Side.B, 4), new LaneLeftEmpty(Side.B, 5)),
                sim.setupEvents());

        assertEquals(new MatchResult(Outcome.A_WIN, EndReason.NO_CREATURES), sim.report().result());
        assertEquals(2, sim.report().turns().size());

        CreatureRef a1 = new CreatureRef(Side.A, 1, "RU-VIN-A");
        CreatureRef b1 = new CreatureRef(Side.B, 1, "PA-SPI-K");
        Attack aHitsB = new Attack(Side.A, 1, Optional.of(new Target(Side.B, 1)));
        List<Modifier> throughScudo = List.of(
                new Modifier(b1, "K_SCUDO", "E28", Modifier.Kind.DAMAGE_REDUCTION_PCT, 25),
                new Modifier(a1, "K_PERFORANTE", "E56", Modifier.Kind.PIERCE_PCT, 30));

        assertEquals(new AttackResolved(aHitsB, 1, 4000, 1000, 4000, 825, 3300, 5000, 1700, false, false, 1000, 1000,
                0, throughScudo), attackBy(sim.report().turns().get(0), Side.A));

        TurnReport turn2 = sim.report().turns().get(1);
        List<Modifier> killing = new ArrayList<>(throughScudo);
        killing.add(new Modifier(a1, "K_TRABOCCANTE", "E21", Modifier.Kind.SPILL_MULTIPLIER, 1200));
        assertEquals(new AttackResolved(aHitsB, 1, 4000, 1000, 4000, 825, 3300, 1700, -1600, true, false, 1000, 1200,
                1920, killing), attackBy(turn2, Side.A));
        assertEquals(1000, attackBy(turn2, Side.B).defAfter(), "B hits A: 4000 - 3000");
        assertEquals(new EffectResolved(a1, "K_INSAZIABILE", "E1", a1,
                List.of(new StatChange(StatChange.Stat.ATK, 4000, 4600)), OptionalInt.of(1)),
                turn2.events().stream().filter(e -> e instanceof EffectResolved).findFirst().orElseThrow());
    }

    private static AttackResolved attackBy(TurnReport turn, Side side) {
        return turn.events().stream()
                .filter(e -> e instanceof AttackResolved a && a.attack().attacker() == side)
                .map(e -> (AttackResolved) e)
                .findFirst().orElseThrow();
    }

    private static String deck(String prefix, String suit, String subfaction) {
        List<String> cards = new ArrayList<>();
        for (int rank = 1; rank <= 10; rank++) {
            cards.add(creature(prefix + rank, rank, suit, subfaction, 1000 + rank * 100, 3000, ""));
        }
        return String.join(", ", cards);
    }

    @Test
    void decksAreShuffledAThenBWithTheSeed() {
        String deckA = deck("A", "VINCOLO", "ADATTATI");
        String deckB = deck("B", "SPIRA", "SEKTAC");

        Simulation sim = ENGINE.simulate(input(123, "1.0-alpha", deckA, deckB));

        SplitMix64 rng = new SplitMix64(123);
        List<String> idsA = List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10");
        List<String> idsB = idsA.stream().map(id -> "B" + id.substring(1)).toList();
        assertEquals(Shuffle.shuffled(idsA, rng), sim.drawOrder().get(Side.A));
        assertEquals(Shuffle.shuffled(idsB, rng), sim.drawOrder().get(Side.B), "B is shuffled second, same PRNG");
    }

    @Test
    void sameInputSameMatchAnotherSeedAnotherOrder() {
        String json = input(99, "1.0-alpha", deck("A", "VINCOLO", "ADATTATI"), deck("B", "SPIRA", "SEKTAC"));
        assertEquals(ENGINE.simulate(json), ENGINE.simulate(json));
        String other = json.replace("\"seed\": 99", "\"seed\": 100");
        assertTrue(!ENGINE.simulate(other).drawOrder().equals(ENGINE.simulate(json).drawOrder()));
    }

    @Test
    void theInputIsCheckedAgainstTheRuleset() {
        assertTrue(refused(input(1, "2.0", ACE, KING)).contains("input.ruleset_version: this engine plays 1.0-alpha, got 2.0"));
        assertTrue(refused(input(1, "1.0-alpha", ACE + ", " + KING, KING))
                .contains("players.A.deck[1]: card PA-SPI-K is POTENZE_ARTIFICIALI, the player is RESISTENZA_UMANA"));
        assertTrue(refused(input(1, "1.0-alpha", ACE + ", " + ACE, KING)).contains("players.A.deck[1].id: RU-VIN-A is already in this deck"));
        assertTrue(refused(input(1, "1.0-alpha", ACE, KING.replace("K_SCUDO\", \"params\": {\"x\": 25}", "K_LETALE\"")))
                .contains("players.B.deck[0].abilities: card PA-SPI-K K_LETALE: unknown ability"));
        String spell = "{\"id\": \"S\", \"kind\": \"SPELL\", \"faction\": \"POTENZE_ARTIFICIALI\", \"abilities\": [{\"id\": \"K_SCUDO\"}]}";
        assertTrue(refused(input(1, "1.0-alpha", ACE, KING + ", " + spell))
                .contains("players.B.deck[1].abilities: card S: spell and equipment effects are not implemented"));
    }

    @Test
    void aBlankSpellIsShuffledInAndDoesNothing() {
        String spell = "{\"id\": \"S\", \"kind\": \"SPELL\", \"faction\": \"POTENZE_ARTIFICIALI\", \"abilities\": []}";
        Simulation sim = ENGINE.simulate(input(5, "1.0-alpha", ACE, KING + ", " + spell));
        assertEquals(new MatchResult(Outcome.A_WIN, EndReason.NO_CREATURES), sim.report().result());
        List<MatchEvent> all = new ArrayList<>(sim.setupEvents());
        sim.report().turns().forEach(t -> all.addAll(t.events()));
        assertTrue(all.stream().anyMatch(e -> e instanceof MatchEvent.CardSetAside || e instanceof MatchEvent.SpellResolved),
                "the spell is set aside at setup or drawn in Refill");
    }
}
