package quorma.gameengine.model;

import java.util.Objects;

/**
 * A creature card in a lane, with its current state. Damage is permanent across turns (ruleset 3.2.6).
 *
 * <p>A creature at DEF 0 or less is flagged <em>destroyed</em>. It stays in its lane until Refill, cannot
 * be targeted unless an effect says so, and is then discarded.</p>
 */
public final class Creature {

    private final CreatureCard card;
    private final int lane;
    private int currentDef;
    private boolean destroyed;

    public Creature(CreatureCard card, int lane) {
        this.card = Objects.requireNonNull(card, "card");
        if (lane < 1 || lane > PlayerState.LANES) {
            throw new IllegalArgumentException("lane must be 1.." + PlayerState.LANES + ", got " + lane);
        }
        this.lane = lane;
        this.currentDef = card.def();
    }

    public CreatureCard card() {
        return card;
    }

    public int lane() {
        return lane;
    }

    public int currentDef() {
        return currentDef;
    }

    /** True once the creature has been destroyed. */
    public boolean destroyed() {
        return destroyed;
    }

    /** Current ATK before the subfaction modifier. For now the printed ATK; modifiers come with effects. */
    public int currentAtk() {
        return card.atk();
    }

    /** Lowers DEF by {@code amount} (0 or more). Flags the creature destroyed when DEF reaches 0 or less. */
    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("damage must be 0 or more, got " + amount);
        }
        currentDef = Math.subtractExact(currentDef, amount);
        if (currentDef <= 0) {
            destroyed = true;
        }
    }

    /** Flags the creature destroyed without changing its DEF (for effects). */
    public void markDestroyed() {
        destroyed = true;
    }

    @Override
    public String toString() {
        return "Creature[lane=" + lane + ", " + card.id() + ", def=" + currentDef + (destroyed ? ", destroyed" : "") + "]";
    }
}
