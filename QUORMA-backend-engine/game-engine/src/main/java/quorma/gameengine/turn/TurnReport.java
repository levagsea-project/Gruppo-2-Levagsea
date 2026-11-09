package quorma.gameengine.turn;

import java.util.List;
import java.util.Optional;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchResult;

/**
 * What happened in one turn.
 *
 * @param turn   the turn number that was played
 * @param result present only if the match ended at the end of this turn
 */
public record TurnReport(
        int turn, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> events, Optional<MatchResult> result) {

    public TurnReport {
        attacks = List.copyOf(attacks);
        events = List.copyOf(events);
    }
}
