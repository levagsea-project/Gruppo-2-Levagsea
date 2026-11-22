package quorma.gameengine.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.hand.Suit;

/** One player's side of the table: life, five lanes, the deck and the discard pile. */
public final class PlayerState {

    public static final int LANES = 5;

    private final Faction faction;
    private int life;
    private final Creature[] lanes = new Creature[LANES];
    private final List<Card> deck;
    private final List<Card> discard = new ArrayList<>();

    // ---- per-turn state of the suit synergies (ruleset 7.10), set by Evaluate and Refill ----
    private List<BoundExpansion> playerAbilities = List.of();
    private Map<Suit, Integer> suitCounts;
    private final Map<Integer, Suit> vacated = new HashMap<>();
    private final Map<String, Lock> locks = new LinkedHashMap<>();

    /** A lockout set by an effect (E12): ends when, after Refill, the player has fewer than {@code below} of {@code suit}. */
    private record Lock(Suit suit, int below) {}

    private final List<Aura> auras = new ArrayList<>();
    private final List<LaneEffect> laneEffects = new ArrayList<>();

    /** The lasting effects of this player's spells still in force, oldest first. */
    public List<Aura> auras() {
        auras.removeIf(a -> !a.lasting().active());
        return List.copyOf(auras);
    }

    public void addAura(Aura aura) {
        auras.add(Objects.requireNonNull(aura, "aura"));
    }

    /** Ends the auras that last until {@code moment} in turn {@code now} (see {@link Lasting#endsAt}). */
    public void expireAuras(String moment, int now) {
        auras.removeIf(a -> a.lasting().endsAt(moment, now));
    }

    /** Effects waiting for the next creature placed in a lane (E64), oldest first. */
    public void addLaneEffect(LaneEffect effect) {
        laneEffects.add(Objects.requireNonNull(effect, "effect"));
    }

    /** Takes and returns the effects waiting on {@code lane}, oldest first. */
    public List<LaneEffect> takeLaneEffects(int lane) {
        List<LaneEffect> out = laneEffects.stream().filter(e -> e.lane() == lane).toList();
        laneEffects.removeAll(out);
        return out;
    }

    /** Raises life by {@code amount} (0 or more), for example E9. Life has no cap. */
    public void gainLife(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be 0 or more, got " + amount);
        }
        life = Math.addExact(life, amount);
    }

    /** The lowest empty lane, if any. */
    public java.util.OptionalInt lowestEmptyLane() {
        for (int lane = 1; lane <= LANES; lane++) {
            if (lanes[lane - 1] == null) {
                return java.util.OptionalInt.of(lane);
            }
        }
        return java.util.OptionalInt.empty();
    }

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

    /**
     * The expansions of the active {@code per_player} suit synergy tiers, bound by Evaluate for this turn: their
     * source is the player, not a creature.
     */
    public List<BoundExpansion> playerAbilities() {
        return playerAbilities;
    }

    public void setPlayerAbilities(List<BoundExpansion> expansions) {
        this.playerAbilities = List.copyOf(expansions);
    }

    /** Evaluate: fixes the creature count of each suit for the turn, destroyed ones included (7.10.3). */
    public void fixSuitCounts() {
        Map<Suit, Integer> counts = new EnumMap<>(Suit.class);
        for (Creature c : creatures()) {
            counts.merge(c.card().suit(), 1, Integer::sum);
        }
        suitCounts = counts;
    }

    /** Creatures of {@code suit}: the count fixed by Evaluate, or the creatures in play if none was fixed. */
    public int suitCount(Suit suit) {
        if (suitCounts != null) {
            return suitCounts.getOrDefault(suit, 0);
        }
        return (int) creatures().stream().filter(c -> c.card().suit() == suit).count();
    }

    /** Refill: forgets which lanes a destroyed creature left, before this refill's discards note them. */
    public void clearVacated() {
        vacated.clear();
    }

    /** Refill: a destroyed creature of {@code suit} was discarded from {@code lane} (condition C25). */
    public void noteVacated(int lane, Suit suit) {
        vacated.put(lane, suit);
    }

    /** The suit of the creature destroyed in {@code lane} this turn, if any. */
    public Optional<Suit> vacatedSuit(int lane) {
        return Optional.ofNullable(vacated.get(lane));
    }

    /** Sets the lockout {@code key} (effect E12 with {@code lockout}). */
    public void lock(String key, Suit suit, int reactivateBelow) {
        locks.put(Objects.requireNonNull(key, "key"), new Lock(suit, reactivateBelow));
    }

    public boolean isLocked(String key) {
        return locks.containsKey(key);
    }

    /** End of Refill (10.10): lifts every lockout whose suit now has fewer creatures in play than it needs. */
    public void releaseLocks() {
        Iterator<Lock> it = locks.values().iterator();
        while (it.hasNext()) {
            Lock lock = it.next();
            long inPlay = creatures().stream().filter(c -> c.card().suit() == lock.suit()).count();
            if (inPlay < lock.below()) {
                it.remove();
            }
        }
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
        c.markLeftPlay();
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
