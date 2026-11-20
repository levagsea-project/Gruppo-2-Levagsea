package quorma.gameengine.turn;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.log.MatchEvent.MatchEnded;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/**
 * End of turn (ruleset 11): its T2 effects, the result if the match is over (defeat, turn 50), and otherwise the T26
 * effects and the end of what lasts until the end of the turn.
 */
final class TurnEnd {

    private TurnEnd() {}

    static Optional<MatchResult> endOfTurn(Game g, Comparator<HandResult> handComparator, List<MatchEvent> log) {
        MatchState state = g.state;
        Evaluation evaluation = g.evaluation;
        g.phase("Fine del turno", log);
        PlayerState a = state.player(Side.A);
        PlayerState b = state.player(Side.B);

        boolean aNoCreatures = a.creatures().stream().allMatch(Creature::destroyed);
        boolean bNoCreatures = b.creatures().stream().allMatch(Creature::destroyed);
        boolean aOut = aNoCreatures || a.life() <= 0;
        boolean bOut = bNoCreatures || b.life() <= 0;

        MatchResult result;

        if (aOut && bOut) {
            result = new MatchResult(Outcome.DRAW, EndReason.BOTH_LOST);

        } else if (aOut) {
            EndReason reason = aNoCreatures ? EndReason.NO_CREATURES : EndReason.LIFE_ZERO;
            result = new MatchResult(Outcome.B_WIN, reason);

        } else if (bOut) {
            EndReason reason = bNoCreatures ? EndReason.NO_CREATURES : EndReason.LIFE_ZERO;
            result = new MatchResult(Outcome.A_WIN, reason);

        } else if (state.turn() != MatchState.LAST_TURN) {
            g.fireAndResolve(List.of(new GameEvent.EndOfTurn()), log);
            g.expire(Lasting.END_OF_TURN);
            for (Side side : Side.values()) {
                state.player(side).creatures().forEach(Creature::newTurn);
            }
            return Optional.empty();

        } else if (a.life() != b.life()) {
            Outcome winner = a.life() > b.life() ? Outcome.A_WIN : Outcome.B_WIN;
            result = new MatchResult(winner, EndReason.TURN_50_LIFE);

        } else {
            int handComparison = handComparator.compare(evaluation.a(), evaluation.b());
            if (handComparison == 0) {
                result = new MatchResult(Outcome.DRAW, EndReason.TURN_50_DRAW);
            } else {
                Outcome winner = handComparison > 0 ? Outcome.A_WIN : Outcome.B_WIN;
                result = new MatchResult(winner, EndReason.TURN_50_HAND);
            }
        }

        log.add(new MatchEnded(state.turn(), result));
        return Optional.of(result);
    }
}
