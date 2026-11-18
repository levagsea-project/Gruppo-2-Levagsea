package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.Effect;
import quorma.gameengine.effects.EffectContext;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.Effects;
import quorma.gameengine.effects.Followups;
import quorma.gameengine.effects.Outcome;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.CreatureSaved;
import quorma.gameengine.log.MatchEvent.EffectFizzled;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.EquipmentApplied;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.EquipmentPending;
import quorma.gameengine.log.MatchEvent.SpellResolved;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.log.TargetRef;
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
final class Game implements EffectStack.Hooks, EffectContext {

    final Ruleset rules;
    final AbilityBinder binder;
    final MatchState state;
    final CardEffects cardEffects;
    Evaluation evaluation;
    int cycle;

    /** An equipment drawn and waiting for the next creature, per side (ruleset 10.6). */
    private final Map<Side, EquipmentCard> pendingEquipment = new LinkedHashMap<>();

    Game(Ruleset rules, AbilityBinder binder, MatchState state, Evaluation evaluation, CardEffects cardEffects) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.binder = Objects.requireNonNull(binder, "binder");
        this.state = Objects.requireNonNull(state, "state");
        this.evaluation = evaluation;
        this.cardEffects = Objects.requireNonNull(cardEffects, "cardEffects");
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

    /** Fires {@code events} and resolves the stack. */
    void fireAndResolve(List<GameEvent> events, List<MatchEvent> log) {
        if (events.isEmpty()) {
            return;
        }
        EffectStack stack = stack(log);
        fire(events, stack);
        stack.resolveAll();
    }

    private record Candidate(int sideRank, int lane, int seq, EffectStack.Item item) {}

    /**
     * Pushes the triggered abilities that listen to {@code events}, in ruleset 4.3 order: Potenze Artificiali
     * first, each side in lane order (the player's own abilities, such as suit tiers, after its creatures), a
     * creature's abilities in order. Conditions are checked now.
     */
    void fire(List<GameEvent> events, EffectStack stack) {
        Board b = board();
        List<Candidate> out = new ArrayList<>();
        Side[] order = b.prioritySides();
        for (GameEvent event : events) {
            switch (event) {
                case GameEvent.Destroyed d -> {
                    TriggerContext ctx = TriggerContext.destroyed(d.victim(), d.killerSide(), d.killer());
                    listen(b, out, order, d.side(), d.victim(), "T10", e -> true, ctx, true);
                    for (Creature c : state.player(d.side()).creatures()) {
                        if (c != d.victim()) {
                            listen(b, out, order, d.side(), c, "T36", e -> true, ctx, false);
                        }
                    }
                    for (Creature c : state.player(d.side().other()).creatures()) {
                        listen(b, out, order, d.side().other(), c, "T37", e -> true, ctx, false);
                    }
                    if (d.killer() != null) {
                        listen(b, out, order, d.killerSide(), d.killer(), "T19", e -> true, ctx, true);
                    }
                }
                case GameEvent.Damaged d -> {
                    TriggerContext ctx = TriggerContext.damagedBy(d.damagerSide(), d.damager(), d.amount());
                    listen(b, out, order, d.side(), d.victim(), "T13", e -> {
                        String source = e.triggerParam("source").orElse("qualsiasi");
                        return source.equals("qualsiasi") || source.equals(d.combat() ? "combattimento" : "effetto");
                    }, ctx, true);
                }
                case GameEvent.Dealt d -> listen(b, out, order, d.side(), d.attacker(), "T38", e -> true,
                        TriggerContext.dealt(d.victimSide(), d.victim(), d.amount(), d.killed()), true);
                case GameEvent.Survived s -> listen(b, out, order, s.side(), s.creature(), "T12", e -> true,
                        TriggerContext.NONE, false);
                case GameEvent.Spilled s -> listen(b, out, order, s.side(), s.attacker(), "T11", e -> true,
                        TriggerContext.NONE, true);
                case GameEvent.EvolutionFired f -> {
                    for (Creature c : state.player(f.side()).creatures()) {
                        listen(b, out, order, f.side(), c, "T15", e -> {
                            boolean ally = e.triggerParam("source").orElse("self").equals("ally");
                            return ally ? c != f.creature() : c == f.creature();
                        }, TriggerContext.NONE, false);
                    }
                }
                case GameEvent.SpellCast s -> {
                    for (Side side : order) {
                        for (Creature c : state.player(side).creatures()) {
                            listen(b, out, order, side, c, "T18", e -> {
                                String controller = e.triggerParam("controller").orElse("any");
                                boolean byController = controller.equals("any")
                                        || controller.equals(side == s.side() ? "own" : "opponent");
                                return byController && e.triggerParam("spell_id").map(id -> id.equals(s.cardId())).orElse(true);
                            }, TriggerContext.NONE, false);
                        }
                    }
                }
                case GameEvent.PhaseStart p -> {
                    for (Side side : order) {
                        for (Creature c : state.player(side).creatures()) {
                            listen(b, out, order, side, c, "T2", e -> e.triggerParam("phase").orElse("").equals(p.phase()),
                                    TriggerContext.NONE, false);
                        }
                    }
                }
                case GameEvent.EndOfTurn _ -> {
                    for (Side side : order) {
                        for (Creature c : state.player(side).creatures()) {
                            listen(b, out, order, side, c, "T26", e -> true, TriggerContext.NONE, false);
                        }
                    }
                }
                case GameEvent.Attacks a -> {
                    listen(b, out, order, a.side(), a.attacker(), "T6", e -> true,
                            TriggerContext.attacking(a.targetSide(), a.target(), a.side(), a.attacker()), false);
                    listen(b, out, order, a.targetSide(), a.target(), "T7", e -> true,
                            TriggerContext.attacking(a.targetSide(), a.target(), a.side(), a.attacker()), false);
                }
            }
        }
        out.sort(Comparator.comparingInt(Candidate::sideRank).thenComparingInt(Candidate::lane)
                .thenComparingInt(Candidate::seq));
        for (Candidate c : out) {
            stack.push(c.item());
        }
    }

    private void listen(Board b, List<Candidate> out, Side[] order, Side side, Creature source, String trigger,
            java.util.function.Predicate<BoundExpansion> params, TriggerContext ctx, boolean evenIfDestroyed) {
        if (source.destroyed() && !evenIfDestroyed) {
            return;
        }
        for (BoundExpansion e : b.abilities(source, trigger)) {
            if (e.effectId().equals("E24") || e.effectId().equals("E17") || !params.test(e)
                    || !b.holds(e, side, source, Board.Ctx.of(ctx))) {
                continue;
            }
            Creature fixed = e.targetId().equals("S1") ? source : null;
            out.add(new Candidate(side == order[0] ? 0 : 1, source.lane(), out.size(),
                    new EffectStack.Item(side, source, fixed, e, trigger, scaleOf(b, side, source, e), ctx)));
        }
    }

    /** The factor of the creature's E19 (Spira 4) for an Evolution, 1000 otherwise. */
    int scaleOf(Board b, Side side, Creature source, BoundExpansion e) {
        if (!e.evolution() || source.destroyed()) {
            return 1000;
        }
        long factor = 1000;
        for (Board.Active a : b.reaching("E19", source, Board.Ctx.NONE)) {
            factor = factor * a.e().intParam("factor") / 1000;
        }
        return Math.toIntExact(factor);
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
            fire(List.of(new GameEvent.EvolutionFired(item.side(), item.source())), stack);
        }
    }

    @Override
    public void resolve(EffectStack.Item item, EffectStack stack, List<MatchEvent> log) {
        BoundExpansion e = item.expansion().scaled(item.scalePermille());
        CreatureRef source = CreatureRef.of(item.side(), item.source());
        Board b = board();
        switch (e.targetId()) {
            case "S30", "S31" -> {
                Side player = e.targetId().equals("S31") ? item.side() : item.side().other();
                StatChange change = changeLife(e, player);
                log.add(new EffectResolved(source, e.abilityId(), e.effectId(), new TargetRef.PlayerRef(player),
                        List.of(change), OptionalInt.empty()));
                return;
            }
            case "S4" -> {
                PlayerState p = state.player(item.side());
                BoundExpansion pending = laneEffect(e);
                p.addLaneEffect(new LaneEffect(item.source().lane(), item.source().card().id(), pending));
                log.add(new EffectResolved(source, e.abilityId(), e.effectId(),
                        new TargetRef.LaneRef(item.side(), item.source().lane()), List.of(), OptionalInt.empty()));
                return;
            }
            default -> { }
        }
        List<Creature> targets;
        if (item.target() != null) {
            if (item.target().destroyed()) {
                log.add(new EffectFizzled(source, e.abilityId(), e.effectId(), FizzleReason.TARGET_DESTROYED));
                return;
            }
            targets = List.of(item.target());
        } else {
            targets = targets(b, item);
            if (targets.isEmpty()) {
                FizzleReason reason = singleCreatureTarget(e.targetId()) ? FizzleReason.TARGET_DESTROYED : FizzleReason.NO_TARGET;
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
                fire(after.events(), stack);
            }
        }
    }

    private static boolean singleCreatureTarget(String targetId) {
        return List.of("S1", "S2", "S26", "S27", "S43", "S6", "S7").contains(targetId);
    }

    /** The living creatures a triggered effect reaches when it resolves. */
    private List<Creature> targets(Board b, EffectStack.Item item) {
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

    private static void add(List<Creature> out, Creature c) {
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

    private BoundExpansion laneEffect(BoundExpansion e64) {
        String effectId = e64.stringParam("effect_id").orElseThrow();
        String duration = e64.stringParam("duration").orElseThrow();
        return new BoundExpansion(e64.abilityId(), null, Map.of(), effectId, "S27", Map.of(), List.of(), duration,
                ResClass.I, e64.nested("effect_params"), OptionalInt.empty(), Optional.empty(), false, 0, false,
                e64.index());
    }

    // ================================================================ effects on a creature

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
        Board.Active rescue = rescue(side, t);
        if (rescue == null) {
            return false;
        }
        after.logs().add(new CreatureSaved(side, t.lane(), t.card().id(), rescue.e().abilityId()));
        return true;
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

    /** E8 (lose) or E9 (gain) life. */
    private StatChange changeLife(BoundExpansion e, Side player) {
        PlayerState p = state.player(player);
        int before = p.life();
        switch (e.effectId()) {
            case "E8" -> p.loseLife(e.intParam("amount"));
            case "E9" -> p.gainLife(e.intParam("amount"));
            default -> throw new IllegalStateException("effect " + e.effectId() + " does not aim at a player");
        }
        return new StatChange(StatChange.Stat.LIFE, before, p.life());
    }

    // ================================================================ damage and destruction

    /**
     * Damage by an effect (not combat): T21 replacements first (E29), then T20 if it would destroy. Logs the
     * destruction or the save after the effect's own line, and fires T13 and the destruction triggers.
     *
     * @param source the creature whose ability deals it, null for a spell
     */
    @Override
    public List<StatChange> damage(Side sourceSide, Creature source, Side side, Creature t, int amount, Followups after) {
        int before = t.currentDef();
        if (t.destroyed() || amount <= 0) {
            return List.of(new StatChange(StatChange.Stat.DEF, before, before));
        }
        amount = preventDamage(t, amount).left();
        if (amount <= 0) {
            return List.of(new StatChange(StatChange.Stat.DEF, before, before));
        }
        after.events().add(new GameEvent.Damaged(side, t, amount, sourceSide, source, false));
        if (amount >= before && saveFromDestruction(side, t, after)) {
            return List.of(new StatChange(StatChange.Stat.DEF, before, t.currentDef()));
        }
        t.takeDamage(amount);
        if (t.destroyed()) {
            destroyed(side, t, sourceSide, source, after);
        }
        return List.of(new StatChange(StatChange.Stat.DEF, before, t.currentDef()));
    }

    /** What is left of some damage after T21 replacements, and the last ability that prevented part of it. */
    record Prevention(int left, String abilityId) {}

    /** T21 with E29: prevents the next damage ({@code once}) or part of it ({@code amount}). */
    Prevention preventDamage(Creature t, int amount) {
        String by = null;
        for (Board.Active a : board().replacements("T21", t)) {
            if (!a.e().effectId().equals("E29") || amount <= 0) {
                continue;
            }
            markReplacement(a, t);
            by = a.e().abilityId();
            amount = a.e().boolParam("once", false) ? 0 : Math.max(0, amount - a.e().intParam("amount", 0));
        }
        return new Prevention(amount, by);
    }

    /** E10: destroys {@code t} (not damage), unless a T20 replacement saves it. */
    @Override
    public void destroy(Side sourceSide, Creature source, Side side, Creature t, Followups after) {
        if (t.destroyed()) {
            return;
        }
        if (saveFromDestruction(side, t, after)) {
            return;
        }
        t.markDestroyed();
        destroyed(side, t, sourceSide, source, after);
    }

    @Override
    public void destroyed(Side side, Creature t, Side sourceSide, Creature source, Followups after) {
        after.logs().add(new CreatureDestroyed(side, t.lane(), t.card().id()));
        after.events().add(new GameEvent.Destroyed(side, t, source != null ? sourceSide : null, source));
        if (source != null) {
            source.endUntilKill();
        }
    }

    /** E13: puts {@code t} in its owner's discard pile, with no destruction (no T10, no T20). */
    @Override
    public void discard(Side side, Creature t, Followups after) {
        PlayerState p = state.player(side);
        p.remove(t.lane());
        p.discard().add(t.card());
        after.logs().add(new CreatureDiscarded(side, t.lane(), t.card().id()));
    }

    /**
     * T20 "about to be destroyed": the first replacement that saves {@code target} (E12: DEF back to the base DEF,
     * destroyed at the next Refill if {@code then} says so, a lockout set; E11: DEF set to {@code set_def}). A creature
     * already saved and waiting for Refill is not saved again. Returns the replacement, or null.
     */
    Board.Active rescue(Side side, Creature target) {
        if (target.condemned()) {
            return null;
        }
        for (Board.Active a : board().replacements("T20", target)) {
            BoundExpansion e = a.e();
            switch (e.effectId()) {
                case "E12" -> {
                    if (e.stringParam("lockout.key").isPresent()) {
                        state.player(side).lock(e.stringParam("lockout.key").orElseThrow(),
                                Suit.valueOf(e.stringParam("lockout.suit").orElseThrow().toUpperCase(java.util.Locale.ROOT)),
                                e.intParam("lockout.reactivate_below"));
                    }
                    target.saveWithFullDef("destroy_at_next_refill".equals(e.stringParam("then").orElse("none")));
                }
                case "E11" -> {
                    target.setDef(e.intParam("set_def"));
                    target.undestroy();
                }
                default -> {
                    continue;
                }
            }
            markReplacement(a, target);
            return a;
        }
        return null;
    }

    /** Counts a replacement that applied: on its aura, or on the creature whose ability it is. */
    void markReplacement(Board.Active a, Creature c) {
        if (a.aura() != null) {
            a.aura().markTriggered();
        } else {
            (a.source() != null ? a.source() : c).markTriggered(a.e(), turn());
        }
    }

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
    boolean draw(Side side, int lane, List<MatchEvent> log) {
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
    void place(Side side, Creature creature, boolean drawn, List<MatchEvent> log) {
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
                stack.push(new EffectStack.Item(side, replacement, replacement, e, "T29", 1000, ctx));
            }
        }
        for (Creature source : player.creatures()) {
            if (source == replacement || source.destroyed()) {
                continue;
            }
            for (BoundExpansion e : b.abilities(source, "T29")) {
                if (b.holds(e, side, replacement, Board.Ctx.of(ctx))) {
                    stack.push(new EffectStack.Item(side, source, e.targetId().equals("S27") ? replacement : source, e,
                            "T29", scaleOf(b, side, source, e), ctx));
                }
            }
        }
        for (Aura aura : player.auras()) {
            BoundExpansion e = aura.expansion();
            if ("T29".equals(e.triggerId()) && aura.canTrigger() && b.holds(e, side, replacement, Board.Ctx.of(ctx))) {
                aura.markTriggered();
                stack.push(new EffectStack.Item(side, replacement, replacement, e, "T29", 1000, ctx));
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
                BoundExpansion now = e.asImmediate("S27", Lasting.PERMANENT);
                List<StatChange> changes = immediate(owner, now, receiver, log);
                log.add(new MatchEvent.CardEffectResolved(owner, card.id(), e.abilityId(), e.effectId(),
                        CreatureRef.of(owner, receiver), changes));
                continue;
            }
            boolean replacement = "T29".equals(e.triggerId());
            lasting.add(replacement || !e.targetId().equals("S27") ? e : e.withTarget("S1"));
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
            boolean selection = List.of("S33", "S35", "S37", "S38", "S39", "S40").contains(e.targetId());
            if ("T0".equals(e.triggerId()) && selection) {
                Creature target = chosen.containsKey(e.targetId()) ? chosen.get(e.targetId())
                        : b.select(e.targetId(), e.targetParams(), owner).orElse(null);
                if (target != null && !target.destroyed()) {
                    chosen.putIfAbsent(e.targetId(), target);
                    target.grant(new Grant(e.abilityId(), List.of(e.withTarget("S1")), lasting, false));
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
            switch (e.targetId()) {
                case "S30", "S31" -> {
                    Side player = e.targetId().equals("S31") ? owner : owner.other();
                    if (e.effectId().equals("E57")) {
                        mill(player, e, log);
                        continue;
                    }
                    if (e.effectId().equals("E35")) {
                        drawFirst(player, log);
                        continue;
                    }
                    StatChange change = changeLife(e, player);
                    log.add(new MatchEvent.CardEffectResolved(owner, card.id(), e.abilityId(), e.effectId(),
                            new TargetRef.PlayerRef(player), List.of(change)));
                    continue;
                }
                case "S32" -> {
                    returnFromDiscard(owner, e, card, log);
                    continue;
                }
                case "S47" -> {
                    moveInDeck(owner, e, log);
                    continue;
                }
                default -> { }
            }
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

    /** E57: the top {@code count} cards of {@code player}'s deck go to the discard pile with no effect. */
    private void mill(Side player, BoundExpansion e, List<MatchEvent> log) {
        PlayerState p = state.player(player);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < e.intParam("count") && !p.deck().isEmpty(); i++) {
            Card c = p.deck().remove(0);
            p.discard().add(c);
            ids.add(c.id());
        }
        log.add(new MatchEvent.CardsMilled(player, e.abilityId(), ids));
    }

    /**
     * E35: draws the top card. A creature goes to the lowest empty lane (if there is none it stays on the deck), a
     * spell resolves at once, an equipment waits for the next creature drawn.
     */
    private void drawFirst(Side player, List<MatchEvent> log) {
        PlayerState p = state.player(player);
        if (p.deck().isEmpty()) {
            return;
        }
        if (p.deck().get(0) instanceof CreatureCard) {
            OptionalInt lane = p.lowestEmptyLane();
            if (lane.isPresent()) {
                draw(player, lane.getAsInt(), log);
            }
            return;
        }
        draw(player, p.lowestEmptyLane().orElse(1), log);
    }

    /**
     * E36: places a creature in the lowest empty lane of the owner. With {@code source: discard} it is the last
     * creature that went to the discard pile, back with full DEF and no modifiers; with {@code source: deck}, the top
     * card if it is a creature. Nothing happens with no empty lane or no creature.
     */
    private void returnFromDiscard(Side owner, BoundExpansion e, Card card, List<MatchEvent> log) {
        PlayerState p = state.player(owner);
        OptionalInt lane = e.effectParams().containsKey("lane") ? (p.creatureAt(e.intParam("lane")).isEmpty()
                ? OptionalInt.of(e.intParam("lane")) : OptionalInt.empty()) : p.lowestEmptyLane();
        if (lane.isEmpty()) {
            return;
        }
        if (e.stringParam("source").orElse("deck").equals("discard")) {
            for (int i = p.discard().size() - 1; i >= 0; i--) {
                if (p.discard().get(i) instanceof CreatureCard creatureCard) {
                    p.discard().remove(i);
                    place(owner, new Creature(creatureCard, lane.getAsInt(), binder.bind(creatureCard)), false, log);
                    return;
                }
            }
            return;
        }
        if (!p.deck().isEmpty() && p.deck().get(0) instanceof CreatureCard creatureCard) {
            p.deck().remove(0);
            place(owner, new Creature(creatureCard, lane.getAsInt(), binder.bind(creatureCard)), true, log);
        }
    }

    /** E49 on S47: the highest-ranked creature among the top {@code count} cards goes on top (or to the bottom). */
    private void moveInDeck(Side owner, BoundExpansion e, List<MatchEvent> log) {
        PlayerState p = state.player(owner);
        int count = ((Value.IntV) e.targetParams().get("count")).v() > Integer.MAX_VALUE ? Integer.MAX_VALUE
                : Math.toIntExact(((Value.IntV) e.targetParams().get("count")).v());
        int best = -1;
        int bestRank = -1;
        for (int i = 0; i < Math.min(count, p.deck().size()); i++) {
            if (p.deck().get(i) instanceof CreatureCard c) {
                int rank = c.rank() == 1 ? 14 : c.rank();
                if (rank > bestRank) {
                    bestRank = rank;
                    best = i;
                }
            }
        }
        if (best < 0) {
            return;
        }
        Card card = p.deck().remove(best);
        String position = e.stringParam("position").orElse("top");
        if (position.equals("top")) {
            p.deck().add(0, card);
        } else {
            p.deck().add(card);
        }
        log.add(new MatchEvent.CardMoved(owner, card.id(), position));
    }
}
