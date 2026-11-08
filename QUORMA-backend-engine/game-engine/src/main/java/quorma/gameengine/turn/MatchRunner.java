package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.model.MatchState;

/** Plays turns one after the other until the match has a result. Always ends: turn 50 is the last. */
public final class MatchRunner {

    private MatchRunner() {}

    /** Plays turns from the current turn of {@code state} until {@code resolver} reports a result. */
    public static MatchReport run(MatchState state, TurnResolver resolver) {
        List<TurnReport> turns = new ArrayList<>();
        TurnReport report;
        do {
            report = resolver.playTurn(state);
            turns.add(report);
        } while (report.result().isEmpty());
        return new MatchReport(turns, report.result().orElseThrow());
    }
}
