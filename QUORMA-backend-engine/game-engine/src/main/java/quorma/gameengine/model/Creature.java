package quorma.gameengine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import quorma.gameengine.effects.BoundExpansion;

/**
 * A creature card in a lane, with its current state. Damage is permanent across turns (ruleset 3.2.6).
 *
 * <p>A creature at DEF 0 or less is flagged <em>destroyed</em>. It stays in its lane until Refill, cannot
 * be targeted unless an effect says so, and is then discarded.</p>
 *
 * <p>What effects left on it is kept with its {@link Lasting}: ATK modifiers, DEF bonuses, granted abilities
 * ({@link Grant}), removed keywords, the attackers it cannot damage (E23) and an ATK set to its base (E4). The
 * turn code ends them at the moment their duration names ({@link #expire}).</p>
 */
public final class Creature {

    /** An ATK or DEF change left by an effect. */
    public record StatMod(int amount, Lasting lasting) {}

    /** Something an effect left with a duration: a removed keyword, a creature this one cannot damage. */
    public record Timed<T>(T value, Lasting lasting) {}

    private final CreatureCard card;
    private final int lane;
    private final List<BoundExpansion> abilities;
    private List<BoundExpansion> synergyAbilities = List.of();
    private final List<BoundExpansion> equipment = new ArrayList<>();
    private final List<Grant> grants = new ArrayList<>();
    private final List<Timed<String>> removals = new ArrayList<>();
    private final List<StatMod> atkMods = new ArrayList<>();
    private final List<StatMod> defBonuses = new ArrayList<>();
    private final List<Timed<Creature>> noDamageTo = new ArrayList<>();
    private final List<String> noDamageBy = new ArrayList<>();
    private Lasting atkSetToBase;
    private String atkSetToBaseBy;
    private int currentDef;
    private int baseDefBonus;
    private boolean condemned;
    private boolean leftPlay;
    private boolean damagedThisTurn;
    private final Map<String, Integer> evolutionTriggers = new LinkedHashMap<>();
    private final Map<String, Integer> triggerCounts = new LinkedHashMap<>();
    private final Map<String, Integer> lastTurnTriggered = new LinkedHashMap<>();
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

    /** The bound expansions of the card's own abilities, in card order. */
    public List<BoundExpansion> cardAbilities() {
        return abilities;
    }

    /**
     * Every bound expansion the creature has on its own: the card's abilities in card order, then the ones of the
     * equipment applied to it, then the abilities effects granted it (oldest first), then the suit synergy tiers it
     * has this turn (ruleset 7.10). Keywords an effect removed are left out. Static effects of other creatures and of
     * spells (a keyword granted or removed while their source is in play) are added by the turn code.
     */
    public List<BoundExpansion> abilities() {
        if (equipment.isEmpty() && grants.isEmpty() && synergyAbilities.isEmpty() && removals.isEmpty()) {
            return abilities;
        }
        List<BoundExpansion> all = new ArrayList<>(abilities);
        all.addAll(equipment);
        for (Grant g : grants) {
            if (g.lasting().active()) {
                all.addAll(g.expansions());
            }
        }
        all.addAll(synergyAbilities);
        List<String> removed = removedKeywords();
        if (!removed.isEmpty()) {
            all.removeIf(e -> removed.contains(e.abilityId()));
        }
        return List.copyOf(all);
    }

    /** The expansions of {@link #abilities()} with trigger {@code triggerId}, in that order. */
    public List<BoundExpansion> abilities(String triggerId) {
        return abilities().stream().filter(a -> triggerId.equals(a.triggerId())).toList();
    }

    /**
     * Replaces the synergy abilities, which Evaluate sets for the turn: a copy of each expansion of the active
     * tiers of this creature's suit, the creature being the source. Empty clears them.
     */
    public void setSynergyAbilities(List<BoundExpansion> expansions) {
        this.synergyAbilities = List.copyOf(expansions);
    }

    /** Gives the creature the lasting expansions of an equipment applied to it. */
    public void equip(List<BoundExpansion> expansions) {
        equipment.addAll(expansions);
    }

    /** Gives the creature abilities for a while. */
    public void grant(Grant grant) {
        grants.add(Objects.requireNonNull(grant, "grant"));
    }

    /** True if the creature has an ability with id {@code abilityId} (its own, granted or of its suit tiers). */
    public boolean has(String abilityId) {
        return abilities().stream().anyMatch(e -> e.abilityId().equals(abilityId));
    }

    /** Takes the keyword {@code keywordId} away for a while (E15). */
    public void removeKeyword(String keywordId, Lasting lasting) {
        removals.add(new Timed<>(keywordId, lasting));
    }

    /** The keywords effects took away from it, still in force. */
    public List<String> removedKeywords() {
        List<String> out = new ArrayList<>();
        for (Timed<String> r : removals) {
            if (r.lasting().active()) {
                out.add(r.value());
            }
        }
        return out;
    }

    /** True if the creature counts as any suit for the hand (Camaleonte, effect E55). Never for suit synergies. */
    public boolean wildSuit() {
        return abilities().stream().anyMatch(a -> a.effectId().equals("E55"));
    }

    public int currentDef() {
        return currentDef;
    }

    /** The base DEF: the printed DEF plus what E3 added. Heals stop there. */
    public int baseDef() {
        return Math.addExact(card.def(), baseDefBonus);
    }

    /** E3: raises the base DEF by {@code amount}, without healing. */
    public void addBaseDef(int amount) {
        baseDefBonus = Math.addExact(baseDefBonus, amount);
    }

    /** True once the creature has been destroyed. */
    public boolean destroyed() {
        return destroyed;
    }

    /** True once the creature has left its lane (discarded or put away by an effect). */
    public boolean leftPlay() {
        return leftPlay;
    }

    /** The creature left its lane: what depends on it as a source ends. */
    public void markLeftPlay() {
        leftPlay = true;
    }

    /**
     * Current ATK before the subfaction modifier: printed ATK plus the modifiers in force, never below 0
     * (ruleset 9.1).
     */
    public int currentAtk() {
        return atk(false, false);
    }

    /**
     * The ATK with the modifiers in force, leaving out the positive ones (E61: bonuses do not apply) or the negative
     * ones (E52: ATK cannot be lowered by effects), never below 0.
     */
    public int atk(boolean ignorePositive, boolean ignoreNegative) {
        long atk = card.atk();
        for (StatMod m : atkMods) {
            if (!m.lasting().active() || (m.amount() > 0 && ignorePositive) || (m.amount() < 0 && ignoreNegative)) {
                continue;
            }
            atk += m.amount();
        }
        return Math.toIntExact(Math.max(0, atk));
    }

    /** Adds {@code amount} (may be negative) to the ATK, permanently. The amount is already floored by the effect. */
    public void addAtk(int amount) {
        addAtk(amount, Lasting.permanent());
    }

    /** Adds {@code amount} (may be negative) to the ATK for as long as {@code lasting} says. */
    public void addAtk(int amount, Lasting lasting) {
        atkMods.add(new StatMod(amount, lasting));
    }

    /**
     * Adds {@code amount} to the ATK until this creature destroys a creature (duration {@code D_FinoAUccisione}):
     * {@link #endUntilKill()} takes it away.
     */
    public void addUntilKillAtk(int amount) {
        addAtk(amount, new Lasting(Lasting.UNTIL_KILL, 0, null));
    }

    /** Ends the effects that last until this creature destroys a creature. */
    public void endUntilKill() {
        atkMods.removeIf(m -> m.lasting().duration().equals(Lasting.UNTIL_KILL));
        grants.removeIf(g -> g.lasting().duration().equals(Lasting.UNTIL_KILL));
        equipment.removeIf(e -> e.duration().equals(Lasting.UNTIL_KILL));
    }

    /**
     * Ends what lasts until {@code moment} ({@code D_FinoAllaFineDelTurno}, {@code D_FinoAllaFineDelCombattimento} or
     * {@code D_FinoAlProssimoRifornimento}) in turn {@code now}. A DEF bonus that ends takes its DEF back, never below
     * 1 for a creature still in play.
     */
    public void expire(String moment, int now) {
        Predicate<Lasting> ends = l -> l.endsAt(moment, now);
        atkMods.removeIf(m -> ends.test(m.lasting()));
        grants.removeIf(g -> ends.test(g.lasting()));
        removals.removeIf(r -> ends.test(r.lasting()));
        for (int i = noDamageTo.size() - 1; i >= 0; i--) {
            if (ends.test(noDamageTo.get(i).lasting())) {
                noDamageTo.remove(i);
                noDamageBy.remove(i);
            }
        }
        if (atkSetToBase != null && ends.test(atkSetToBase)) {
            atkSetToBase = null;
        }
        List<StatMod> ending = defBonuses.stream().filter(m -> ends.test(m.lasting())).toList();
        for (StatMod m : ending) {
            defBonuses.remove(m);
            loseDefBonus(m.amount());
        }
    }

    private void loseDefBonus(int amount) {
        if (!destroyed && currentDef > 1) {
            currentDef = Math.max(1, Math.subtractExact(currentDef, amount));
        }
    }

    /**
     * E59: takes away the positive ATK modifiers, the DEF bonuses (DEF never below 1) and the keywords and
     * Evolutions granted by effects. The printed values stay.
     */
    public void cancelBonuses() {
        atkMods.removeIf(m -> m.amount() > 0);
        for (StatMod m : List.copyOf(defBonuses)) {
            loseDefBonus(m.amount());
        }
        defBonuses.clear();
        grants.removeIf(Grant::byEffect);
    }

    /** Sets the ATK to the printed ATK for as long as {@code lasting} says (E4 with value {@code base}). */
    public void setAtkToBase(String abilityId, Lasting lasting) {
        atkSetToBase = lasting;
        atkSetToBaseBy = abilityId;
    }

    /** True while an E4 sets the ATK to the printed ATK. */
    public boolean atkSetToBase() {
        return atkSetToBase != null && atkSetToBase.active();
    }

    /** The ability whose E4 sets the ATK to the printed ATK. */
    public String atkSetToBaseBy() {
        return atkSetToBaseBy;
    }

    /**
     * E23: this creature deals no combat damage to {@code protectedOne} for as long as {@code lasting} says;
     * {@code abilityId} is the ability of {@code protectedOne} that says so.
     */
    public void preventDamageTo(Creature protectedOne, String abilityId, Lasting lasting) {
        noDamageTo.add(new Timed<>(protectedOne, lasting));
        noDamageBy.add(abilityId);
    }

    /** True if an E23 stops this creature from dealing combat damage to {@code other}. */
    public boolean cannotDamage(Creature other) {
        return cannotDamageBy(other) != null;
    }

    /** The ability whose E23 stops this creature from dealing combat damage to {@code other}, or null. */
    public String cannotDamageBy(Creature other) {
        for (int i = 0; i < noDamageTo.size(); i++) {
            if (noDamageTo.get(i).value() == other && noDamageTo.get(i).lasting().active()) {
                return noDamageBy.get(i);
            }
        }
        return null;
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
        if (amount > 0) {
            damagedThisTurn = true;
        }
        if (currentDef <= 0) {
            destroyed = true;
        }
    }

    /** True if the creature took damage (more than 0) this turn. */
    public boolean damagedThisTurn() {
        return damagedThisTurn;
    }

    /** End of turn: forgets the damage taken this turn. */
    public void newTurn() {
        damagedThisTurn = false;
    }

    /** Raises DEF by {@code amount} without a cap: not a heal, the base DEF stays what it was (effect E2). */
    public void addDef(int amount) {
        currentDef = Math.addExact(currentDef, amount);
    }

    /**
     * Changes DEF by {@code amount} for as long as {@code lasting} says (effect E2). A bonus is remembered so that it
     * can end or be cancelled (E59); a malus lowers DEF at once and destroys the creature at 0 or less.
     */
    public void addDef(int amount, Lasting lasting) {
        currentDef = Math.addExact(currentDef, amount);
        if (amount > 0) {
            defBonuses.add(new StatMod(amount, lasting));
        } else if (currentDef <= 0) {
            destroyed = true;
        }
    }

    /** Sets DEF to {@code value} (E11: a destruction prevented leaves the creature at {@code set_def}). */
    public void setDef(int value) {
        currentDef = value;
    }

    /** False for an expansion that already fired as often as it may on this creature; true for anything else. */
    public boolean canTrigger(BoundExpansion expansion) {
        return canTrigger(expansion, -1);
    }

    /**
     * False for an Evolution that already fired, for an expansion that reached its {@code max_triggers} and, in turn
     * {@code turn}, for a {@code once_per_turn} expansion that already fired this turn.
     */
    public boolean canTrigger(BoundExpansion expansion, int turn) {
        if (expansion.evolution()) {
            return evolutionTriggers.getOrDefault(expansion.abilityId(), 0) < expansion.maxTriggers();
        }
        if (expansion.maxTriggers() > 0
                && triggerCounts.getOrDefault(expansion.counterKey(), 0) >= expansion.maxTriggers()) {
            return false;
        }
        return !(expansion.oncePerTurn() && turn >= 0 && lastTurnTriggered.getOrDefault(expansion.counterKey(), -1) == turn);
    }

    /** Records that an Evolution fired on this creature; other expansions are not counted. */
    public void markTriggered(BoundExpansion expansion) {
        markTriggered(expansion, -1);
    }

    /** Records that {@code expansion} fired on this creature in turn {@code turn}. */
    public void markTriggered(BoundExpansion expansion, int turn) {
        if (expansion.evolution()) {
            evolutionTriggers.merge(expansion.abilityId(), 1, Integer::sum);
            return;
        }
        if (expansion.maxTriggers() > 0) {
            triggerCounts.merge(expansion.counterKey(), 1, Integer::sum);
        }
        if (expansion.oncePerTurn() && turn >= 0) {
            lastTurnTriggered.put(expansion.counterKey(), turn);
        }
    }

    /** How many of this creature's Evolutions have fired (Spira 3 counts them). */
    public int evolutionsTriggered() {
        return evolutionTriggers.size();
    }

    /** Raises DEF by {@code amount}, never above the base DEF. A destroyed creature is not healed. */
    public void heal(int amount) {
        if (!destroyed) {
            currentDef = Math.max(currentDef, Math.min(baseDef(), Math.addExact(currentDef, amount)));
        }
    }

    /**
     * Saves the creature from destruction (effect E12 with {@code then = destroy_at_next_refill}): DEF goes back to
     * the base DEF, and Refill destroys it if it is still in its lane.
     */
    public void saveWithFullDef() {
        saveWithFullDef(true);
    }

    /** Saves the creature from destruction with its DEF back to the base DEF; {@code condemn} makes Refill destroy it. */
    public void saveWithFullDef(boolean condemn) {
        currentDef = baseDef();
        destroyed = false;
        if (condemn) {
            condemned = true;
        }
    }

    /** True if the creature was saved and Refill will destroy it. */
    public boolean condemned() {
        return condemned;
    }

    /** Flags the creature destroyed without changing its DEF (for effects). */
    public void markDestroyed() {
        destroyed = true;
    }

    /** Clears the destroyed flag: a destruction prevented by a replacement effect (E11, E12). */
    public void undestroy() {
        destroyed = false;
    }

    @Override
    public String toString() {
        return "Creature[lane=" + lane + ", " + card.id() + ", atk=" + currentAtk() + ", def=" + currentDef
                + (destroyed ? ", destroyed" : "") + "]";
    }
}
