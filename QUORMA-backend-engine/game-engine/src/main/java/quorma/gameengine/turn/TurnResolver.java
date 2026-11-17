package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.EffectResolver;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandEvaluator;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.hand.WildSuit;
import quorma.gameengine.log.BoardSnapshot;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.CreatureSaved;
import quorma.gameengine.log.MatchEvent.EquipmentApplied;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.EquipmentPending;
import quorma.gameengine.log.MatchEvent.HandEvaluated;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.MatchEvent.MatchEnded;
import quorma.gameengine.log.MatchEvent.SpellResolved;
import quorma.gameengine.log.MatchEvent.TargetDeclared;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.SkipReason;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Target;

/**
 * Plays one turn: Evaluate, Declare, Combat (Cycle 1, Cycle 2), Refill, End of turn (ruleset 6 to 11).
 *
 * <p>The hand evaluator and comparator are parameters so that the turn code does not wait for
 * {@link HandEvaluator}: tests pass fixed results. Every phase is public so that tests can run it alone.
 */
public final class TurnResolver {

    private final Ruleset rules;
    private final Function<List<HandCard>, HandResult> handEvaluator;
    private final Comparator<HandResult> handComparator;
    private final CardEffects cardEffects;
    private final AbilityBinder binder;

    public TurnResolver(
            Ruleset rules,
            Function<List<HandCard>, HandResult> handEvaluator,
            Comparator<HandResult> handComparator,
            CardEffects cardEffects) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.handEvaluator = Objects.requireNonNull(handEvaluator, "handEvaluator");
        this.handComparator = Objects.requireNonNull(handComparator, "handComparator");
        this.cardEffects = Objects.requireNonNull(cardEffects, "cardEffects");
        this.binder = new AbilityBinder(rules);
    }

    /** The real evaluator and comparator, and spell and equipment hooks that do nothing. */
    public TurnResolver(Ruleset rules) {
        this(rules, HandEvaluator::evaluate, HandEvaluator::compare, CardEffects.NONE);
    }

    /** Plays the turn of {@code state}: every phase in order. Moves {@code state} to the next turn if the match goes on. */
    public TurnReport playTurn(MatchState state) {
        int turn = state.turn();
        List<MatchEvent> opening = new ArrayList<>();
        Evaluation evaluation = evaluate(state);
        opening.add(new HandEvaluated(turn, Side.A, evaluation.a(), evaluation.multiplierA()));
        opening.add(new HandEvaluated(turn, Side.B, evaluation.b(), evaluation.multiplierB()));
        forceEvolutions(state, opening);

        List<Attack> attacks = declare(state, evaluation);
        for (Attack attack : attacks) {
            opening.add(new TargetDeclared(attack));
        }
        attacks = declareTriggers(state, attacks, opening);
        List<MatchEvent> cycle1 = new ArrayList<>();
        cycle1(state, evaluation, attacks, cycle1);
        List<MatchEvent> cycle2 = new ArrayList<>();
        cycle2(state, evaluation, attacks, cycle2);
        List<MatchEvent> refill = new ArrayList<>();
        refill(state, refill);
        List<MatchEvent> closing = new ArrayList<>();
        Optional<MatchResult> result = endOfTurn(state, evaluation, closing);
        BoardSnapshot board = BoardSnapshot.of(state);
        if (result.isEmpty() && state.turn() < MatchState.LAST_TURN) {
            state.nextTurn();
        }
        return new TurnReport(turn, evaluation, attacks, opening, cycle1, cycle2, refill, closing, board, result);
    }

    /** The best hand of one side. A Camaleonte takes the suit that helps most ({@link WildSuit}). */
    private HandResult hand(List<Creature> creatures) {
        List<HandCard> cards = new ArrayList<>();
        Set<Integer> wildLanes = new HashSet<>();
        for (Creature creature : creatures) {
            cards.add(new HandCard(creature.lane(), creature.card().rank(), creature.card().suit()));
            if (creature.wildSuit()) {
                wildLanes.add(creature.lane());
            }
        }
        return WildSuit.evaluate(cards, wildLanes, handEvaluator, handComparator);
    }

    /** Evaluate (7): each player's hand from their creatures in lane order, and the multiplier. */
    public Evaluation evaluate(MatchState state) {
        HandResult handA = hand(state.player(Side.A).creatures());
        HandResult handB = hand(state.player(Side.B).creatures());

        int multiplierA = 0;
        if (handA.type() != HandType.NONE) {
            multiplierA = rules.handMultiplierPermille(handA.type().name());
        }

        int multiplierB = 0;
        if (handB.type() != HandType.NONE) {
            multiplierB = rules.handMultiplierPermille(handB.type().name());
        }
        Synergies synergies = SynergyTiers.evaluate(rules, state);
        SynergyTiers.bind(binder, rules, state, synergies);
        return new Evaluation(handA, handB, multiplierA, multiplierB, synergies);
    }

    /**
     * Declare (8): one attack for each attacker of each hand, with its frozen target. Ordered by attacker lane,
     * and within a lane by faction priority, then {@code Side.A} first.
     */
    public List<Attack> declare(MatchState state, Evaluation evaluation) {
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

    /**
     * Cycle 1 (9.2, 9.5 to 9.7): attacks on the opposite lane, simultaneous. Once every blow has landed, the
     * kills trigger and the stack resolves. Appends events to {@code log}.
     */
    public void cycle1(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        record Blow(Attack attack, Creature attacker, Creature target, Strike strike) {}

        // Every blow is worked out from the state at the start of the cycle, then they all land.
        CycleStart start = CycleStart.of(state, evaluation);
        List<Blow> blows = new ArrayList<>();
        for (Attack attack : attacks) {
            if (!attack.inCycle1() || attack.isRedirected()) continue;

            Target targetRef = attack.target().orElseThrow();
            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);

            if (attacker == null || target == null) continue;

            Strike strike = Strike.of(rules, state, evaluation, start, attack.attacker(), attacker, targetRef.side(), target);
            blows.add(new Blow(attack, attacker, target, strike));
        }

        List<Kill> kills = new ArrayList<>();
        List<Survivor> survivors = new ArrayList<>();
        for (Blow blow : blows) {
            boolean mutualKill = blow.strike().killed() && blows.stream().anyMatch(other ->
                    other != blow && other.strike().killed()
                            && other.attacker() == blow.target() && other.target() == blow.attacker());
            Strike strike = mutualKill ? blow.strike().withoutSpill() : blow.strike();
            Target targetRef = blow.attack().target().orElseThrow();

            BoundExpansion rescue = strike.killed() ? rescue(state, evaluation, targetRef.side(), blow.target()) : null;
            if (rescue != null) {
                strike = strike.saved(blow.target().card().def());
            } else {
                blow.target().takeDamage(strike.damage());
            }
            if (strike.spill() > 0) {
                state.player(blow.attack().attacker().other()).loseLife(strike.spill());
            }
            log.add(strike.event(blow.attack(), 1, mutualKill));
            if (rescue != null) {
                log.add(new CreatureSaved(targetRef.side(), targetRef.lane(), blow.target().card().id(), rescue.abilityId()));
            } else if (strike.killed()) {
                log.add(new CreatureDestroyed(targetRef.side(), targetRef.lane(), blow.target().card().id()));
                kills.add(new Kill(blow.attack().attacker(), blow.attacker()));
                blow.attacker().endUntilKill();
            } else if (strike.damage() > 0 && !blow.target().destroyed()) {
                survivors.add(new Survivor(targetRef.side(), blow.target()));
            }
        }
        triggerKills(state, kills, log);
        triggerSurvivors(state, survivors, log);

        // 9.4: a redirected attack of Cycle 1 resolves after all the simultaneous ones, one at a time, in attack order.
        for (Attack attack : attacks) {
            if (attack.inCycle1() && attack.isRedirected()) {
                sequentialAttack(state, evaluation, start, attack, 1, log);
            }
        }
    }

    /**
     * Cycle 2 (9.3): the other attacks, one at a time in attacker lane order. After an attack that kills, the kill
     * triggers and the stack resolves before the next attack. Appends events to {@code log}.
     */
    public void cycle2(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        CycleStart start = CycleStart.of(state, evaluation);
        for (Attack attack : attacks) {
            if (!attack.inCycle1()) {
                sequentialAttack(state, evaluation, start, attack, 2, log);
            }
        }
    }

    /**
     * One attack resolved on its own with immediate effect: the attacks of Cycle 2 (9.3) and the redirected ones of
     * Cycle 1 (9.4). An attacker already destroyed does not attack, nor does one whose target is gone, so a redirect
     * to a creature destroyed meanwhile does nothing. After a kill the stack resolves before the next attack.
     */
    private void sequentialAttack(MatchState state, Evaluation evaluation, CycleStart start, Attack attack, int cycle,
            List<MatchEvent> log) {
        Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
        if (attacker == null || attacker.destroyed()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.ATTACKER_DESTROYED));
            return;
        }
        if (attack.target().isEmpty()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.NO_TARGET));
            return;
        }
        Target targetRef = attack.target().orElseThrow();
        Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
        if (target == null || target.destroyed()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.TARGET_DESTROYED));
            return;
        }

        Strike strike = Strike.of(rules, state, evaluation, start, attack.attacker(), attacker, targetRef.side(), target);

        BoundExpansion rescue = strike.killed() ? rescue(state, evaluation, targetRef.side(), target) : null;
        if (rescue != null) {
            strike = strike.saved(target.card().def());
        } else {
            target.takeDamage(strike.damage());
        }
        if (strike.spill() > 0) {
            state.player(attack.attacker().other()).loseLife(strike.spill());
        }
        log.add(strike.event(attack, cycle, false));
        if (rescue != null) {
            log.add(new CreatureSaved(targetRef.side(), targetRef.lane(), target.card().id(), rescue.abilityId()));
        } else if (strike.killed()) {
            log.add(new CreatureDestroyed(targetRef.side(), targetRef.lane(), target.card().id()));
            attacker.endUntilKill();
            triggerKills(state, List.of(new Kill(attack.attacker(), attacker)), log);
        } else if (strike.damage() > 0 && !target.destroyed()) {
            triggerSurvivors(state, List.of(new Survivor(targetRef.side(), target)), log);
        }
    }

    /**
     * T20 "about to be destroyed", effect E12: if {@code target} has a living effect that saves it (its conditions
     * hold, for Vestigio 5 the lockout is not set) and it was not already saved, it is saved: DEF back to the base
     * DEF, the lockout is set, and the blow it would have died of deals no spill. Returns that effect, or null.
     */
    private static BoundExpansion rescue(MatchState state, Evaluation evaluation, Side side, Creature target) {
        if (target.condemned()) {
            return null;
        }
        for (BoundExpansion e : target.abilities("T20")) {
            if (e.effectId().equals("E12") && StaticEffects.holds(e, state, evaluation, side, target)) {
                state.player(side).lock(e.stringParam("lockout.key").orElseThrow(),
                        Suit.valueOf(e.stringParam("lockout.suit").orElseThrow().toUpperCase(java.util.Locale.ROOT)),
                        e.intParam("lockout.reactivate_below"));
                target.saveWithFullDef();
                return e;
            }
        }
        return null;
    }

    /** A creature that destroyed another with an attack. */
    private record Kill(Side side, Creature killer) {}

    /**
     * T19 "when it kills a creature": pushes the killers' triggered abilities, then resolves the stack (decision
     * {@code 2026-11-14-stack-resolves-when-quiet}). Pushed in ruleset 4.3 order: Potenze Artificiali first, each
     * side in lane order, a creature's abilities in card order, so the Resistenza Umana resolves first. A killer
     * destroyed in the same exchange still triggers; its effect fizzles.
     */
    private static void triggerKills(MatchState state, List<Kill> kills, List<MatchEvent> log) {
        EffectStack stack = new EffectStack(log);
        for (Side side : prioritySides(state)) {
            kills.stream()
                    .filter(kill -> kill.side() == side)
                    .sorted(Comparator.comparingInt(kill -> kill.killer().lane()))
                    .forEach(kill -> {
                        for (BoundExpansion ability : kill.killer().abilities("T19")) {
                            pushTriggered(stack, state, side, kill.killer(), ability);
                        }
                    });
        }
        stack.resolveAll();
    }

    /** A creature that took combat damage and is still in play. */
    private record Survivor(Side side, Creature creature) {}

    /** Pushes a triggered ability; an Evolution carries the factor of the creature's E19 (Spira 4). */
    private static void pushTriggered(EffectStack stack, MatchState state, Side side, Creature source,
            BoundExpansion e) {
        stack.push(side, source, source, e, e.triggerId(),
                e.evolution() ? StaticEffects.evolutionScale(state, side, source) : 1000);
    }

    /**
     * T12 "survives combat damage" (Crescita): pushes the abilities of the creatures that took damage and stayed
     * in play, in the order of ruleset 4.3, then resolves the stack.
     */
    private static void triggerSurvivors(MatchState state, List<Survivor> survivors, List<MatchEvent> log) {
        EffectStack stack = new EffectStack(log);
        for (Side side : prioritySides(state)) {
            survivors.stream()
                    .filter(s -> s.side() == side)
                    .sorted(Comparator.comparingInt(s -> s.creature().lane()))
                    .forEach(s -> {
                        for (BoundExpansion ability : s.creature().abilities("T12")) {
                            pushTriggered(stack, state, side, s.creature(), ability);
                        }
                    });
        }
        stack.resolveAll();
    }

    /**
     * T9 "declared as the target of an attack", after Declare: the targets' abilities, in attack order (Trincea), then
     * the stack resolves; then the redirects (E24) take effect. Returns the attacks as they stand, in the same order:
     * a redirected attack keeps its place and its cycle (ruleset 9.4).
     */
    public List<Attack> declareTriggers(MatchState state, List<Attack> attacks, List<MatchEvent> log) {
        EffectStack stack = new EffectStack(log);
        for (Attack attack : attacks) {
            Creature creature = declaredTarget(state, attack);
            if (creature == null) {
                continue;
            }
            for (BoundExpansion ability : creature.abilities("T9")) {
                if (!ability.effectId().equals("E24")) {
                    pushTriggered(stack, state, attack.target().orElseThrow().side(), creature, ability);
                }
            }
        }
        stack.resolveAll();

        List<Attack> out = new ArrayList<>();
        for (Attack attack : attacks) {
            out.add(redirect(state, attack, log));
        }
        return List.copyOf(out);
    }

    private static Creature declaredTarget(MatchState state, Attack attack) {
        if (attack.target().isEmpty()) {
            return null;
        }
        Target target = attack.target().get();
        return state.player(target.side()).creatureAt(target.lane()).orElse(null);
    }

    /**
     * E24 with trigger T9: the creature an attack was declared against aims it at a creature of its own player
     * next to it (new target S7: a living creature in a lane differing by 1, the lowest lane when there are two).
     * Without one the effect fizzles. The first creature's effect wins: a redirected attack is not declared against
     * its new target, so it triggers nothing there.
     */
    private static Attack redirect(MatchState state, Attack attack, List<MatchEvent> log) {
        Creature creature = declaredTarget(state, attack);
        if (creature == null) {
            return attack;
        }
        Side side = attack.target().get().side();
        for (BoundExpansion e : creature.abilities("T9")) {
            if (!e.effectId().equals("E24")) {
                continue;
            }
            CreatureRef source = CreatureRef.of(side, creature);
            log.add(new MatchEvent.EffectTriggered(source, e.abilityId(), e.triggerId(), e.effectId(), 1));
            Creature next = state.player(side).creatures().stream()
                    .filter(c -> !c.destroyed() && Math.abs(c.lane() - creature.lane()) == 1)
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
        return attack;
    }

    /**
     * T5 with E17 (Spira 5), after Evaluate: the Evolutions of the player's living creatures of the suit fire now, once,
     * whatever their own trigger. An Evolution that already fired does nothing. The log shows T5 as the trigger.
     */
    public void forceEvolutions(MatchState state, List<MatchEvent> log) {
        EffectStack stack = new EffectStack(log);
        for (Side side : prioritySides(state)) {
            for (BoundExpansion force : state.player(side).playerAbilities()) {
                if (!force.triggerId().equals("T5") || !force.effectId().equals("E17")) {
                    continue;
                }
                for (Creature creature : targetsOf(state, side, force, null)) {
                    for (BoundExpansion evolution : creature.abilities()) {
                        if (evolution.evolution()) {
                            stack.push(side, creature, creature, evolution, "T5",
                                    StaticEffects.evolutionScale(state, side, creature));
                        }
                    }
                }
            }
        }
        stack.resolveAll();
    }

    /**
     * The two sides in resolution order (ruleset 4.3 and 10.3): Potenze Artificiali first; with the same faction,
     * {@code Side.A} first.
     */
    static Side[] prioritySides(MatchState state) {
        boolean bFirst = state.player(Side.B).faction() == Faction.POTENZE_ARTIFICIALI
                && state.player(Side.A).faction() != Faction.POTENZE_ARTIFICIALI;
        return bFirst ? new Side[] {Side.B, Side.A} : new Side[] {Side.A, Side.B};
    }

    /** Refill (10): discard destroyed creatures, draw replacements in lane order. Appends events to {@code log}. */
    public void refill(MatchState state, List<MatchEvent> log) {
        Side[] order = prioritySides(state);

        // 10.1: the effects that trigger at the start of Refill resolve first, and do not touch destroyed creatures.
        startOfRefill(state, order, log);

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
            EquipmentCard pendingEquipment = null;
            for (int lane = 1; lane <= 5; lane++) {
                if (player.creatureAt(lane).isPresent()) {
                    continue;
                }
                if (player.deck().isEmpty()) {
                    log.add(new LaneLeftEmpty(side, lane));
                    continue;
                }
                Card card = player.deck().remove(0);
                if (card instanceof CreatureCard creatureCard) {
                    Creature drawn = new Creature(creatureCard, lane, binder.bind(creatureCard));
                    player.place(drawn);
                    log.add(new CardDrawn(side, lane, card.id()));
                    onReplacementPlaced(state, side, drawn, log);
                    if (pendingEquipment != null) {
                        cardEffects.onEquipment(pendingEquipment, side, drawn);
                        log.add(new EquipmentApplied(side, lane, pendingEquipment.id(), card.id()));
                        applyImmediate(state, side, pendingEquipment, drawn, log);
                        player.discard().add(pendingEquipment);
                        pendingEquipment = null;
                    }
                } else if (card instanceof SpellCard spell) {
                    cardEffects.onSpell(spell, side);
                    player.discard().add(spell);
                    log.add(new SpellResolved(side, lane, spell.id()));
                    applyImmediate(state, side, spell, null, log);
                    lane--;
                } else if (card instanceof EquipmentCard equipment) {
                    pendingEquipment = equipment;
                    log.add(new EquipmentPending(side, equipment.id()));
                    lane--;
                }
            }
            if (pendingEquipment != null) {
                player.discard().add(pendingEquipment);
                log.add(new EquipmentLost(side, pendingEquipment.id()));
            }
        }

        // 10.10: what depends on the creatures in play after Refill (the lockout of Vestigio 5).
        for (Side side : order) {
            state.player(side).releaseLocks();
        }
    }

    /**
     * The effects written on a spell (10.5) or an equipment (10.6), in card order, each on the creatures it aims at:
     * S9 the owner's living creatures and S10 the opponent's, in lane order; S27 the creature the equipment was applied
     * to. Destroyed creatures are not affected. Each change is logged.
     */
    private void applyImmediate(MatchState state, Side owner, Card card, Creature receiver, List<MatchEvent> log) {
        for (BoundExpansion e : binder.bindImmediate(card)) {
            Side side = e.targetId().equals("S10") ? owner.other() : owner;
            List<Creature> targets = switch (e.targetId()) {
                case "S27" -> receiver == null || receiver.destroyed() ? List.of() : List.of(receiver);
                default -> state.player(side).creatures().stream().filter(c -> !c.destroyed()).toList();
            };
            for (Creature target : targets) {
                EffectResolver.Outcome outcome = EffectResolver.resolve(e, target);
                log.add(new MatchEvent.CardEffectResolved(owner, card.id(), e.abilityId(), e.effectId(),
                        CreatureRef.of(side, target), outcome.changes()));
            }
        }
    }

    /**
     * T2 with phase Rifornimento (10.1): the abilities of the creatures in play and of the player (suit synergies),
     * pushed Potenze Artificiali first, then resolved. Destroyed creatures are neither sources nor targets. A tier
     * whose source is the player is logged with the creature it affects as source.
     */
    private void startOfRefill(MatchState state, Side[] order, List<MatchEvent> log) {
        EffectStack stack = new EffectStack(log);
        for (Side side : order) {
            var player = state.player(side);
            for (Creature source : player.creatures()) {
                if (!source.destroyed()) {
                    pushRefillStart(state, side, source, source.abilities("T2"), stack);
                }
            }
            pushRefillStart(state, side, null, player.playerAbilities().stream()
                    .filter(e -> e.triggerId().equals("T2")).toList(), stack);
        }
        stack.resolveAll();
    }

    private static void pushRefillStart(MatchState state, Side side, Creature source, List<BoundExpansion> expansions,
            EffectStack stack) {
        for (BoundExpansion e : expansions) {
            if (!"Rifornimento".equals(((Value.StrV) e.triggerParams().get("phase")).v())) {
                continue;
            }
            for (Creature target : targetsOf(state, side, e, source)) {
                if (StaticEffects.holds(e, state, null, side, target)) {
                    stack.push(side, source != null ? source : target, target, e);
                }
            }
        }
    }

    /** The living creatures an effect of the player or of {@code source} (null for the player) is aimed at. */
    private static List<Creature> targetsOf(MatchState state, Side side, BoundExpansion e, Creature source) {
        List<Creature> out = new ArrayList<>();
        switch (e.targetId()) {
            case "S1" -> {
                if (source != null && !source.destroyed()) {
                    out.add(source);
                }
            }
            case "S12" -> {
                Suit suit = Suit.valueOf(((Value.StrV) e.targetParams().get("suit")).v().toUpperCase(java.util.Locale.ROOT));
                for (Creature c : state.player(side).creatures()) {
                    if (!c.destroyed() && c.card().suit() == suit) {
                        out.add(c);
                    }
                }
            }
            default -> { }
        }
        return out;
    }

    /**
     * T29 (10.4): a creature was placed as a replacement. The abilities of the player (suit synergies) and of the
     * creatures in play that trigger on it are pushed and resolved at once; their target S27 is the replacement.
     */
    private void onReplacementPlaced(MatchState state, Side side, Creature replacement, List<MatchEvent> log) {
        EffectStack stack = new EffectStack(log);
        var player = state.player(side);
        for (BoundExpansion e : player.playerAbilities()) {
            if (e.triggerId().equals("T29") && StaticEffects.holds(e, state, null, side, replacement)) {
                stack.push(side, replacement, replacement, e);
            }
        }
        for (Creature source : player.creatures()) {
            for (BoundExpansion e : source.abilities("T29")) {
                if (source != replacement && !source.destroyed() && StaticEffects.holds(e, state, null, side, replacement)) {
                    stack.push(side, source, e.targetId().equals("S27") ? replacement : source, e);
                }
            }
        }
        stack.resolveAll();
    }

    /** End of turn (11): the result if the match is over, empty if it goes on. Appends events to {@code log}. */
    public Optional<MatchResult> endOfTurn(MatchState state, Evaluation evaluation, List<MatchEvent> log) {
        PlayerState a = state.player(Side.A);
        PlayerState b = state.player(Side.B);

        boolean aNoCreatures = a.creatures().stream().allMatch(Creature::destroyed);
        boolean bNoCreatures = b.creatures().stream().allMatch(Creature::destroyed);
        boolean aOut = aNoCreatures || a.life() <= 0;
        boolean bOut = bNoCreatures || b.life() <= 0;

        MatchResult result;

        if (aOut && bOut) {
            result = new MatchResult(Outcome.DRAW, EndReason.BOTH_LOST);

        } else if (aOut) {
            EndReason reason = aNoCreatures ? EndReason.NO_CREATURES : EndReason.LIFE_ZERO;
            result = new MatchResult(Outcome.B_WIN, reason);

        } else if (bOut) {
            EndReason reason = bNoCreatures ? EndReason.NO_CREATURES : EndReason.LIFE_ZERO;
            result = new MatchResult(Outcome.A_WIN, reason);

        } else if (state.turn() != MatchState.LAST_TURN) {
            return Optional.empty();

        } else if (a.life() != b.life()) {
            Outcome winner = a.life() > b.life() ? Outcome.A_WIN : Outcome.B_WIN;
            result = new MatchResult(winner, EndReason.TURN_50_LIFE);

        } else {
            int handComparison = handComparator.compare(evaluation.a(), evaluation.b());
            if (handComparison == 0) {
                result = new MatchResult(Outcome.DRAW, EndReason.TURN_50_DRAW);
            } else {
                Outcome winner = handComparison > 0 ? Outcome.A_WIN : Outcome.B_WIN;
                result = new MatchResult(winner, EndReason.TURN_50_HAND);
            }
        }

        log.add(new MatchEnded(state.turn(), result));
        return Optional.of(result);
    }
}
