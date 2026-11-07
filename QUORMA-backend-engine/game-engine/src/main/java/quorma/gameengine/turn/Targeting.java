package quorma.gameengine.turn;

import java.util.OptionalInt;

/** Lane targeting (ruleset 8.1). Pure functions, no state. */
public final class Targeting {

    private Targeting() {}

    /**
     * The lane an attacker aims at: its own lane number if the enemy has a targetable creature there,
     * otherwise the first enemy lane found scanning upward from its own lane and wrapping from 5 to 1
     * (lane 4 checks 4, 5, 1, 2, 3).
     *
     * @param attackerLane lane of the attacker, 1..5
     * @param targetable   enemy lanes that can be targeted, index 0 = lane 1, length 5 (see
     *                     {@code PlayerState#targetableLanes}). Destroyed creatures are not targetable
     * @return the target lane 1..5, or empty if no enemy lane is targetable
     */
    public static OptionalInt targetLane(int attackerLane, boolean[] targetable) {
        // TODO(task-2): implement ruleset 8.1
        throw new UnsupportedOperationException("Targeting.targetLane is not implemented yet");
    }
}
