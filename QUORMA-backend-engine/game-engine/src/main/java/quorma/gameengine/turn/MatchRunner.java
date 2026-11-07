package quorma.gameengine.turn;

import quorma.gameengine.model.MatchState;

/** Plays turns one after the other until the match has a result. Always ends: turn 50 is the last. */
public final class MatchRunner {

    private MatchRunner() {}

    /** Plays turns from the current turn of {@code state} until {@code resolver} reports a result. */
    public static MatchReport run(MatchState state, TurnResolver resolver) {
        // TODO(task-2): loop playTurn until a result
        throw new UnsupportedOperationException("MatchRunner.run is not implemented yet");
    }
}
