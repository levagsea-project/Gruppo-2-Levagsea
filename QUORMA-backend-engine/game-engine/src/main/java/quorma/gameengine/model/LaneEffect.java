package quorma.gameengine.model;

import java.util.Objects;
import quorma.gameengine.effects.BoundExpansion;

/**
 * An effect waiting on a lane (E64): the next creature placed there receives it, like a pending equipment
 * (ruleset 3.4.3). It stays when its source leaves play and is used up by the first creature placed.
 *
 * @param lane     1..5
 * @param cardId   the card whose ability left it
 * @param effect   what the creature receives, already bound (class I, target S27)
 */
public record LaneEffect(int lane, String cardId, BoundExpansion effect) {

    public LaneEffect {
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(effect, "effect");
    }
}
