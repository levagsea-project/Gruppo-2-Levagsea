package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Target;

/**
 * Declare (ruleset 8) and what follows it: one frozen attack for each attacker of each hand, then the triggers the
 * declarations set off (T9, T8, T35) and the redirects (E24).
 */
final class Declaration {

    private Declaration() {}

    /**
     * Declare (8): one attack for each attacker of each hand, with its frozen target. Ordered by attacker lane,
     * and within a lane by faction priority, then {@code Side.A} first.
     */
    static List<Attack> declare(MatchState state, Evaluation evaluation) {
        List<Attack> attacks = new ArrayList<>();
        for (int lane = 1; lane <= 5; lane++) {
            for (Side side : new Side[] {Side.A, Side.B}) {
                if (!evaluation.hand(side).attackerLanes().contains(lane)
                        || state.player(side).creatureAt(lane).isEmpty()) {
                    continue;
                }
                OptionalInt targetLane = Targeting.targetLane(
                        lane, state.player(side.other()).targetableLanes(false));
                attacks.add(new Attack(
                        side,
                        lane,
                        targetLane.isPresent()
                                ? Optional.of(new Target(side.other(), targetLane.getAsInt()))
                                : Optional.empty()));
            }
        }
        attacks.sort(Comparator.comparingInt(Attack::attackerLane)
                .thenComparingInt(attack -> state.player(attack.attacker()).faction() == Faction.POTENZE_ARTIFICIALI ? 0 : 1)
                .thenComparingInt(attack -> attack.attacker() == Side.A ? 0 : 1));
        return List.copyOf(attacks);
    }


    static List<Attack> declareTriggers(Game g, List<Attack> attacks, List<MatchEvent> log) {
        EffectStack stack = g.stack(log);
        MatchState state = g.state;
        for (Attack attack : attacks) {
            Creature target = declaredTarget(state, attack);
            if (target == null) {
                continue;
            }
            Board b = g.board();
            Side targetSide = attack.target().orElseThrow().side();
            Side attackerSide = attack.attacker();
            Creature attacker = state.player(attackerSide).creatureAt(attack.attackerLane()).orElseThrow();
            TriggerContext ctx = new TriggerContext(attacker, attackerSide, null, null, null, null, 0, false, null, 0,
                    target, targetSide, null);
            for (BoundExpansion e : b.abilities(target, "T9")) {
                if (!e.effectId().equals("E24") && b.holds(e, targetSide, target, Board.Ctx.of(ctx))) {
                    g.triggers.push(stack, b, targetSide, target, e, ctx);
                }
            }
            for (Side side : b.prioritySides()) {
                for (Creature watcher : state.player(side).creatures()) {
                    if (watcher.destroyed()) {
                        continue;
                    }
                    for (BoundExpansion e : b.abilities(watcher, "T8")) {
                        if (declarationMatches(b, e, side, watcher, attackerSide, targetSide, target)
                                && b.holds(e, side, watcher, Board.Ctx.of(ctx))) {
                            g.triggers.push(stack, b, side, watcher, e, ctx);
                        }
                    }
                }
            }
            for (Creature ally : state.player(targetSide).creatures()) {
                if (ally == target || ally.destroyed() || !b.adjacent(targetSide, ally, target)) {
                    continue;
                }
                for (BoundExpansion e : b.abilities(ally, "T35")) {
                    if (!e.effectId().equals("E24") && b.holds(e, targetSide, ally, Board.Ctx.of(ctx))) {
                        g.triggers.push(stack, b, targetSide, ally, e, ctx);
                    }
                }
            }
        }
        stack.resolveAll();

        List<Attack> out = new ArrayList<>();
        for (Attack attack : attacks) {
            out.add(redirect(g, attack, log));
        }
        return List.copyOf(out);
    }

    /** T8 params: who attacks ({@code attacker}: opponent, own, any) and at whom ({@code target_scope}). */
    private static boolean declarationMatches(Board b, BoundExpansion e, Side side, Creature watcher, Side attackerSide,
            Side targetSide, Creature target) {
        String attacker = e.triggerParam("attacker").orElse("any");
        if ((attacker.equals("opponent") && attackerSide == side) || (attacker.equals("own") && attackerSide != side)) {
            return false;
        }
        return switch (e.triggerParam("target_scope").orElse("any")) {
            case "self" -> target == watcher;
            case "self_or_adjacent" -> target == watcher || (targetSide == side && b.adjacent(side, watcher, target));
            default -> true;
        };
    }

    private static Creature declaredTarget(MatchState state, Attack attack) {
        if (attack.target().isEmpty()) {
            return null;
        }
        Target target = attack.target().get();
        return state.player(target.side()).creatureAt(target.lane()).orElse(null);
    }

    /**
     * E24: the creature an attack was declared against aims it at a creature of its own player next to it (T9, new
     * target S7: a living adjacent creature, the lowest lane when there are two; without one the effect fizzles);
     * otherwise an adjacent ally of the target with E24 on T35 takes the attack (Provocazione, new target S1: itself;
     * the lowest lane first). The first effect wins: a redirected attack is not declared against its new target, so
     * it triggers nothing there.
     */
    private static Attack redirect(Game g, Attack attack, List<MatchEvent> log) {
        MatchState state = g.state;
        Creature creature = declaredTarget(state, attack);
        if (creature == null) {
            return attack;
        }
        Board b = g.board();
        Side side = attack.target().get().side();
        for (BoundExpansion e : b.abilities(creature, "T9")) {
            if (!e.effectId().equals("E24")) {
                continue;
            }
            CreatureRef source = CreatureRef.of(side, creature);
            log.add(new MatchEvent.EffectTriggered(source, e.abilityId(), e.triggerId(), e.effectId(), 1));
            Creature next = state.player(side).creatures().stream()
                    .filter(c -> !c.destroyed() && b.adjacent(side, creature, c))
                    .min(Comparator.comparingInt(Creature::lane))
                    .orElse(null);
            if (next == null) {
                log.add(new MatchEvent.EffectFizzled(source, e.abilityId(), e.effectId(), FizzleReason.NO_NEW_TARGET));
                return attack;
            }
            Target to = new Target(side, next.lane());
            log.add(new MatchEvent.AttackRedirected(attack, to, e.abilityId()));
            return attack.redirectTo(to);
        }
        for (Creature ally : state.player(side).creatures()) {
            if (ally == creature || ally.destroyed() || !b.adjacent(side, ally, creature)) {
                continue;
            }
            for (BoundExpansion e : b.abilities(ally, "T35")) {
                if (!e.effectId().equals("E24") || !b.holds(e, side, ally, Board.Ctx.NONE)) {
                    continue;
                }
                log.add(new MatchEvent.EffectTriggered(CreatureRef.of(side, ally), e.abilityId(), "T35", "E24", 1));
                Target to = new Target(side, ally.lane());
                log.add(new MatchEvent.AttackRedirected(attack, to, e.abilityId()));
                return attack.redirectTo(to);
            }
        }
        return attack;
    }
}
