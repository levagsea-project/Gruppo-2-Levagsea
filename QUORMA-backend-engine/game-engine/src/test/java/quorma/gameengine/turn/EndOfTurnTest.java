package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static quorma.gameengine.testkit.Evaluators.evaluation;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.MatchEnded;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * End of turn (ruleset 11). A player with no creatures, or at 0 life or less, loses; both losing is a draw.
 * After turn 50, if nobody lost: more life wins, then the better hand of the last Evaluate (the comparator
 * is a stand-in that only looks at the hand type), then a draw. {@code endOfTurn} does not move to the next turn:
 * {@code playTurn} does that when the match goes on.
 */
class EndOfTurnTest {

    private static final String ONE = "1:2V(1000/1000)";

    private final TurnResolver resolver = TurnResolvers.noEvaluation();

    /** Hands that do not matter (turns before 50, or a loss). */
    private final Evaluation anyHands = evaluation(hand(HandType.PAIR, 1), 1100, hand(HandType.HIGH_CARD, 1), 1000);

    private static MatchState state(int turn, int lifeA, String boardA, int lifeB, String boardB) {
        return Boards.match(
                Boards.side(Subfaction.ADATTATI, lifeA, boardA), Boards.side(Subfaction.SEKTAC, lifeB, boardB), turn);
    }

    private Optional<MatchResult> endOfTurn(MatchState state, Evaluation evaluation) {
        return resolver.endOfTurn(state, evaluation, new ArrayList<>());
    }

    private static MatchResult result(Outcome outcome, EndReason reason) {
        return new MatchResult(outcome, reason);
    }

    // ---- losing (11.1 to 11.3) ----

    @Test
    void theMatchGoesOnWhenBothPlayersHaveCreaturesAndLife() {
        MatchState s = state(1, 12000, ONE, 1, ONE);
        List<MatchEvent> log = new ArrayList<>();

        assertEquals(Optional.empty(), resolver.endOfTurn(s, anyHands, log));
        assertEquals(List.of(), log);
    }

    @Test
    void aPlayerWithNoCreaturesLoses() {
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.NO_CREATURES)),
                endOfTurn(state(1, 12000, "", 12000, ONE), anyHands));
        assertEquals(
                Optional.of(result(Outcome.A_WIN, EndReason.NO_CREATURES)),
                endOfTurn(state(1, 12000, ONE, 12000, ""), anyHands));
    }

    @Test
    void aPlayerAtZeroLifeLoses() {
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.LIFE_ZERO)),
                endOfTurn(state(1, 0, ONE, 12000, ONE), anyHands));
        assertEquals(
                Optional.of(result(Outcome.A_WIN, EndReason.LIFE_ZERO)),
                endOfTurn(state(1, 12000, ONE, 0, ONE), anyHands));
    }

    @Test
    void aPlayerBelowZeroLifeLoses() {
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.LIFE_ZERO)),
                endOfTurn(state(1, -500, ONE, 12000, ONE), anyHands));
    }

    @Test
    void oneLifePointIsStillAlive() {
        assertEquals(Optional.empty(), endOfTurn(state(1, 1, ONE, 1, ONE), anyHands));
    }

    @Test
    void bothPlayersLosingAtOnceIsADraw() {
        MatchResult draw = result(Outcome.DRAW, EndReason.BOTH_LOST);
        assertEquals(Optional.of(draw), endOfTurn(state(1, 0, ONE, -100, ONE), anyHands), "both at 0 life or less");
        assertEquals(Optional.of(draw), endOfTurn(state(1, 12000, "", 12000, ""), anyHands), "neither has creatures");
        assertEquals(Optional.of(draw), endOfTurn(state(1, 12000, "", 0, ONE), anyHands), "A has no creatures, B has no life");
        assertEquals(Optional.of(draw), endOfTurn(state(1, 0, ONE, 12000, ""), anyHands), "A has no life, B has no creatures");
    }

    @Test
    void aDestroyedCreatureStillInItsLaneIsNotADefenceAgainstLosing() {
        // after Refill no destroyed creature is left in a lane; if one is, it does not count as a creature in play
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.NO_CREATURES)),
                endOfTurn(state(1, 12000, "1:2V(1000/1000)!", 12000, ONE), anyHands));
    }

    // ---- turn 50 (11.4) ----

    @Test
    void turn49NeverEndsBecauseOfTheTurnNumber() {
        assertEquals(Optional.empty(), endOfTurn(state(49, 12000, ONE, 5000, ONE), anyHands));
    }

    @Test
    void afterTurn50MoreLifeWins() {
        assertEquals(
                Optional.of(result(Outcome.A_WIN, EndReason.TURN_50_LIFE)),
                endOfTurn(state(50, 9000, ONE, 8000, ONE), anyHands));
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.TURN_50_LIFE)),
                endOfTurn(state(50, 8000, ONE, 9000, ONE), anyHands));
    }

    @Test
    void moreLifeWinsEvenAgainstABetterHand() {
        // A has the worse hand but more life
        Evaluation aWorse = evaluation(hand(HandType.HIGH_CARD, 1), 1000, hand(HandType.PAIR, 1), 1100);
        assertEquals(
                Optional.of(result(Outcome.A_WIN, EndReason.TURN_50_LIFE)),
                endOfTurn(state(50, 9000, ONE, 8000, ONE), aWorse));
    }

    @Test
    void withEqualLifeTheBetterHandOfTheLastEvaluateWins() {
        Evaluation aBetter = evaluation(hand(HandType.PAIR, 1), 1100, hand(HandType.HIGH_CARD, 1), 1000);
        Evaluation bBetter = evaluation(hand(HandType.HIGH_CARD, 1), 1000, hand(HandType.FLUSH, 1, 2, 3, 4, 5), 1700);
        assertEquals(
                Optional.of(result(Outcome.A_WIN, EndReason.TURN_50_HAND)),
                endOfTurn(state(50, 7000, ONE, 7000, ONE), aBetter));
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.TURN_50_HAND)),
                endOfTurn(state(50, 7000, ONE, 7000, ONE), bBetter));
    }

    @Test
    void withEqualLifeAndEqualHandsItIsADraw() {
        Evaluation same = evaluation(hand(HandType.PAIR, 1), 1100, hand(HandType.PAIR, 2), 1100);
        assertEquals(
                Optional.of(result(Outcome.DRAW, EndReason.TURN_50_DRAW)),
                endOfTurn(state(50, 7000, ONE, 7000, ONE), same));
    }

    @Test
    void aPlayerWhoLostBeforeTheTurn50RulesLosesWhateverTheLifeOrTheHand() {
        // A has more life and the better hand, but no creatures: B wins by 11.1, not by the turn 50 rules
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.NO_CREATURES)),
                endOfTurn(state(50, 12000, "", 100, ONE), anyHands));
        assertEquals(
                Optional.of(result(Outcome.B_WIN, EndReason.LIFE_ZERO)),
                endOfTurn(state(50, 0, ONE, 100, ONE), anyHands));
    }

    // ---- events and the turn counter ----

    @Test
    void theEndOfTheMatchIsLogged() {
        MatchState s = state(7, 12000, "", 12000, ONE);
        List<MatchEvent> log = new ArrayList<>();

        Optional<MatchResult> result = resolver.endOfTurn(s, anyHands, log);

        assertEquals(List.of(new MatchEnded(7, result(Outcome.B_WIN, EndReason.NO_CREATURES))), log);
        assertEquals(result.orElseThrow(), ((MatchEnded) log.get(0)).result());
    }

    @Test
    void endOfTurnDoesNotMoveToTheNextTurn() {
        MatchState s = state(7, 12000, ONE, 12000, ONE);

        resolver.endOfTurn(s, anyHands, new ArrayList<>());

        assertEquals(7, s.turn(), "playTurn moves on, not endOfTurn");
        assertEquals(12000, s.player(Side.A).life());
    }
}
