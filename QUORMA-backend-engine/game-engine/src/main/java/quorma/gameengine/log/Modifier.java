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
        /** {@code value} per mille less damage (Muro: 275 is 27.5% less). */
        DAMAGE_REDUCTION_PERMILLE,
        /** {@code value} less damage per blow (flat Scudo). */
        DAMAGE_REDUCTION_FLAT,
        /** Ignores {@code value}% of the target's reductions (Perforante). */
        PIERCE_PCT,
        /** Spill multiplier × {@code value} / 1000 (Traboccante). */
        SPILL_MULTIPLIER,
        /** Spill treats the target's DEF before the blow as {@code value} / 1000 of itself. */
        SPILL_DEF_PERMILLE,
        /** {@code value} less ATK (a static malus). */
        ATK_MALUS,
        /** The ATK is set to {@code value}, the printed ATK (E4). */
        ATK_SET,
        /** The damage of the blow is prevented (E23, E29); {@code value} is the damage it would have dealt. */
        DAMAGE_PREVENTED,
        /** The blow destroys the target whatever its DEF (Letale, E26). */
        LETHAL
    }

    public Modifier {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(kind, "kind");
    }
}
