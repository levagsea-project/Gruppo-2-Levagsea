package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

/**
 * Which creatures an effect aims at: the ones a triggered effect reaches when it resolves (S1, S2, S9, S12, S41, S44, ...)
 * and the ones an immediate effect of a spell or an equipment is aimed at. Selection targets that need the whole table
 * (the highest ATK, the lowest DEF) are still {@link Board#select}.
 */
final class TargetSelector {

    private final MatchState state;

    TargetSelector(MatchState state) {
        this.state = state;
    }

    static boolean singleCreatureTarget(String targetId) {
        return List.of("S1", "S2", "S26", "S27", "S43", "S6", "S7").contains(targetId);
    }

    /** The living creatures a triggered effect reaches when it resolves. */
    List<Creature> targets(Board b, EffectStack.Item item) {
        BoundExpansion e = item.expansion();
        Side side = item.side();
        Creature source = item.source();
        TriggerContext ctx = item.context();
        List<Creature> out = new ArrayList<>();
        switch (e.targetId()) {
            case "S1" -> add(out, source);
            case "S2" -> add(out, ctx.triggerer());
            case "S26" -> add(out, ctx.damager());
            case "S27" -> add(out, ctx.replacement());
            case "S43" -> add(out, ctx.declaredTarget());
            case "S6" -> state.player(side.other()).creatureAt(source.lane()).ifPresent(c -> add(out, c));
            case "S7" -> state.player(side).creatures().stream()
                    .filter(c -> c != source && !c.destroyed() && b.adjacent(side, source, c))
                    .min(Comparator.comparingInt(Creature::lane)).ifPresent(out::add);
            case "S9" -> state.player(side).creatures().forEach(c -> add(out, c));
            case "S10" -> state.player(side.other()).creatures().forEach(c -> add(out, c));
            case "S11" -> b.everyone().forEach(c -> add(out, c));
            case "S12" -> {
                Suit suit = Suit.valueOf(((Value.StrV) e.targetParams().get("suit")).v().toUpperCase(java.util.Locale.ROOT));
                state.player(side).creatures().stream().filter(c -> c.card().suit() == suit).forEach(c -> add(out, c));
            }
            case "S41" -> {
                for (Creature c : state.player(side).creatures()) {
                    if (c != source && !c.destroyed() && b.adjacent(side, source, c)
                            && b.filtersHold(e.filters(), side, side, c)) {
                        out.add(c);
                    }
                }
            }
            case "S44" -> {
                if (ctx.damaged() != null) {
                    neighbours(ctx.damagedSide(), ctx.damaged()).forEach(out::add);
                }
            }
            case "S33", "S35", "S37", "S38", "S39", "S40" ->
                    b.select(e.targetId(), e.targetParams(), side).ifPresent(out::add);
            default -> throw new IllegalStateException("target " + e.targetId() + " has no selection for a triggered effect");
        }
        return out;
    }

    static void add(List<Creature> out, Creature c) {
        if (c != null && !c.destroyed() && !c.leftPlay()) {
            out.add(c);
        }
    }

    /** The living creatures of {@code side} in the lanes next to {@code c} (S44: lanes differing by 1). */
    List<Creature> neighbours(Side side, Creature c) {
        List<Creature> out = new ArrayList<>();
        for (int lane : new int[] {c.lane() - 1, c.lane() + 1}) {
            if (lane >= 1 && lane <= 5) {
                state.player(side).creatureAt(lane).filter(x -> !x.destroyed()).ifPresent(out::add);
            }
        }
        return out;
    }

    /**
     * The creatures an immediate effect of a spell or an equipment aims at. {@code receiver} is the creature an equipment
     * was applied to; {@code chosen} holds the creatures picked by selection targets so far (S44 {@code of} uses them) and
     * gets the new pick.
     */
    List<Creature> immediateTargets(Board b, BoundExpansion e, Side owner, Creature receiver, Map<String, Creature> chosen) {
        List<Creature> targets = new ArrayList<>();
        switch (e.targetId()) {
            case "S27" -> {
                if (receiver != null && !receiver.destroyed()) {
                    targets.add(receiver);
                }
            }
            case "S9" -> state.player(owner).creatures().forEach(c -> add(targets, c));
            case "S10" -> state.player(owner.other()).creatures().forEach(c -> add(targets, c));
            case "S11" -> b.everyone().forEach(c -> add(targets, c));
            case "S44" -> {
                String of = e.targetParams().get("of") instanceof Value.StrV s ? s.v() : null;
                Creature anchor = of == null ? null : chosen.get(of);
                if (anchor != null) {
                    Side anchorSide = state.player(owner).creatures().contains(anchor) ? owner : owner.other();
                    for (Creature n : neighbours(anchorSide, anchor)) {
                        if (b.filtersHold(e.filters(), owner, anchorSide, n)) {
                            targets.add(n);
                        }
                    }
                }
            }
            default -> {
                Creature c = b.select(e.targetId(), e.targetParams(), owner).orElse(null);
                if (c != null) {
                    chosen.putIfAbsent(e.targetId(), c);
                    targets.add(c);
                }
            }
        }
        return targets;
    }
}
