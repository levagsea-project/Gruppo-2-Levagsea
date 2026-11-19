package quorma.gameengine.turn;

import java.util.Objects;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.model.Side;

/**
 * The Evaluate phase result: each player's hand and spill multiplier, and the suit synergy tiers each has active,
 * fixed for the whole turn (ruleset 7.9, 7.10).
 */
public record Evaluation(HandResult a, HandResult b, int multiplierA, int multiplierB, Synergies synergies) {

    public Evaluation {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(synergies, "synergies");
    }

    /** An evaluation with no synergy tier active. */
    public Evaluation(HandResult a, HandResult b, int multiplierA, int multiplierB) {
        this(a, b, multiplierA, multiplierB, Synergies.NONE);
    }

    public HandResult hand(Side side) {
        return side == Side.A ? a : b;
    }

    public int multiplier(Side side) {
        return side == Side.A ? multiplierA : multiplierB;
    }
}
