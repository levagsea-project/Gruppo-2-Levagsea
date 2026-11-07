package quorma.gameengine.testkit;

import java.util.Optional;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Target;

/** Test helper: write an attack in one line. */
public final class Attacks {

    private Attacks() {}

    /** An attack by {@code attacker}'s creature in {@code lane} on the opponent's creature in {@code targetLane}. */
    public static Attack hit(Side attacker, int lane, int targetLane) {
        return new Attack(attacker, lane, Optional.of(new Target(attacker.other(), targetLane)));
    }

    /** An attack with no target: the opponent had nothing to aim at. */
    public static Attack noTarget(Side attacker, int lane) {
        return new Attack(attacker, lane, Optional.empty());
    }
}
