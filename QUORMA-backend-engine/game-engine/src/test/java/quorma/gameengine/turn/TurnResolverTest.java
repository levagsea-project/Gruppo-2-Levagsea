package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static quorma.gameengine.testkit.Attacks.hit;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.HandEvaluated;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.MatchEvent.MatchEnded;
import quorma.gameengine.log.MatchEvent.TargetDeclared;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.Evaluators;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * The whole turn, with the exact event list worked out by hand. The hands come from a stand-in evaluator
 * that returns A's hand and then B's on each turn ({@code Evaluators.sequence}), so these tests do not depend
 * on Task 1. Side A is Adattati (Resistenza Umana), side B is SEKTAC (Potenze Artificiali): no advantage.
 */
class TurnResolverTest {

    // ---- evaluate (ruleset 7) ----

    @Test
    void evaluateGivesEachPlayersCreaturesInLaneOrderAndLooksUpTheMultiplier() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "4:5S(1000/1000) 1:9V(1000/1000) 3:KF(1000/1000)");
        // B's creature in lane 2 is destroyed, and still counts for the hand (ruleset 7.9)
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "2:2T(1000/1000)! 5:JV(1000/1000)");
        var evaluator = new Evaluators.Recording(Evaluators.sequence(hand(HandType.PAIR, 1), hand(HandType.STRAIGHT, 2, 5)));

        Evaluation evaluation = TurnResolvers.with(evaluator).evaluate(Boards.match(a, b));

        assertEquals(
                List.of(
                        List.of(
                                new HandCard(1, 9, Suit.VINCOLO),
                                new HandCard(3, 13, Suit.FRATTURA),
                                new HandCard(4, 5, Suit.SPIRA)),
                        List.of(new HandCard(2, 2, Suit.VESTIGIO), new HandCard(5, 11, Suit.VINCOLO))),
                evaluator.calls(),
                "A first, then B, each in ascending lane order");
        assertEquals(hand(HandType.PAIR, 1), evaluation.a());
        assertEquals(hand(HandType.STRAIGHT, 2, 5), evaluation.b());
        assertEquals(1100, evaluation.multiplierA(), "Pair, from balance.json");
        assertEquals(1600, evaluation.multiplierB(), "Straight, from balance.json");
    }

    @Test
    void aBoardWithNoCreaturesHasNoHandAndNoSpill() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "1:2V(1000/1000)");
        var evaluator = Evaluators.sequence(HandResult.none(), hand(HandType.HIGH_CARD, 1));

        Evaluation evaluation = TurnResolvers.with(evaluator).evaluate(Boards.match(a, b));

        assertEquals(HandResult.none(), evaluation.a());
        assertEquals(0, evaluation.multiplierA());
        assertEquals(1000, evaluation.multiplierB());
    }

    // ---- a whole turn ----

    /** Turn 1, five creatures against five, everything in Cycle 1, then Refill. */
    private static MatchState fiveAgainstFive(int lifeB) {
        PlayerState a = Boards.side(
                Subfaction.ADATTATI,
                12000,
                "1:4V(4000/3000) 2:4F(3500/2500) 3:7S(2000/5000) 4:9T(1500/4000) 5:JV(1000/6000)",
                "KF(2200/3300) 8S(1000/1000)");
        PlayerState b = Boards.side(
                Subfaction.SEKTAC,
                lifeB,
                "1:2V(2500/1200) 2:3F(3000/4000) 3:KS(5000/2000) 4:5T(1000/1000) 5:6V(1000/1000)",
                "QT(2000/2600)");
        return Boards.match(a, b);
    }

    private static final HandResult PAIR_1_2 = hand(HandType.PAIR, 1, 2);
    private static final HandResult HIGH_CARD_3 = hand(HandType.HIGH_CARD, 3);

    /** The events of {@link #fiveAgainstFive(int)} up to the end of Refill, worked out by hand. */
    private static List<MatchEvent> fiveAgainstFiveEvents() {
        return List.of(
                // Evaluate: A has a Pair (1100), B has a High card (1000)
                new HandEvaluated(1, Side.A, PAIR_1_2, 1100),
                new HandEvaluated(1, Side.B, HIGH_CARD_3, 1000),
                // Declare: attackers by lane: A1, A2, B3, each on the opposite lane
                new TargetDeclared(hit(Side.A, 1, 1)),
                new TargetDeclared(hit(Side.A, 2, 2)),
                new TargetDeclared(hit(Side.B, 3, 3)),
                // Cycle 1. A1: ATK 4000 against DEF 1200 kills, spill (4000 - 1200) * 1100 / 1000 = 3080
                new AttackResolved(hit(Side.A, 1, 1), 1, 4000, 1000, 1200, -2800, true, 3080, false),
                new CreatureDestroyed(Side.B, 1, "2V"),
                // A2: ATK 3500 against DEF 4000 leaves 500, no spill
                new AttackResolved(hit(Side.A, 2, 2), 1, 3500, 1000, 4000, 500, false, 0, false),
                // B3: ATK 5000 against DEF 5000 kills, spill (5000 - 5000) * 1000 / 1000 = 0
                new AttackResolved(hit(Side.B, 3, 3), 1, 5000, 1000, 5000, 0, true, 0, false),
                new CreatureDestroyed(Side.A, 3, "7S"),
                // Cycle 2: nothing. Refill: B (Potenze Artificiali) first
                new CreatureDiscarded(Side.B, 1, "2V"),
                new CreatureDiscarded(Side.A, 3, "7S"),
                new CardDrawn(Side.B, 1, "QT"),
                new CardDrawn(Side.A, 3, "KF"));
    }

    private static TurnResolver fiveAgainstFiveResolver() {
        return TurnResolvers.with(Evaluators.sequence(PAIR_1_2, HIGH_CARD_3));
    }

    @Test
    void aFullTurnFromEvaluateToRefill() {
        MatchState state = fiveAgainstFive(12000);

        TurnReport report = fiveAgainstFiveResolver().playTurn(state);

        assertEquals(1, report.turn());
        assertEquals(Evaluators.evaluation(PAIR_1_2, 1100, HIGH_CARD_3, 1000), report.evaluation());
        assertEquals(List.of(hit(Side.A, 1, 1), hit(Side.A, 2, 2), hit(Side.B, 3, 3)), report.attacks());
        assertEquals(fiveAgainstFiveEvents(), report.events());
        assertEquals(Optional.empty(), report.result());

        PlayerState a = state.player(Side.A);
        PlayerState b = state.player(Side.B);
        assertEquals(12000, a.life());
        assertEquals(12000 - 3080, b.life());
        assertEquals(500, Boards.creature(b, 2).currentDef(), "damage stays");
        assertEquals("QT", Boards.creature(b, 1).card().id());
        assertEquals(2600, Boards.creature(b, 1).currentDef());
        assertEquals("KF", Boards.creature(a, 3).card().id());
        assertEquals(3300, Boards.creature(a, 3).currentDef());
        assertEquals(List.of("2V"), Boards.ids(b.discard()));
        assertEquals(List.of("7S"), Boards.ids(a.discard()));
        assertEquals(List.of("8S"), Boards.ids(a.deck()));
        assertEquals(List.of(), Boards.ids(b.deck()));
        assertEquals(2, state.turn(), "the match goes on, so the next turn starts");
    }

    @Test
    void aTurnWithTheSameInputGivesTheSameOutput() {
        TurnReport first = fiveAgainstFiveResolver().playTurn(fiveAgainstFive(12000));
        TurnReport second = fiveAgainstFiveResolver().playTurn(fiveAgainstFive(12000));

        assertEquals(first, second);
    }

    @Test
    void theHandsAreEvaluatedOnceEachBeforeAnyCombat() {
        MatchState state = fiveAgainstFive(12000);
        var evaluator = new Evaluators.Recording(Evaluators.sequence(PAIR_1_2, HIGH_CARD_3));

        TurnResolvers.with(evaluator).playTurn(state);

        assertEquals(2, evaluator.calls().size());
        // A's lane 3 creature (7 of Spira) is destroyed during the turn but was in the hand
        assertTrue(evaluator.calls().get(0).contains(new HandCard(3, 7, Suit.SPIRA)));
        assertEquals(5, evaluator.calls().get(0).size());
        assertEquals(5, evaluator.calls().get(1).size());
    }

    @Test
    void lifeBelowZeroEndsTheMatchOnlyAtTheEndOfTheTurn() {
        // same turn, but B has 1000 life: A1's spill takes it to 1000 - 3080 = -2080 and the turn still plays out
        MatchState state = fiveAgainstFive(1000);

        TurnReport report = fiveAgainstFiveResolver().playTurn(state);

        MatchResult result = new MatchResult(Outcome.A_WIN, EndReason.LIFE_ZERO);
        List<MatchEvent> expected = new ArrayList<>(fiveAgainstFiveEvents());
        expected.add(new MatchEnded(1, result));
        assertEquals(expected, report.events(), "Refill still happens before the match ends");
        assertEquals(Optional.of(result), report.result());
        assertEquals(1000 - 3080, state.player(Side.B).life());
        assertEquals(1, state.turn(), "a finished match does not move on to another turn");
    }

    // ---- a whole turn with Cycle 2 and the end of the match ----

    @Test
    void aTurnWithACycle2AttackAndTheEndOfTheMatch() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:4V(4000/3000) 3:7S(2000/5000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "3:KS(1000/1500)");
        MatchState state = Boards.match(a, b);
        HandResult handA = hand(HandType.HIGH_CARD, 1);
        HandResult handB = hand(HandType.HIGH_CARD, 3);

        TurnReport report = TurnResolvers.with(Evaluators.sequence(handA, handB)).playTurn(state);

        // A1 finds B's lane 1 empty and scans 1, 2, 3: its target is B3, in Cycle 2. B3 attacks A3 in Cycle 1.
        Optional<MatchResult> result = Optional.of(new MatchResult(Outcome.A_WIN, EndReason.NO_CREATURES));
        assertEquals(
                List.of(
                        new HandEvaluated(1, Side.A, handA, 1000),
                        new HandEvaluated(1, Side.B, handB, 1000),
                        new TargetDeclared(hit(Side.A, 1, 3)),
                        new TargetDeclared(hit(Side.B, 3, 3)),
                        // Cycle 1: B3 (ATK 1000) hits A3 (DEF 5000), no kill
                        new AttackResolved(hit(Side.B, 3, 3), 1, 1000, 1000, 5000, 4000, false, 0, false),
                        // Cycle 2: A1 (ATK 4000) kills B3 (DEF 1500), spill (4000 - 1500) * 1000 / 1000 = 2500
                        new AttackResolved(hit(Side.A, 1, 3), 2, 4000, 1000, 1500, -2500, true, 2500, false),
                        new CreatureDestroyed(Side.B, 3, "KS"),
                        // Refill: B's only creature is discarded and nothing is left to draw (both decks are empty)
                        new CreatureDiscarded(Side.B, 3, "KS"),
                        new LaneLeftEmpty(Side.B, 1),
                        new LaneLeftEmpty(Side.B, 2),
                        new LaneLeftEmpty(Side.B, 3),
                        new LaneLeftEmpty(Side.B, 4),
                        new LaneLeftEmpty(Side.B, 5),
                        new LaneLeftEmpty(Side.A, 2),
                        new LaneLeftEmpty(Side.A, 4),
                        new LaneLeftEmpty(Side.A, 5),
                        // End of turn: B has no creatures
                        new MatchEnded(1, result.orElseThrow())),
                report.events());
        assertEquals(result, report.result());
        assertEquals(12000 - 2500, b.life());
        assertEquals(4000, Boards.creature(a, 3).currentDef(), "A3 kept the damage of Cycle 1: 5000 - 1000");
        assertEquals(1, state.turn());
    }

    // ---- with the real evaluator ----

    @Test
    @Disabled("needs HandEvaluator.evaluate")
    void withTheRealEvaluatorAPairIsFoundAndItsMultiplierApplied() {
        PlayerState a = Boards.side(
                Subfaction.ADATTATI, 12000, "1:9V(1000/1000) 2:9F(1000/1000) 3:KS(1000/1000) 4:5T(1000/1000) 5:2V(1000/1000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "");

        Evaluation evaluation = new TurnResolver(TurnResolvers.RULES).evaluate(Boards.match(a, b));

        assertEquals(HandType.PAIR, evaluation.a().type());
        assertEquals(List.of(1, 2), evaluation.a().attackerLanes());
        assertEquals(1100, evaluation.multiplierA());
        assertEquals(HandResult.none(), evaluation.b());
        assertEquals(0, evaluation.multiplierB());
    }
}
