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
 * @param redirectedFrom the target the attack had when it was declared, if an effect redirected it (E24); the attack
 *                     then keeps the cycle of that original target (ruleset 9.4)
 */
public record Attack(Side attacker, int attackerLane, Optional<Target> target, Optional<Target> redirectedFrom) {

    public Attack {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(redirectedFrom, "redirectedFrom");
        if (attackerLane < 1 || attackerLane > PlayerState.LANES) {
            throw new IllegalArgumentException("attackerLane must be 1.." + PlayerState.LANES + ", got " + attackerLane);
        }
    }

    /** An attack that has not been redirected. */
    public Attack(Side attacker, int attackerLane, Optional<Target> target) {
        this(attacker, attackerLane, target, Optional.empty());
    }

    /**
     * True if the attack aimed at the creature in the opposite lane when it was declared: it is resolved in Cycle 1
     * (ruleset 9.2). A redirected attack keeps the cycle of the attack it was (ruleset 9.4).
     */
    public boolean inCycle1() {
        Optional<Target> original = redirectedFrom.isPresent() ? redirectedFrom : target;
        return original.isPresent() && original.get().lane() == attackerLane;
    }

    public boolean isRedirected() {
        return redirectedFrom.isPresent();
    }

    /** This attack aimed at {@code newTarget}; the target it has now becomes {@link #redirectedFrom}. */
    public Attack redirectTo(Target newTarget) {
        return new Attack(attacker, attackerLane, Optional.of(newTarget),
                Optional.of(redirectedFrom.orElse(target.orElseThrow())));
    }

    /** 1 for an attack on the opposite lane, 2 for every other attack, including one without a target. */
    public int cycle() {
        return inCycle1() ? 1 : 2;
    }
}
