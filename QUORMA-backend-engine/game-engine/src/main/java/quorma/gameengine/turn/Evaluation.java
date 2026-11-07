package quorma.gameengine.turn;

import java.util.Objects;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.model.Side;

/** The Evaluate phase result: each player's hand and spill multiplier, fixed for the whole turn (ruleset 7.9). */
public record Evaluation(HandResult a, HandResult b, int multiplierA, int multiplierB) {

    public Evaluation {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
    }

    public HandResult hand(Side side) {
        return side == Side.A ? a : b;
    }

    public int multiplier(Side side) {
        return side == Side.A ? multiplierA : multiplierB;
    }
}
