package quorma.gameengine.model;

import java.util.Objects;
import java.util.Optional;

/**
 * An attack declared in the Dichiarazione phase, frozen from then on.
 *
 * @param attacker     the attacking player
 * @param attackerLane lane of the attacker, 1..5
 * @param target       the enemy creature it aims at. Empty when the opponent has nothing to target: such an
 *                     attack does not happen.
 */
public record Attack(Side attacker, int attackerLane, Optional<Target> target) {

    public Attack {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        if (attackerLane < 1 || attackerLane > PlayerState.LANES) {
            throw new IllegalArgumentException("attackerLane must be 1.." + PlayerState.LANES + ", got " + attackerLane);
        }
    }

    /** True if the attack aims at the creature in the opposite lane: it is resolved in Cycle 1 (ruleset 9.2). */
    public boolean inCycle1() {
        return target.isPresent() && target.get().lane() == attackerLane;
    }

    /** 1 for an attack on the opposite lane, 2 for every other attack, including one without a target. */
    public int cycle() {
        return inCycle1() ? 1 : 2;
    }
}
