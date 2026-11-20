package quorma.gameengine.log;

import java.util.Objects;
import quorma.gameengine.model.Side;

/** What an effect was aimed at: a creature, a player or a lane. */
public sealed interface TargetRef permits CreatureRef, TargetRef.PlayerRef, TargetRef.LaneRef {

    /** A player (life, deck). */
    record PlayerRef(Side side) implements TargetRef {
        public PlayerRef {
            Objects.requireNonNull(side, "side");
        }
    }

    /** A lane of a player (E64, E36). */
    record LaneRef(Side side, int lane) implements TargetRef {
        public LaneRef {
            Objects.requireNonNull(side, "side");
        }
    }
}
