package quorma.gameengine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One player's side of the table: life, five lanes, the deck and the discard pile. */
public final class PlayerState {

    public static final int LANES = 5;

    private final Faction faction;
    private int life;
    private final Creature[] lanes = new Creature[LANES];
    private final List<Card> deck;
    private final List<Card> discard = new ArrayList<>();

    /** @param deck cards in draw order: index 0 is the top. The list is copied. */
    public PlayerState(Faction faction, int life, List<Card> deck) {
        this.faction = Objects.requireNonNull(faction, "faction");
        this.life = life;
        this.deck = new ArrayList<>(deck);
    }

    public Faction faction() {
        return faction;
    }

    /** Current life. May be 0 or below during a turn: the loss is checked at End of turn (ruleset 11.2). */
    public int life() {
        return life;
    }

    /** @param lane 1..5 */
    public Optional<Creature> creatureAt(int lane) {
        return Optional.ofNullable(lanes[index(lane)]);
    }

    /** Creatures in ascending lane order, destroyed ones included. A snapshot: changing it changes nothing. */
    public List<Creature> creatures() {
        List<Creature> out = new ArrayList<>();
        for (Creature c : lanes) {
            if (c != null) {
                out.add(c);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Lanes an enemy attacker may target, index 0 = lane 1. A lane is targetable when it holds a creature
     * that is not destroyed, or any creature when {@code includeDestroyed} is true (for effects that say so).
     */
    public boolean[] targetableLanes(boolean includeDestroyed) {
        boolean[] out = new boolean[LANES];
        for (int i = 0; i < LANES; i++) {
            out[i] = lanes[i] != null && (includeDestroyed || !lanes[i].destroyed());
        }
        return out;
    }

    /** The live deck. Index 0 is the top. */
    public List<Card> deck() {
        return deck;
    }

    /** The live discard pile, in the order cards were discarded. */
    public List<Card> discard() {
        return discard;
    }

    /** Puts a creature in its lane. The lane must be empty. */
    public void place(Creature creature) {
        int i = index(creature.lane());
        if (lanes[i] != null) {
            throw new IllegalStateException("lane " + creature.lane() + " is not empty");
        }
        lanes[i] = creature;
    }

    /** Empties a lane and returns the creature that was there. The lane must hold a creature. */
    public Creature remove(int lane) {
        int i = index(lane);
        Creature c = lanes[i];
        if (c == null) {
            throw new IllegalStateException("lane " + lane + " is already empty");
        }
        lanes[i] = null;
        return c;
    }

    /** Lowers life by {@code amount} (0 or more), for example spill damage. */
    public void loseLife(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be 0 or more, got " + amount);
        }
        life = Math.subtractExact(life, amount);
    }

    private static int index(int lane) {
        if (lane < 1 || lane > LANES) {
            throw new IllegalArgumentException("lane must be 1.." + LANES + ", got " + lane);
        }
        return lane - 1;
    }
}
