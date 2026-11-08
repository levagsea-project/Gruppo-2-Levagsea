package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static quorma.gameengine.testkit.Attacks.hit;
import static quorma.gameengine.testkit.Attacks.noTarget;
import static quorma.gameengine.testkit.Evaluators.evaluation;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Cycle 1 (ruleset 9.2, 9.5 to 9.7): attacks on the opposite lane, all at the same time. Damage and spill come
 * from the state at the start of the cycle. Each {@code AttackResolved} is followed by a {@code CreatureDestroyed}
 * when the attack killed. All expected values are worked out by hand.
 *
 * <p>Side A is Adattati (Resistenza Umana) and side B is SEKTAC (Potenze Artificiali): no advantage between them,
 * so the modifier is 1000. A's hand multiplier is 1100 (Pair), B's is 1000 (High card), unless a test says otherwise.
 */
class Cycle1Test {

    private final TurnResolver resolver = TurnResolvers.noEvaluation();
    private final Evaluation pairAgainstHighCard = evaluation(hand(HandType.PAIR, 3), 1100, hand(HandType.HIGH_CARD, 3), 1000);

    private List<MatchEvent> cycle1(MatchState state, List<Attack> attacks) {
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, pairAgainstHighCard, attacks, log);
        return log;
    }

    private static MatchState match(String boardA, String boardB) {
        return Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, boardA), Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    @Test
    void aKillSpillsToThePlayerWithTheAttackersMultiplier() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/1500)");

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        // effective ATK 4000, target DEF 1500 before the blow: spill (4000 - 1500) * 1100 / 1000 = 2750
        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.A, 3, 3), 1, 4000, 1000, 1500, -2500, true, 2750, false),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        PlayerState b = state.player(Side.B);
        assertEquals(12000 - 2750, b.life());
        assertTrue(Boards.creature(b, 3).destroyed());
        assertEquals(-2500, Boards.creature(b, 3).currentDef());
        assertEquals(12000, state.player(Side.A).life());
        assertEquals(3000, Boards.creature(state.player(Side.A), 3).currentDef(), "the target did not attack");
    }

    @Test
    void aHitWithoutAKillDamagesButDoesNotSpill() {
        MatchState state = match("3:4V(1000/3000)", "3:9F(1000/3000)");

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        assertEquals(List.of(new AttackResolved(hit(Side.A, 3, 3), 1, 1000, 1000, 3000, 2000, false, 0, false)), log);
        assertEquals(12000, state.player(Side.B).life());
        assertFalse(Boards.creature(state.player(Side.B), 3).destroyed());
    }

    @Test
    void damagePersistsAndAnAtkEqualToDefKillsWithNoSpill() {
        MatchState state = match("3:4V(1000/3000)", "3:9F(1000/3000)");
        cycle1(state, List.of(hit(Side.A, 3, 3)));
        cycle1(state, List.of(hit(Side.A, 3, 3)));
        assertEquals(1000, Boards.creature(state.player(Side.B), 3).currentDef(), "3000 - 1000 - 1000");

        List<MatchEvent> third = cycle1(state, List.of(hit(Side.A, 3, 3)));

        // DEF 1000 before the blow, ATK 1000: it kills, and (1000 - 1000) * 1100 / 1000 = 0
        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.A, 3, 3), 1, 1000, 1000, 1000, 0, true, 0, false),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                third);
        assertEquals(12000, state.player(Side.B).life());
        assertTrue(Boards.creature(state.player(Side.B), 3).destroyed());
    }

    @Test
    void twoCreaturesThatKillEachOtherGiveNoSpillToEitherPlayer() {
        MatchState state = match("3:4V(4000/1500)", "3:9F(3000/2000)");

        // B (Potenze Artificiali) is declared before A in the same lane
        List<MatchEvent> log = cycle1(state, List.of(hit(Side.B, 3, 3), hit(Side.A, 3, 3)));

        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.B, 3, 3), 1, 3000, 1000, 1500, -1500, true, 0, true),
                        new CreatureDestroyed(Side.A, 3, "4V"),
                        new AttackResolved(hit(Side.A, 3, 3), 1, 4000, 1000, 2000, -2000, true, 0, true),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        assertEquals(12000, state.player(Side.A).life());
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void aCreatureKilledInTheCycleStillHitsBack() {
        // A3 dies to B3 and B3 dies to A3: both blows land because Cycle 1 is simultaneous
        MatchState state = match("3:4V(4000/1500)", "3:9F(3000/2000)");

        cycle1(state, List.of(hit(Side.B, 3, 3), hit(Side.A, 3, 3)));

        assertEquals(-2000, Boards.creature(state.player(Side.B), 3).currentDef());
        assertEquals(-1500, Boards.creature(state.player(Side.A), 3).currentDef());
    }

    @Test
    void whenOnlyOneOfTwoAttackersKillsItsSpillIsNormal() {
        // A3 survives B3's hit (DEF 5000 - 3000 = 2000); B3 dies to A3
        MatchState state = match("3:4V(4000/5000)", "3:9F(3000/2000)");

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.B, 3, 3), hit(Side.A, 3, 3)));

        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.B, 3, 3), 1, 3000, 1000, 5000, 2000, false, 0, false),
                        new AttackResolved(hit(Side.A, 3, 3), 1, 4000, 1000, 2000, -2000, true, 2200, false),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        // spill (4000 - 2000) * 1100 / 1000 = 2200
        assertEquals(12000 - 2200, state.player(Side.B).life());
        assertEquals(12000, state.player(Side.A).life());
    }

    @Test
    void anAttackOnAnotherLaneIsLeftForCycle2() {
        MatchState state = match("4:4V(4000/3000)", "1:9F(1000/1500)");

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 4, 1)));

        assertEquals(List.of(), log);
        assertEquals(1500, Boards.creature(state.player(Side.B), 1).currentDef());
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void anAttackWithoutATargetIsNotACycle1Attack() {
        MatchState state = match("2:4V(4000/3000)", "");

        assertEquals(List.of(), cycle1(state, List.of(noTarget(Side.A, 2))));
    }

    @Test
    void anAtkOfZeroDoesNoDamage() {
        MatchState state = match("3:4V(0/3000)", "3:9F(1000/1500)");

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        assertEquals(List.of(new AttackResolved(hit(Side.A, 3, 3), 1, 0, 1000, 1500, 1500, false, 0, false)), log);
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void theAttackersOwnMultiplierIsUsedNotTheDefenders() {
        // B (High card, 1000) kills A3: spill (4000 - 1500) * 1000 / 1000 = 2500, not 2750 with A's Pair
        MatchState state = match("3:4V(1000/1500)", "3:9F(4000/9000)");

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.B, 3, 3)));

        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.B, 3, 3), 1, 4000, 1000, 1500, -2500, true, 2500, false),
                        new CreatureDestroyed(Side.A, 3, "4V")),
                log);
        assertEquals(12000 - 2500, state.player(Side.A).life());
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void aStrongAttackerHitsHarderAndSpillsMore() {
        // Adattati are strong against MEROZYN: effective ATK 4000 * 1100 / 1000 = 4400
        MatchState state = Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, "3:4V(4000/3000)"),
                Boards.side(Subfaction.MEROZYN, 12000, "3:9F(1000/1500)"));

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        // spill (4400 - 1500) * 1100 / 1000 = 3190
        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.A, 3, 3), 1, 4400, 1100, 1500, -2900, true, 3190, false),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        assertEquals(12000 - 3190, state.player(Side.B).life());
    }

    @Test
    void aWeakAttackerHitsSofterAndSpillsLess() {
        // Adattati are weak against EFFRIX: effective ATK 4000 * 900 / 1000 = 3600
        MatchState state = Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, "3:4V(4000/3000)"),
                Boards.side(Subfaction.EFFRIX, 12000, "3:9F(1000/1500)"));

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        // spill (3600 - 1500) * 1100 / 1000 = 2310
        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.A, 3, 3), 1, 3600, 900, 1500, -2100, true, 2310, false),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        assertEquals(12000 - 2310, state.player(Side.B).life());
    }

    @Test
    void lifeCanGoBelowZeroInTheMiddleOfATurn() {
        MatchState state = Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, "3:4V(4000/3000)"),
                Boards.side(Subfaction.SEKTAC, 1000, "3:9F(1000/1500)"));

        cycle1(state, List.of(hit(Side.A, 3, 3)));

        assertEquals(1000 - 2750, state.player(Side.B).life(), "no clamping: the loss is checked at End of turn");
    }

    @Test
    void bothPlayersCanSpillInTheSameCycle() {
        MatchState state = match("1:4V(4000/3000) 2:4F(1000/2000)", "1:9F(1000/1500) 2:5S(3000/4000)");

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 1, 1), hit(Side.B, 2, 2)));

        // A1 kills B1: (4000 - 1500) * 1100 / 1000 = 2750. B2 kills A2: (3000 - 2000) * 1000 / 1000 = 1000
        assertEquals(
                List.of(
                        new AttackResolved(hit(Side.A, 1, 1), 1, 4000, 1000, 1500, -2500, true, 2750, false),
                        new CreatureDestroyed(Side.B, 1, "9F"),
                        new AttackResolved(hit(Side.B, 2, 2), 1, 3000, 1000, 2000, -1000, true, 1000, false),
                        new CreatureDestroyed(Side.A, 2, "4F")),
                log);
        assertEquals(12000 - 2750, state.player(Side.B).life());
        assertEquals(12000 - 1000, state.player(Side.A).life());
    }

    @Test
    void noAttacksNoEventsNoChange() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/1500)");

        assertEquals(List.of(), cycle1(state, List.of()));
        assertEquals(1500, Boards.creature(state.player(Side.B), 3).currentDef());
    }
}
