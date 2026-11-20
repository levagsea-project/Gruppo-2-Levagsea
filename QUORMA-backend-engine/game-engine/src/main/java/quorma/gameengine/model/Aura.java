package quorma.gameengine.model;

import java.util.Objects;
import quorma.gameengine.effects.BoundExpansion;

/**
 * A lasting effect of a spell, left on its player's side of the table: a static effect on a group of creatures
 * (S9, S10, S11, S20), a replacement (T4, T23, T25, T34) or a triggered effect (T29). Its targets are found when it
 * applies, so creatures that enter play later are covered too. It ends with its duration.
 *
 * @param cardId    the spell
 * @param expansion the expansion, its id the spell's {@code INLINE_<n>}
 * @param lasting   how long it lasts
 */
public record Aura(String cardId, BoundExpansion expansion, Lasting lasting, int[] triggers) {

    public Aura {
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(expansion, "expansion");
        Objects.requireNonNull(lasting, "lasting");
    }

    public Aura(String cardId, BoundExpansion expansion, Lasting lasting) {
        this(cardId, expansion, lasting, new int[1]);
    }

    /** False once the aura fired its {@code max_triggers} times. */
    public boolean canTrigger() {
        return expansion.maxTriggers() == 0 || triggers[0] < expansion.maxTriggers();
    }

    public void markTriggered() {
        triggers[0]++;
    }
}
