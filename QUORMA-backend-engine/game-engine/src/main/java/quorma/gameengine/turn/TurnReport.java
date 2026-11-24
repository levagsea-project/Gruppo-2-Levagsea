package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import quorma.gameengine.log.BoardSnapshot;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchResult;

/**
 * What happened in one turn, phase by phase.
 *
 * @param turn       the turn number that was played
 * @param attacks    the declared attacks, in resolution order
 * @param evaluation the hands and multipliers of Evaluate, fixed for the turn
 * @param opening    Evaluate and Declare: {@code HandEvaluated} for A and B, then one {@code TargetDeclared} per attack
 * @param cycle1     Cycle 1 events, stack resolutions included
 * @param cycle2     Cycle 2 events
 * @param refill     Refill events
 * @param closing    End of turn: its T2 and T26 effects, and {@code MatchEnded} if the match ended
 * @param board      the table at the end of the turn, after Refill
 * @param result     present only if the match ended at the end of this turn
 * @param decisive   telemetry: the suit synergy tiers that changed the outcome of a blow this turn (a kill, a survival
 *                   or the spill), per side; always among the tiers of {@code evaluation.synergies()}
 */
public record TurnReport(
        int turn,
        Evaluation evaluation,
        List<Attack> attacks,
        List<MatchEvent> opening,
        List<MatchEvent> cycle1,
        List<MatchEvent> cycle2,
        List<MatchEvent> refill,
        List<MatchEvent> closing,
        BoardSnapshot board,
        Optional<MatchResult> result,
        Synergies decisive) {

    public TurnReport {
        Objects.requireNonNull(evaluation, "evaluation");
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(decisive, "decisive");
        attacks = List.copyOf(attacks);
        opening = List.copyOf(opening);
        cycle1 = List.copyOf(cycle1);
        cycle2 = List.copyOf(cycle2);
        refill = List.copyOf(refill);
        closing = List.copyOf(closing);
    }

    /** Every event of the turn in the order it happened, phase after phase. */
    public List<MatchEvent> events() {
        List<MatchEvent> all = new ArrayList<>(opening);
        all.addAll(cycle1);
        all.addAll(cycle2);
        all.addAll(refill);
        all.addAll(closing);
        return List.copyOf(all);
    }
}
