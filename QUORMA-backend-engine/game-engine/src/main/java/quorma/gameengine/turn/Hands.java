package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.ExcludeFromHandEffect;
import quorma.gameengine.effects.ExtraRankEffect;
import quorma.gameengine.effects.HandTypeCondition;
import quorma.gameengine.effects.RankedTarget;
import quorma.gameengine.effects.ReplaceRankEffect;
import quorma.gameengine.effects.Targets;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.WildSuit;
import quorma.gameengine.model.Aura;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * Evaluate (ruleset 7) with the effects that bend the hand:
 * <ul>
 *   <li>E63 (T4): the creature does not count for its player's hand ({@link #excluded}).</li>
 *   <li>E32: the creature's rank is replaced ({@code highest_ally}: the highest rank among its living allies).</li>
 *   <li>E30: the creature may count as another rank; at most one E30 substitution per hand.</li>
 *   <li>E54: for straights, the creature may count as its rank plus or minus {@code step}.</li>
 *   <li>E55: the creature counts as any suit for flushes ({@link WildSuit}).</li>
 * </ul>
 * Every combination of these options is tried and the best hand wins; on a tie the first one found, trying each
 * creature in lane order with its own rank first. A C9 condition on E30 or E32 is read on the hand that results:
 * the option is kept only when the hand is one of the listed types. E54 options only count for straights.
 */
final class Hands {

    private static final Set<HandType> STRAIGHTS = EnumSet.of(HandType.STRAIGHT, HandType.STRAIGHT_FLUSH, HandType.ROYAL_FLUSH);

    private Hands() {}

    /** One way a creature may count for the hand: a rank, the hand types it needs (null: any), whether it is an E30. */
    private record Option(int rank, Set<HandType> needs, boolean substitution) {}

    /**
     * The creatures excluded from their player's hand this Evaluate (E63, trigger T4): chosen by the target of each
     * T4 effect of a living creature or of a spell's aura, seen from its controller.
     */
    static Set<Creature> excluded(Board b) {
        Set<Creature> out = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Creature c : b.everyone()) {
            if (c.destroyed()) {
                continue;
            }
            Side side = b.sideOf(c);
            for (BoundExpansion e : b.abilities(c, "T4")) {
                if (e.effectId().equals(ExcludeFromHandEffect.ID) && b.holds(e, side, c, Board.Ctx.NONE)) {
                    pick(b, e, side, c).ifPresent(out::add);
                }
            }
        }
        for (Side owner : b.prioritySides()) {
            for (Aura aura : b.state().player(owner).auras()) {
                BoundExpansion e = aura.expansion();
                if ("T4".equals(e.triggerId()) && e.effectId().equals(ExcludeFromHandEffect.ID)) {
                    pick(b, e, owner, null).ifPresent(out::add);
                }
            }
        }
        return out;
    }

    private static java.util.Optional<Creature> pick(Board b, BoundExpansion e, Side side, Creature source) {
        return Targets.find(e.targetId())
                .orElseThrow(() -> new IllegalStateException("not a selection target: " + e.targetId()))
                .pick(b, e, side, source);
    }

    /** The best hand of {@code side}: its creatures in lane order (destroyed ones included), minus {@code excluded}. */
    static HandResult best(Board b, Side side, Set<Creature> excluded, Function<List<HandCard>, HandResult> evaluator,
            Comparator<HandResult> comparator) {
        List<Creature> creatures = new ArrayList<>();
        for (Creature c : b.state().player(side).creatures()) {
            if (!excluded.contains(c)) { // destroyed creatures still count until Refill (ruleset 7.9)
                creatures.add(c);
            }
        }
        List<List<Option>> options = new ArrayList<>();
        Set<Integer> wildLanes = new HashSet<>();
        for (Creature c : creatures) {
            options.add(options(b, side, c));
            if (b.wildSuit(c)) {
                wildLanes.add(c.lane());
            }
        }
        Search search = new Search(creatures, options, wildLanes, evaluator, comparator);
        search.run(0, new ArrayList<>(), false);
        return search.best;
    }

    private static List<Option> options(Board b, Side side, Creature c) {
        List<Option> out = new ArrayList<>();
        out.add(new Option(c.card().rank(), null, false));
        for (Board.Active a : reachingIgnoringC9(b, ReplaceRankEffect.ID, c)) {
            String rank = a.e().stringParam("rank").orElseThrow();
            int value = rank.equals("highest_ally") ? highestAlly(b, side, c) : rankOf(rank);
            if (value > 0) {
                out.add(new Option(value, needs(a.e()), false));
            }
        }
        for (Board.Active a : reachingIgnoringC9(b, ExtraRankEffect.ID, c)) {
            out.add(new Option(rankOf(a.e().stringParam("rank").orElseThrow()), needs(a.e()), true));
        }
        int step = b.rankStep(c);
        for (int k = 1; k <= step; k++) {
            out.add(new Option(shift(c.card().rank(), -k), STRAIGHTS, false));
            out.add(new Option(shift(c.card().rank(), k), STRAIGHTS, false));
        }
        return out;
    }

    /** E30 and E32 reaching {@code c} whose conditions hold, leaving out C9 (read on the resulting hand). */
    private static List<Board.Active> reachingIgnoringC9(Board b, String effectId, Creature c) {
        List<Board.Active> out = new ArrayList<>();
        Side side = b.sideOf(c);
        for (Board.Active a : b.statics()) {
            if (a.e().effectId().equals(effectId) && b.reaches(a, side, c)) {
                List<ConditionDef> rest = a.e().conditions().stream().filter(x -> !x.id().equals("C9")).toList();
                if (b.holds(a.e().withConditions(rest), a.side(), a.sourceOr(c), Board.Ctx.NONE)) {
                    out.add(a);
                }
            }
        }
        return out;
    }

    private static Set<HandType> needs(BoundExpansion e) {
        Set<HandType> out = null;
        for (ConditionDef c : e.conditions()) {
            if (c.id().equals("C9")) {
                out = HandTypeCondition.handTypes(c);
            }
        }
        return out;
    }

    /** The rank of the highest-ranked living ally of {@code c}, 0 if it has none (1..13, Ace = 1). */
    private static int highestAlly(Board b, Side side, Creature c) {
        int best = 0;
        int bestRank = 0;
        for (Creature x : b.state().player(side).creatures()) {
            if (x != c && !x.destroyed() && RankedTarget.rankValue(x) > best) {
                best = RankedTarget.rankValue(x);
                bestRank = x.card().rank();
            }
        }
        return bestRank;
    }

    /** Rank 1..13 (Ace = 1) shifted by {@code by} on the cycle 2..K, A: A-1 is K, K+1 is A. */
    private static int shift(int rank, int by) {
        int high = rank == 1 ? 14 : rank;
        int shifted = high + by;
        while (shifted > 14) {
            shifted -= 13;
        }
        while (shifted < 2) {
            shifted += 13;
        }
        return shifted == 14 ? 1 : shifted;
    }

    static int rankOf(String rank) {
        return switch (rank) {
            case "A" -> 1;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            default -> Integer.parseInt(rank);
        };
    }

    private static final class Search {
        final List<Creature> creatures;
        final List<List<Option>> options;
        final Set<Integer> wildLanes;
        final Function<List<HandCard>, HandResult> evaluator;
        final Comparator<HandResult> comparator;
        HandResult best;

        Search(List<Creature> creatures, List<List<Option>> options, Set<Integer> wildLanes,
                Function<List<HandCard>, HandResult> evaluator, Comparator<HandResult> comparator) {
            this.creatures = creatures;
            this.options = options;
            this.wildLanes = wildLanes;
            this.evaluator = evaluator;
            this.comparator = comparator;
        }

        void run(int i, List<Option> chosen, boolean substituted) {
            if (i == creatures.size()) {
                List<HandCard> cards = new ArrayList<>();
                for (int k = 0; k < creatures.size(); k++) {
                    Creature c = creatures.get(k);
                    cards.add(new HandCard(c.lane(), chosen.get(k).rank(), c.card().suit()));
                }
                HandResult result = WildSuit.evaluate(cards, wildLanes, evaluator, comparator);
                for (Option o : chosen) {
                    if (o.needs() != null && !o.needs().contains(result.type())) {
                        return;
                    }
                }
                if (best == null || comparator.compare(result, best) > 0) {
                    best = result;
                }
                return;
            }
            for (Option o : options.get(i)) {
                if (o.substitution() && substituted) {
                    continue;
                }
                chosen.add(o);
                run(i + 1, chosen, substituted || o.substitution());
                chosen.remove(chosen.size() - 1);
            }
        }
    }
}
