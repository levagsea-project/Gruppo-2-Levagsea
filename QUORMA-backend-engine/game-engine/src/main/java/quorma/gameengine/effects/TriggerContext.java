package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * What a trigger provides to the targets and conditions of the effects it fires (vocab {@code provides}): the
 * creature that caused it, the damage dealt or taken, the creature destroyed, the declared target, the replacement.
 * Fields a trigger does not provide are null (or 0, false).
 *
 * @param triggerer       {@code triggerer} (S2): the attacker of T7, T35, T39
 * @param triggererSide   its side
 * @param damager         {@code damager} (S26): the creature that dealt the damage of T13
 * @param damagerSide     its side
 * @param damaged         {@code damaged_creature} (S44): the creature the source damaged (T38)
 * @param damagedSide     its side
 * @param amount          the damage taken (T13) or dealt (T38)
 * @param killed          true if that damage destroyed the damaged creature (C27)
 * @param destroyed       {@code destroyed}: the creature destroyed (T10, T36, T37; C28)
 * @param destroyedLane   its lane
 * @param declaredTarget  {@code declared_target} (S43): the target of the source's attack (T6, T22)
 * @param declaredSide    its side
 * @param replacement     {@code replacement} (S27): the creature placed (T29)
 */
public record TriggerContext(
        Creature triggerer, Side triggererSide,
        Creature damager, Side damagerSide,
        Creature damaged, Side damagedSide,
        int amount, boolean killed,
        Creature destroyed, int destroyedLane,
        Creature declaredTarget, Side declaredSide,
        Creature replacement) {

    public static final TriggerContext NONE = new TriggerContext(null, null, null, null, null, null, 0, false, null, 0,
            null, null, null);

    public static TriggerContext triggeredBy(Side side, Creature c) {
        return new TriggerContext(c, side, null, null, null, null, 0, false, null, 0, null, null, null);
    }

    public static TriggerContext damagedBy(Side side, Creature damager, int amount) {
        return new TriggerContext(null, null, damager, side, null, null, amount, false, null, 0, null, null, null);
    }

    public static TriggerContext dealt(Side side, Creature damaged, int amount, boolean killed) {
        return new TriggerContext(null, null, null, null, damaged, side, amount, killed, null, 0, null, null, null);
    }

    public static TriggerContext destroyed(Creature destroyed, Side killerSide, Creature killer) {
        return new TriggerContext(killer, killerSide, null, null, null, null, 0, false, destroyed, destroyed.lane(),
                null, null, null);
    }

    public static TriggerContext attacking(Side targetSide, Creature target, Side attackerSide, Creature attacker) {
        return new TriggerContext(attacker, attackerSide, null, null, null, null, 0, false, null, 0, target, targetSide,
                null);
    }

    public static TriggerContext placed(Creature replacement) {
        return new TriggerContext(null, null, null, null, null, null, 0, false, null, 0, null, null, replacement);
    }
}
