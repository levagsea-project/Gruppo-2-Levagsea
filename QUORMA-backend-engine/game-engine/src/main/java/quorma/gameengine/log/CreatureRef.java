package quorma.gameengine.log;

import java.util.Objects;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** A creature in play, by owner, lane and card: how events name a creature without holding the state object. */
public record CreatureRef(Side side, int lane, String cardId) {

    public CreatureRef {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cardId, "cardId");
    }

    public static CreatureRef of(Side side, Creature creature) {
        return new CreatureRef(side, creature.lane(), creature.card().id());
    }
}
