package quorma.gameengine.log;

import java.util.Objects;

/**
 * An ability that changed an attack's numbers.
 *
 * @param source the creature that has the ability
 * @param value  the ability's number
 */
public record Modifier(CreatureRef source, String abilityId, String effectId, Kind kind, int value) {

    public enum Kind {
        /** {@code value} more ATK (a suit synergy). */
        ATK_BONUS,
        /** {@code value}% less damage (Scudo). */
        DAMAGE_REDUCTION_PCT,
        /** {@code value} less damage per blow (flat Scudo). */
        DAMAGE_REDUCTION_FLAT,
        /** Ignores {@code value}% of the target's reductions (Perforante). */
        PIERCE_PCT,
        /** Spill multiplier × {@code value} / 1000 (Traboccante). */
        SPILL_MULTIPLIER,
        /** Spill treats the target's DEF before the blow as {@code value} / 1000 of itself. */
        SPILL_DEF_PERMILLE
    }

    public Modifier {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(kind, "kind");
    }
}
