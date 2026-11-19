package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.TargetIds;
import quorma.gameengine.effects.Targets;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.MatchEvent.RefillPrevented;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Aura;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

/**
 * Refill (ruleset 10): what lasts until the next Refill ends, the effects that fire at its start, destroyed
 * creatures are discarded, lanes are refilled in order (draws, spells, equipment) and the lockouts are released.
 */
final class Refill {

    private Refill() {}

    static void refill(Game g, List<MatchEvent> log) {
        MatchState state = g.state;
        Side[] order = TurnResolver.prioritySides(state);

        // What lasts until the next Refill ends now; then the effects that trigger at its start (10.1).
        g.expire(Lasting.NEXT_REFILL);
        startOfRefill(g, order, log);

        for (Side side : order) {
            var player = state.player(side);
            player.clearVacated();
            for (int lane = 1; lane <= 5; lane++) {
                Creature creature = player.creatureAt(lane).orElse(null);
                // 10.2: a creature that was saved (E12) and is still in its lane is destroyed now.
                if (creature != null && creature.condemned() && !creature.destroyed()) {
                    creature.markDestroyed();
                    log.add(new CreatureDestroyed(side, lane, creature.card().id()));
                }
                if (creature != null && creature.destroyed()) {
                    player.remove(lane);
                    player.discard().add(creature.card());
                    player.noteVacated(lane, creature.card().suit());
                    log.add(new CreatureDiscarded(side, lane, creature.card().id()));
                }
            }
        }

        for (Side side : order) {
            var player = state.player(side);
            List<Aura> preventing = new ArrayList<>();
            for (int lane = 1; lane <= 5; lane++) {
                if (player.creatureAt(lane).isPresent()) {
                    continue;
                }
                Aura prevented = refillPrevented(state, side, lane);
                if (prevented != null) {
                    if (!preventing.contains(prevented)) {
                        preventing.add(prevented);
                    }
                    log.add(new RefillPrevented(side, lane, prevented.cardId()));
                    continue;
                }
                if (player.deck().isEmpty()) {
                    log.add(new LaneLeftEmpty(side, lane));
                    continue;
                }
                if (!g.draw(side, lane, log)) {
                    lane--; // a spell or an equipment: draw again for the same lane, unless an effect filled it
                }
            }
            preventing.forEach(Aura::markTriggered);
            EquipmentCard pending = g.pendingEquipment(side);
            if (pending != null) {
                g.clearPendingEquipment(side);
                player.discard().add(pending);
                log.add(new EquipmentLost(side, pending.id()));
            }
        }

        // 10.10: what depends on the creatures in play after Refill (the lockout of Vestigio 5).
        for (Side side : order) {
            state.player(side).releaseLocks();
        }
    }

    /**
     * T25 with E47: an aura of a spell keeps the lanes of {@code side} empty in this Refill ({@code player}: the
     * spell's controller or its opponent; {@code lane}: one lane, or every lane). Returns that aura, or null.
     */
    private static Aura refillPrevented(MatchState state, Side side, int lane) {
        for (Side owner : TurnResolver.prioritySides(state)) {
            for (Aura aura : state.player(owner).auras()) {
                BoundExpansion e = aura.expansion();
                if (!"T25".equals(e.triggerId()) || !e.effectId().equals("E47") || !aura.canTrigger()) {
                    continue;
                }
                String who = e.stringParam("player").orElse("avversario");
                Side affected = who.equals("controllore") ? owner : owner.other();
                boolean laneMatches = !e.effectParams().containsKey("lane") || e.intParam("lane") == lane;
                if (affected == side && laneMatches) {
                    return aura;
                }
            }
        }
        return null;
    }

    /**
     * T2 with phase Rifornimento (10.1): the abilities of the creatures in play and of the player (suit synergies),
     * pushed Potenze Artificiali first, then resolved. Destroyed creatures are neither sources nor targets. An effect
     * aimed at the source or at a suit (S1, S12) is pushed once per creature it affects, its conditions checked for
     * that creature; a tier whose source is the player is logged with the creature it affects as source.
     */
    private static void startOfRefill(Game g, Side[] order, List<MatchEvent> log) {
        EffectStack stack = g.stack(log);
        Board b = g.board();
        for (Side side : order) {
            var player = g.state.player(side);
            for (Creature source : player.creatures()) {
                if (!source.destroyed()) {
                    pushRefillStart(g, b, side, source, b.abilities(source, "T2"), stack);
                }
            }
            pushRefillStart(g, b, side, null, player.playerAbilities().stream()
                    .filter(e -> e.triggerId().equals("T2")).toList(), stack);
        }
        stack.resolveAll();
    }

    private static void pushRefillStart(Game g, Board b, Side side, Creature source, List<BoundExpansion> expansions,
            EffectStack stack) {
        for (BoundExpansion e : expansions) {
            if (!"Rifornimento".equals(((Value.StrV) e.triggerParams().get("phase")).v())) {
                continue;
            }
            if (!e.targetId().equals(TargetIds.SELF) && !e.targetId().equals(TargetIds.SUIT_CREATURES)) {
                if (source != null && b.holds(e, side, source, Board.Ctx.NONE)) {
                    g.triggers.push(stack, b, side, source, e, TriggerContext.NONE);
                }
                continue;
            }
            for (Creature target : targetsOf(b, side, e, source)) {
                if (b.holds(e, side, target, Board.Ctx.NONE)) {
                    stack.push(new EffectStack.Item(side, source != null ? source : target, target, e, e.triggerId(),
                            source != null ? g.triggers.scaleOf(b, side, source, e) : 1000, TriggerContext.NONE));
                }
            }
        }
    }

    /** The living creatures an effect of the player or of {@code source} (null for the player) is aimed at (S1, S12). */
    private static List<Creature> targetsOf(Board b, Side side, BoundExpansion e, Creature source) {
        return Targets.find(e.targetId()).map(t -> t.ofPlayerAbility(b, side, source, e)).orElse(List.of());
    }
}
