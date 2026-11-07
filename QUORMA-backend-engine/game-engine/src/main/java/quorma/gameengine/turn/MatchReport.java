package quorma.gameengine.turn;

import java.util.List;
import quorma.gameengine.model.MatchResult;

/** A whole match: every turn report, in order, and how it ended. */
public record MatchReport(List<TurnReport> turns, MatchResult result) {

    public MatchReport {
        turns = List.copyOf(turns);
    }
}
