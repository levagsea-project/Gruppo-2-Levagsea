package quorma.gameengine.model;

import java.util.Objects;

/** The whole match at one moment: both players and the turn number. */
public final class MatchState {

    /** The match ends at the end of this turn at the latest (ruleset 11.4). */
    public static final int LAST_TURN = 50;

    private final PlayerState a;
    private final PlayerState b;
    private int turn;

    /** A match at turn 1. */
    public MatchState(PlayerState a, PlayerState b) {
        this(a, b, 1);
    }

    public MatchState(PlayerState a, PlayerState b, int turn) {
        this.a = Objects.requireNonNull(a, "a");
        this.b = Objects.requireNonNull(b, "b");
        if (turn < 1 || turn > LAST_TURN) {
            throw new IllegalArgumentException("turn must be 1.." + LAST_TURN + ", got " + turn);
        }
        this.turn = turn;
    }

    public PlayerState player(Side side) {
        return side == Side.A ? a : b;
    }

    /** The turn being played, starting at 1. */
    public int turn() {
        return turn;
    }

    /** Moves to the next turn. Called by the turn code when the match goes on. */
    public void nextTurn() {
        if (turn >= LAST_TURN) {
            throw new IllegalStateException("turn " + LAST_TURN + " is the last turn");
        }
        turn++;
    }
}
