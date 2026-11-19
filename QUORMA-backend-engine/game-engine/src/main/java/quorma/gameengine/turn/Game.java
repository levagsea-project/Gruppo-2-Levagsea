package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.Effect;
import quorma.gameengine.effects.EffectContext;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.Effects;
import quorma.gameengine.effects.Followups;
import quorma.gameengine.effects.Outcome;
import quorma.gameengine.effects.PlayerEffect;
import quorma.gameengine.effects.PlayerEffectContext;
import quorma.gameengine.effects.PlayerEffects;
import quorma.gameengine.effects.RankedTarget;
import quorma.gameengine.effects.Target;
import quorma.gameengine.effects.TargetIds;
import quorma.gameengine.effects.Targets;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.EffectFizzled;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.EquipmentApplied;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.EquipmentPending;
import quorma.gameengine.log.MatchEvent.SpellResolved;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Aura;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Grant;
import quorma.gameengine.model.LaneEffect;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;

/**
 * One turn being played: the state, the turn's evaluation and what the effects do. Turns events into triggered
 * effects ({@link #fire}), resolves the effects on the stack ({@link #resolve}), applies damage and destruction by
 * effects (with the replacements T20 and T21), applies spells and equipment, and ends what lasts until a moment of the
 * turn ({@link #expire}).
 */
final class Game implements EffectStack.Hooks, EffectContext, PlayerEffectContext {

    final Ruleset rules;
    final AbilityBinder binder;
    final MatchState state;
    final CardEffects cardEffects;
    Evaluation evaluation;
    int cycle;

    final TriggerDispatcher triggers;
    final Destruction destruction;

    /** An equipment drawn and waiting for the next creature, per side (ruleset 10.6). */
    private final Map<Side, EquipmentCard> pendingEquipment = new LinkedHashMap<>();

    Game(Ruleset rules, AbilityBinder binder, MatchState state, Evaluation evaluation, CardEffects cardEffects) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.binder = Objects.requireNonNull(binder, "binder");
        this.state = Objects.requireNonNull(state, "state");
        this.evaluation = evaluation;
        this.cardEffects = Objects.requireNonNull(cardEffects, "cardEffects");
        this.triggers = new TriggerDispatcher(state, this::board);
        this.destruction = new Destruction(state, this::board);
    }

    Board board() {
        return new Board(binder, state, evaluation, cycle);
    }

    EffectStack stack(List<MatchEvent> log) {
        return new EffectStack(log, this);
    }

    int turn() {
        return state.turn();
    }

    Lasting lasting(String duration, Creature source) {
        return new Lasting(duration, turn(), Lasting.WHILE_SOURCE.equals(duration) ? source : null);
    }

    // ================================================================ triggers

    /** T2 effects at the start of {@code phase}. */
    void phase(String phase, List<MatchEvent> log) {
        fireAndResolve(List.of(new GameEvent.PhaseStart(phase)), log);
    }

    /** Fires {@code events} and resolves the stack. */
    void fireAndResolve(List<GameEvent> events, List<MatchEvent> log) {
        if (events.isEmpty()) {
            return;
        }
        EffectStack stack = stack(log);
        triggers.fire(events, stack);
        stack.resolveAll();
    }

    // ================================================================ stack hooks

    @Override
    public boolean allow(EffectStack.Item item) {
        Creature source = item.source();
        BoundExpansion e = item.expansion();
        if (e.evolution()) {
            if (!source.canTrigger(e) || board().evolutionsSuppressed(source)) {
                return false;
            }
            source.markTriggered(e);
            return true;
        }
        if (!source.canTrigger(e, turn())) {
            return false;
        }
        source.markTriggered(e, turn());
        return true;
    }

    @Override
    public void pushed(EffectStack.Item item, EffectStack stack) {
        if (item.expansion().evolution()) {
            triggers.fire(List.of(new GameEvent.EvolutionFired(item.side(), item.source())), stack);
        }
    }

    @Override
    public void resolve(EffectStack.Item item, EffectStack stack, List<MatchEvent> log) {
        BoundExpansion e = item.expansion().scaled(item.scalePermille());
        CreatureRef source = CreatureRef.of(item.side(), item.source());
        Board b = board();
        PlayerEffect playerEffect = PlayerEffects.find(e.effectId()).orElse(null);
        if (playerEffect != null) {
            Side player = e.targetId().equals(TargetIds.OPPONENT_PLAYER) ? item.side().other() : item.side();
            playerEffect.apply(this, new PlayerEffect.Use(e, item.side(), player, item.source(), log))
                    .ifPresent(r -> log.add(new EffectResolved(source, e.abilityId(), e.effectId(), r.target(),
                            r.changes(), OptionalInt.empty())));
            return;
        }
        List<Creature> targets;
        if (item.target() != null) {
            if (item.target().destroyed()) {
                log.add(new EffectFizzled(source, e.abilityId(), e.effectId(), FizzleReason.TARGET_DESTROYED));
                return;
            }
            targets = List.of(item.target());
        } else {
            targets = Targets.find(e.targetId()).orElseThrow(() -> new IllegalStateException(
                    "target " + e.targetId() + " has no selection for a triggered effect"))
                    .select(b, item.side(), item.source(), item.context(), e);
            if (targets.isEmpty()) {
                FizzleReason reason = Targets.find(e.targetId()).map(Target::single).orElse(false) ? FizzleReason.TARGET_DESTROYED : FizzleReason.NO_TARGET;
                log.add(new EffectFizzled(source, e.abilityId(), e.effectId(), reason));
                return;
            }
        }
        for (Creature t : targets) {
            if (t.destroyed() || t.leftPlay()) {
                continue;
            }
            Side side = b.sideOf(t);
            Followups after = new Followups();
            Outcome outcome = apply(e, item, side, t, after);
            log.add(new EffectResolved(source, e.abilityId(), e.effectId(), CreatureRef.of(side, t),
                    outcome.changes(), outcome.stacks()));
            log.addAll(after.logs());
            if (!after.events().isEmpty()) {
                triggers.fire(after.events(), stack);
            }
        }
    }

    // ================================================================ effects on a creature

    @Override
    public PlayerState player(Side side) {
        return state.player(side);
    }

    @Override
    public Creature newCreature(CreatureCard card, int lane) {
        return new Creature(card, lane, binder.bind(card));
    }

    @Override
    public AbilityBinder binder() {
        return binder;
    }

    @Override
    public boolean hasAbility(Creature creature, String abilityId) {
        return board().abilities(creature).stream().anyMatch(x -> x.abilityId().equals(abilityId));
    }

    @Override
    public boolean noBonus(Creature target) {
        return board().noBonus(target);
    }

    @Override
    public int adjacentAllyCount(Side side, Creature creature) {
        return board().adjacentAllies(side, creature).size();
    }

    @Override
    public boolean saveFromDestruction(Side side, Creature t, Followups after) {
        return destruction.saveFromDestruction(side, t, after);
    }

    @Override
    public List<StatChange> damage(Side sourceSide, Creature source, Side side, Creature t, int amount, Followups after) {
        return destruction.damage(sourceSide, source, side, t, amount, after);
    }

    @Override
    public void destroy(Side sourceSide, Creature source, Side side, Creature t, Followups after) {
        destruction.destroy(sourceSide, source, side, t, after);
    }

    @Override
    public void destroyed(Side side, Creature t, Side sourceSide, Creature source, Followups after) {
        destruction.destroyed(side, t, sourceSide, source, after);
    }

    @Override
    public void discard(Side side, Creature t, Followups after) {
        destruction.discard(side, t, after);
    }

    @Override
    public int healPermille(Creature target) {
        return board().healPermille(target);
    }

    /**
     * Applies a triggered effect to the living creature {@code t} of {@code side} through its {@link Effect} class.
     * {@code item} gives its source and trigger context. An ID with no class is an engine bug: the registry only lets
     * through effects that have one.
     */
    private Outcome apply(BoundExpansion e, EffectStack.Item item, Side side, Creature t, Followups after) {
        Creature source = item.source();
        Lasting lasting = lasting(e.duration(), source);
        Effect effect = Effects.find(e.effectId()).orElseThrow(
                () -> new IllegalStateException("no resolution for effect " + e.effectId() + " on a creature"));
        return effect.apply(this, new Effect.Use(e, item.side(), side, source, lasting, item.context(), after), t);
    }

    // ================================================================ damage and destruction

    // ================================================================ durations

    /** Ends what lasts until {@code moment} ({@link Lasting}) on every creature and spell aura. */
    void expire(String moment) {
        for (Side side : Side.values()) {
            PlayerState p = state.player(side);
            p.expireAuras(moment, turn());
            for (Creature c : p.creatures()) {
                c.expire(moment, turn());
            }
        }
    }

    // ================================================================ Refill: draws, spells, equipment

    /** The equipment {@code side} drew and that waits for a creature, if any. */
    EquipmentCard pendingEquipment(Side side) {
        return pendingEquipment.get(side);
    }

    void clearPendingEquipment(Side side) {
        pendingEquipment.remove(side);
    }

    /**
     * Draws the top card of {@code side}'s deck for {@code lane} (ruleset 10.4 to 10.6): a creature is placed there,
     * a spell resolves, an equipment waits for the next creature. Returns true if the lane was filled.
     */
    @Override
    public boolean draw(Side side, int lane, List<MatchEvent> log) {
        PlayerState player = state.player(side);
        Card card = player.deck().remove(0);
        switch (card) {
            case CreatureCard creatureCard -> {
                place(side, new Creature(creatureCard, lane, binder.bind(creatureCard)), true, log);
                return true;
            }
            case SpellCard spell -> {
                cardEffects.onSpell(spell, side);
                player.discard().add(spell);
                log.add(new SpellResolved(side, lane, spell.id()));
                castSpell(side, spell, log);
                return false;
            }
            case EquipmentCard equipment -> {
                EquipmentCard previous = pendingEquipment.put(side, equipment);
                if (previous != null) {
                    player.discard().add(previous);
                    log.add(new EquipmentLost(side, previous.id()));
                }
                log.add(new EquipmentPending(side, equipment.id()));
                return false;
            }
        }
    }

    /**
     * Places {@code creature} in its empty lane: the effects waiting on the lane (E64), then T29 (10.4), then the
     * equipment waiting for it if it was drawn.
     */
    @Override
    public void place(Side side, Creature creature, boolean drawn, List<MatchEvent> log) {
        PlayerState player = state.player(side);
        player.place(creature);
        if (drawn) {
            log.add(new CardDrawn(side, creature.lane(), creature.card().id()));
        } else {
            log.add(new MatchEvent.CreatureReturned(side, creature.lane(), creature.card().id()));
        }
        for (LaneEffect waiting : player.takeLaneEffects(creature.lane())) {
            List<StatChange> changes = immediate(side, waiting.effect(), creature, log);
            log.add(new MatchEvent.CardEffectResolved(side, waiting.cardId(), waiting.effect().abilityId(),
                    waiting.effect().effectId(), CreatureRef.of(side, creature), changes));
        }
        onReplacementPlaced(side, creature, log);
        EquipmentCard equipment = pendingEquipment.get(side);
        if (drawn && equipment != null && !creature.destroyed()) {
            pendingEquipment.remove(side);
            cardEffects.onEquipment(equipment, side, creature);
            log.add(new EquipmentApplied(side, creature.lane(), equipment.id(), creature.card().id()));
            equip(side, equipment, creature, log);
            player.discard().add(equipment);
        }
    }

    /**
     * T29 (10.4): a creature was placed as a replacement. The abilities of the player (suit synergies), of the
     * creatures in play and of the player's spells (auras) that trigger on it are pushed and resolved at once; their
     * target S27 is the replacement.
     */
    void onReplacementPlaced(Side side, Creature replacement, List<MatchEvent> log) {
        EffectStack stack = stack(log);
        Board b = board();
        PlayerState player = state.player(side);
        TriggerContext ctx = TriggerContext.placed(replacement);
        for (BoundExpansion e : player.playerAbilities()) {
            if (e.triggerId().equals("T29") && b.holds(e, side, replacement, Board.Ctx.of(ctx))) {
                stack.push(EffectStack.Item.onCreature(side, replacement, e, "T29", ctx));
            }
        }
        for (Creature source : player.creatures()) {
            if (source == replacement || source.destroyed()) {
                continue;
            }
            for (BoundExpansion e : b.abilities(source, "T29")) {
                if (b.holds(e, side, replacement, Board.Ctx.of(ctx))) {
                    stack.push(new EffectStack.Item(side, source, e.targetId().equals(TargetIds.REPLACEMENT) ? replacement : source, e,
                            "T29", triggers.scaleOf(b, side, source, e), ctx));
                }
            }
        }
        for (Aura aura : player.auras()) {
            BoundExpansion e = aura.expansion();
            if ("T29".equals(e.triggerId()) && aura.canTrigger() && b.holds(e, side, replacement, Board.Ctx.of(ctx))) {
                aura.markTriggered();
                stack.push(EffectStack.Item.onCreature(side, replacement, e, "T29", ctx));
            }
        }
        stack.resolveAll();
    }

    /**
     * An equipment applied to {@code receiver} (10.6): its immediate effects, in card order, then its lasting ones go
     * to the creature (S27 becomes S1). A static DEF change (E2 with T0) is applied once now and stays.
     */
    void equip(Side owner, EquipmentCard card, Creature receiver, List<MatchEvent> log) {
        List<BoundExpansion> lasting = new ArrayList<>();
        for (BoundExpansion e : binder.bindLasting(card)) {
            if ("T0".equals(e.triggerId()) && e.effectId().equals("E2")) {
                BoundExpansion now = e.asImmediate(TargetIds.REPLACEMENT, Lasting.PERMANENT);
                List<StatChange> changes = immediate(owner, now, receiver, log);
                log.add(new MatchEvent.CardEffectResolved(owner, card.id(), e.abilityId(), e.effectId(),
                        CreatureRef.of(owner, receiver), changes));
                continue;
            }
            boolean replacement = "T29".equals(e.triggerId());
            lasting.add(replacement || !e.targetId().equals(TargetIds.REPLACEMENT) ? e : e.withTarget(TargetIds.SELF));
        }
        applyImmediate(owner, card, receiver, log);
        receiver.equip(lasting);
    }

    /**
     * A spell (10.5): its immediate effects in card order, then its lasting ones: a static effect aimed at one chosen
     * creature (S33-S40) is given to that creature now; the others stay with the player as auras. Then T18.
     */
    void castSpell(Side owner, SpellCard card, List<MatchEvent> log) {
        Map<String, Creature> chosen = applyImmediate(owner, card, null, log);
        Board b = board();
        for (BoundExpansion e : binder.bindLasting(card)) {
            Lasting lasting = new Lasting(e.duration(), turn(), null);
            boolean selection = Targets.find(e.targetId()).filter(RankedTarget.class::isInstance).isPresent();
            if ("T0".equals(e.triggerId()) && selection) {
                Creature target = chosen.containsKey(e.targetId()) ? chosen.get(e.targetId())
                        : b.select(e.targetId(), e.targetParams(), owner).orElse(null);
                if (target != null && !target.destroyed()) {
                    chosen.putIfAbsent(e.targetId(), target);
                    target.grant(new Grant(e.abilityId(), List.of(e.withTarget(TargetIds.SELF)), lasting, false));
                }
            } else {
                state.player(owner).addAura(new Aura(card.id(), e, lasting));
            }
        }
        fireAndResolve(List.of(new GameEvent.SpellCast(owner, card.id())), log);
    }

    /**
     * The immediate (class I) effects of a spell or an equipment, in card order, on the creatures, players, lanes or
     * cards they aim at. {@code receiver} is the creature an equipment was applied to. Returns the creatures chosen
     * by selection targets, by target id (S44 {@code of} uses them).
     */
    Map<String, Creature> applyImmediate(Side owner, Card card, Creature receiver, List<MatchEvent> log) {
        Map<String, Creature> chosen = new LinkedHashMap<>();
        for (BoundExpansion e : binder.bindImmediate(card)) {
            Board b = board();
            PlayerEffect playerEffect = PlayerEffects.find(e.effectId()).orElse(null);
            if (playerEffect != null) {
                Side player = e.targetId().equals(TargetIds.OPPONENT_PLAYER) ? owner.other() : owner;
                playerEffect.apply(this, new PlayerEffect.Use(e, owner, player, null, log))
                        .ifPresent(r -> log.add(new MatchEvent.CardEffectResolved(owner, card.id(), e.abilityId(),
                                e.effectId(), r.target(), r.changes())));
                continue;
            }
            List<Creature> targets = Targets.find(e.targetId()).orElseThrow(
                    () -> new IllegalStateException("not a selection target: " + e.targetId()))
                    .selectImmediate(b, e, owner, receiver, chosen);
            for (Creature t : targets) {
                if (t.destroyed() || t.leftPlay()) {
                    continue;
                }
                Side side = board().sideOf(t);
                Followups after = new Followups();
                List<StatChange> changes = immediateOn(owner, e, side, t, after);
                log.add(new MatchEvent.CardEffectResolved(owner, card.id(), e.abilityId(), e.effectId(),
                        CreatureRef.of(side, t), changes));
                log.addAll(after.logs());
                fireAndResolve(after.events(), log);
            }
        }
        return chosen;
    }

    /** An immediate effect on one creature (a spell's, an equipment's or one waiting on a lane), logged by the caller. */
    List<StatChange> immediate(Side owner, BoundExpansion e, Creature t, List<MatchEvent> log) {
        Side side = board().sideOf(t);
        Followups after = new Followups();
        List<StatChange> changes = immediateOn(owner, e, side, t, after);
        log.addAll(after.logs());
        fireAndResolve(after.events(), log);
        return changes;
    }

    private List<StatChange> immediateOn(Side owner, BoundExpansion e, Side side, Creature t, Followups after) {
        Effect effect = Effects.find(e.effectId())
                .orElseThrow(() -> new IllegalStateException("no immediate resolution for effect " + e.effectId()));
        return effect.apply(this, new Effect.Use(e, owner, side, null, Lasting.permanent(), TriggerContext.NONE,
                after), t).changes();
    }
}
