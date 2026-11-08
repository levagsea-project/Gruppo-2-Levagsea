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
        if (attackerLane < 1 || attackerLane > 5) {
            throw new IllegalArgumentException("attackerLane must be between 1 and 5");
        }
        if (targetable.length != 5) {
            throw new IllegalArgumentException("targetable must contain exactly 5 lanes");
        }
        for (int os = 0; os < 5; os++) {
            int laneIndex = (attackerLane - 1 + os) % 5;
            if (targetable[laneIndex]) {
                return OptionalInt.of(laneIndex + 1);
            }
        }
        return OptionalInt.empty();
    }
}
