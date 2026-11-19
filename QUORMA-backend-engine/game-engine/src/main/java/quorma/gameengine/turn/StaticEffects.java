package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

/**
 * Static (trigger T0, class C) effects that depend on the board: who they apply to and when. They come from any
 * creature ability, whether written on a card, a keyword or a suit synergy tier bound by Evaluate; this class does
 * not know where an expansion comes from. Conditions are checked on the board as it is now, so a destroyed creature
 * neither gives nor receives anything (ruleset 7.10.3).
 *
 * <p>Conditions: C24 (the lockout key is not set), C25 (the creature took a lane left by a destroyed creature of the suit this turn), C22 (the target's DEF is above the source's ATK, both as the cycle started), C1 (the source is an attacker), C4 (its controller has at least {@code level} creatures of
 * {@code suit}, counted with the destroyed ones, which stay until Refill), C11 (the source is adjacent to a living
 * creature of {@code suit}). Targets: S1 (the source) and S41 (living creatures adjacent to the source, with the
 * filter {@code attacker}). Effects: E1 {@code flat} (+ATK) and E34 (the group's creatures count as adjacent).</p>
 */
final class StaticEffects {

    private StaticEffects() {}

    /**
     * The ATK that static E1 {@code flat} effects add to {@code receiver}, one modifier per effect, sources in lane
     * order and each source's abilities in order. {@code source} of a modifier is the creature that has the effect.
     */
    static List<Modifier> atkBonuses(MatchState state, Evaluation evaluation, Side side, Creature receiver,
            Creature target, CycleStart start) {
        List<Modifier> out = new ArrayList<>();
        if (receiver.destroyed()) {
            return out;
        }
        for (Creature source : state.player(side).creatures()) {
            if (source.destroyed()) {
                continue;
            }
            for (BoundExpansion e : source.abilities("T0")) {
                if (!e.effectId().equals("E1") || !"flat".equals(e.stringParam("mode").orElse(null))) {
                    continue;
                }
                int amount = e.intParam("amount");
                if (e.stringParam("scale_by").isPresent()) {
                    // own_evolutions_triggered: once for each Evolution that has fired on a living creature of the player
                    amount = Math.multiplyExact(amount, state.player(side).creatures().stream()
                            .filter(c -> !c.destroyed()).mapToInt(Creature::evolutionsTriggered).sum());
                }
                if (amount != 0 && conditionsHold(e, state, evaluation, side, source, true, target, start)
                        && appliesTo(e, state, evaluation, side, source, receiver)) {
                    out.add(new Modifier(CreatureRef.of(side, source), e.abilityId(), "E1", Modifier.Kind.ATK_BONUS, amount));
                }
            }
        }
        return out;
    }

    /**
     * The factor, per mille, by which the numbers of the Evolutions of {@code creature} are multiplied (effect E19,
     * static): the product of its E19 whose conditions hold, each step floored. 1000 if it has none.
     */
    static int evolutionScale(MatchState state, Side side, Creature creature) {
        long factor = 1000;
        if (!creature.destroyed()) {
            for (BoundExpansion e : creature.abilities("T0")) {
                if (e.effectId().equals("E19") && conditionsHold(e, state, null, side, creature, true, null, null)) {
                    factor = factor * e.intParam("factor") / 1000;
                }
            }
        }
        return Math.toIntExact(factor);
    }

    /**
     * True if the conditions of {@code e}, an ability of {@code source}, hold now (no blow, so C22 fails). For the
     * replacement effects of a blow (Frattura 4 and 5).
     */
    static boolean holds(BoundExpansion e, MatchState state, Evaluation evaluation, Side side, Creature source) {
        // For an effect whose source is the player, {@code source} is the creature the conditions are about:
        // the replacement for C25, the creature that is about to be destroyed for C24.
        return conditionsHold(e, state, evaluation, side, source, true, null, null);
    }

    /** Whether the effect of {@code source} reaches {@code receiver}. */
    private static boolean appliesTo(BoundExpansion e, MatchState state, Evaluation evaluation, Side side,
            Creature source, Creature receiver) {
        return switch (e.targetId()) {
            case "S1" -> receiver == source;
            case "S41" -> receiver != source && adjacent(state, evaluation, side, source, receiver, true)
                    && filtersHold(e, evaluation, side, receiver);
            default -> throw new IllegalStateException("target " + e.targetId() + " has no resolution (EffectRegistry let it through)");
        };
    }

    private static boolean filtersHold(BoundExpansion e, Evaluation evaluation, Side side, Creature receiver) {
        if (e.targetParams().get("filters") instanceof Value.ListV filters) {
            for (Value f : filters.items()) {
                if (f instanceof Value.StrV s && s.v().equals("attacker")
                        && !evaluation.hand(side).attackerLanes().contains(receiver.lane())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param useLinks whether E34 may make creatures adjacent; false while checking the conditions of E34 itself
     * @param target   the creature the source's blow is aimed at, for C22; null when there is no blow, and C22 then fails
     * @param start    the values taken when the cycle started, for C22
     */
    private static boolean conditionsHold(BoundExpansion e, MatchState state, Evaluation evaluation, Side side,
            Creature source, boolean useLinks, Creature target, CycleStart start) {
        for (ConditionDef c : e.conditions()) {
            boolean holds = switch (c.id()) {
                case "C1" -> evaluation != null && evaluation.hand(side).attackerLanes().contains(source.lane());
                case "C4" -> count(state, side, suit(c)) >= ((Value.IntV) c.params().get("level")).v();
                case "C24" -> !state.player(side).isLocked(((Value.StrV) c.params().get("key")).v());
                case "C25" -> state.player(side).vacatedSuit(source.lane()).filter(s -> s == suit(c)).isPresent();
                case "C22" -> target != null && start != null && start.def(target) > start.atk(source);
                case "C11" -> state.player(side).creatures().stream()
                        .anyMatch(o -> o != source && !o.destroyed() && o.card().suit() == suit(c)
                                && adjacent(state, evaluation, side, source, o, useLinks));
                default -> throw new IllegalStateException("condition " + c.id() + " has no evaluation (EffectRegistry let it through)");
            };
            if (!holds) {
                return false;
            }
        }
        return true;
    }

    private static Suit suit(ConditionDef c) {
        return Suit.valueOf(((Value.StrV) c.params().get("suit")).v().toUpperCase(Locale.ROOT));
    }

    /** Creatures of {@code suit} the player had at Evaluate, destroyed ones included: the count is fixed for the turn. */
    private static long count(MatchState state, Side side, Suit suit) {
        return state.player(side).suitCount(suit);
    }

    /**
     * Ruleset 7.10.4: two creatures of one player are adjacent when their lanes differ by exactly 1 (lanes 1 and 5
     * are not adjacent), or when an E34 of one of them says the other's suit counts as adjacent to it.
     */
    private static boolean adjacent(MatchState state, Evaluation evaluation, Side side, Creature a, Creature b,
            boolean useLinks) {
        return Math.abs(a.lane() - b.lane()) == 1
                || (useLinks && (linked(state, evaluation, side, a, b) || linked(state, evaluation, side, b, a)));
    }

    /** True if {@code x} has a living E34 whose group is the suit of {@code y}. */
    private static boolean linked(MatchState state, Evaluation evaluation, Side side, Creature x, Creature y) {
        if (x.destroyed()) {
            return false;
        }
        for (BoundExpansion e : x.abilities("T0")) {
            if (e.effectId().equals("E34")
                    && e.stringParam("group").map(g -> g.toUpperCase(Locale.ROOT)).orElse("").equals(y.card().suit().name())
                    && conditionsHold(e, state, evaluation, side, x, false, null, null)) {
                return true;
            }
        }
        return false;
    }
}
