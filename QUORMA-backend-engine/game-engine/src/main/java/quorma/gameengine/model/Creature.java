package quorma.gameengine.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import quorma.gameengine.effects.BoundExpansion;

/**
 * A creature card in a lane, with its current state. Damage is permanent across turns (ruleset 3.2.6).
 *
 * <p>A creature at DEF 0 or less is flagged <em>destroyed</em>. It stays in its lane until Refill, cannot
 * be targeted unless an effect says so, and is then discarded.</p>
 */
public final class Creature {

    private final CreatureCard card;
    private final int lane;
    private final List<BoundExpansion> abilities;
    private int currentDef;
    private int atkBonus;
    private final Map<String, Integer> stacks = new LinkedHashMap<>();
    private boolean destroyed;

    /** A creature whose abilities are not bound (none run). For tests and cards with no abilities. */
    public Creature(CreatureCard card, int lane) {
        this(card, lane, List.of());
    }

    /**
     * @param abilities the card's abilities, bound by {@code AbilityBinder} when the creature enters play, in
     *                  card order
     */
    public Creature(CreatureCard card, int lane, List<BoundExpansion> abilities) {
        this.card = Objects.requireNonNull(card, "card");
        if (lane < 1 || lane > PlayerState.LANES) {
            throw new IllegalArgumentException("lane must be 1.." + PlayerState.LANES + ", got " + lane);
        }
        this.lane = lane;
        this.abilities = List.copyOf(abilities);
        this.currentDef = card.def();
    }

    public CreatureCard card() {
        return card;
    }

    public int lane() {
        return lane;
    }

    /** The bound expansions of the card's abilities, in card order. */
    public List<BoundExpansion> abilities() {
        return abilities;
    }

    /** The bound expansions with trigger {@code triggerId}, in card order. */
    public List<BoundExpansion> abilities(String triggerId) {
        return abilities.stream().filter(a -> a.triggerId().equals(triggerId)).toList();
    }

    public int currentDef() {
        return currentDef;
    }

    /** True once the creature has been destroyed. */
    public boolean destroyed() {
        return destroyed;
    }

    /**
     * Current ATK before the subfaction modifier: printed ATK plus the bonuses gained so far, never below 0
     * (ruleset 9.1).
     */
    public int currentAtk() {
        return Math.max(0, Math.addExact(card.atk(), atkBonus));
    }

    /** Adds {@code amount} (may be negative) to the ATK bonus. The amount is already floored by the effect. */
    public void addAtk(int amount) {
        atkBonus = Math.addExact(atkBonus, amount);
    }

    /** How many times an effect with stack key {@code key} has been applied, 0 if never. */
    public int stacks(String key) {
        return stacks.getOrDefault(key, 0);
    }

    /** Records one more application of the effect with stack key {@code key} and returns the new count. */
    public int addStack(String key) {
        return stacks.merge(Objects.requireNonNull(key, "key"), 1, Integer::sum);
    }

    /** Stack counts per key, in the order each key was first applied. A read-only view. */
    public Map<String, Integer> stacks() {
        return Collections.unmodifiableMap(stacks);
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
        return "Creature[lane=" + lane + ", " + card.id() + ", atk=" + currentAtk() + ", def=" + currentDef
                + (destroyed ? ", destroyed" : "") + "]";
    }
}
