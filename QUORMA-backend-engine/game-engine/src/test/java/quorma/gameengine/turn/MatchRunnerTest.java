package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.Evaluators;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * The match loop: turns are played one after the other until a turn ends with a result. It always stops, because
 * turn 50 is the last. The hands come from stand-in evaluators.
 */
class MatchRunnerTest {

    /**
     * A match in which nothing ever happens: the stand-in evaluator gives both players no hand, so nobody attacks,
     * and both keep their creature and their life. Only the turn number moves.
     */
    private static MatchState quietMatch(int lifeA, int lifeB, int turn) {
        return Boards.match(
                Boards.side(Subfaction.ADATTATI, lifeA, "1:2V(1000/3000)"),
                Boards.side(Subfaction.SEKTAC, lifeB, "1:3F(1000/3000)"),
                turn);
    }

    private static final TurnResolver NOBODY_ATTACKS = TurnResolvers.with(Evaluators.sequence(HandResult.none()));

    @Test
    void aMatchThatEndsOnTheFirstTurnHasOneTurnReport() {
        // A1 reaches B3 in Cycle 2 and B has nothing left: the turn of TurnResolverTest, played by the runner
        MatchState state = Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, "1:4V(4000/3000) 3:7S(2000/5000)"),
                Boards.side(Subfaction.SEKTAC, 12000, "3:KS(1000/1500)"));
        var resolver = TurnResolvers.with(Evaluators.sequence(hand(HandType.HIGH_CARD, 1), hand(HandType.HIGH_CARD, 3)));

        MatchReport report = MatchRunner.run(state, resolver);

        MatchResult expected = new MatchResult(Outcome.A_WIN, EndReason.NO_CREATURES);
        assertEquals(1, report.turns().size());
        assertEquals(1, report.turns().get(0).turn());
        assertEquals(Optional.of(expected), report.turns().get(0).result());
        assertEquals(expected, report.result());
    }

    @Test
    void aQuietMatchRunsToTurn50AndMoreLifeWins() {
        MatchState state = quietMatch(12000, 11000, 1);

        MatchReport report = MatchRunner.run(state, NOBODY_ATTACKS);

        assertEquals(50, report.turns().size());
        assertEquals(new MatchResult(Outcome.A_WIN, EndReason.TURN_50_LIFE), report.result());
        for (int i = 0; i < 50; i++) {
            assertEquals(i + 1, report.turns().get(i).turn());
        }
        for (int i = 0; i < 49; i++) {
            assertEquals(Optional.empty(), report.turns().get(i).result(), "turn " + (i + 1) + " does not end the match");
        }
        assertEquals(Optional.of(report.result()), report.turns().get(49).result());
        assertEquals(50, state.turn(), "the counter stops on the last turn");
    }

    @Test
    void equalLifeAndEqualHandsEndInADrawOnTurn50() {
        MatchReport report = MatchRunner.run(quietMatch(9000, 9000, 1), NOBODY_ATTACKS);

        assertEquals(new MatchResult(Outcome.DRAW, EndReason.TURN_50_DRAW), report.result());
    }

    @Test
    void equalLifeAndABetterHandWinsOnTurn50() {
        var resolver = TurnResolvers.with(Evaluators.sequence(hand(HandType.HIGH_CARD), hand(HandType.PAIR)));
        // the hands have no attackers, so nothing happens; B has the better hand
        MatchReport report = MatchRunner.run(quietMatch(9000, 9000, 1), resolver);

        assertEquals(new MatchResult(Outcome.B_WIN, EndReason.TURN_50_HAND), report.result());
    }

    @Test
    void theRunnerStartsFromTheCurrentTurn() {
        MatchState state = quietMatch(12000, 11000, 48);

        MatchReport report = MatchRunner.run(state, NOBODY_ATTACKS);

        assertEquals(List.of(48, 49, 50), report.turns().stream().map(TurnReport::turn).toList());
        assertEquals(Outcome.A_WIN, report.result().outcome());
    }

    @Test
    void aMatchStartedOnTurn50PlaysOneTurn() {
        MatchReport report = MatchRunner.run(quietMatch(12000, 11000, 50), NOBODY_ATTACKS);

        assertEquals(1, report.turns().size());
        assertEquals(new MatchResult(Outcome.A_WIN, EndReason.TURN_50_LIFE), report.result());
    }
}
