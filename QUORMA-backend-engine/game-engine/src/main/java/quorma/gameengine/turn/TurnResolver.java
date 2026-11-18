package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
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
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.TargetIds;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandEvaluator;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.BoardSnapshot;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.CreatureSaved;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.HandEvaluated;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.MatchEvent.MatchEnded;
import quorma.gameengine.log.MatchEvent.RefillPrevented;
import quorma.gameengine.log.MatchEvent.TargetDeclared;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.SkipReason;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Aura;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Target;

/**
 * Plays one turn: Evaluate, Declare, Combat (Cycle 1, Cycle 2), Refill, End of turn (ruleset 6 to 11).
 *
 * <p>The hand evaluator and comparator are parameters so that the turn code does not wait for
 * {@link HandEvaluator}: tests pass fixed results. Every phase is public so that tests can run it alone.
 *
 * <p>Each phase starts with the T2 effects of its name ({@code Valutazione}, {@code Dichiarazione},
 * {@code Inizio del combattimento}, {@code Ciclo 1}, {@code Ciclo 2}, {@code Fine del combattimento},
 * {@code Rifornimento}, {@code Fine del turno}). Durations end at fixed moments: end of Combat, start of Refill,
 * End of turn ({@link Lasting}).</p>
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

    private Game game(MatchState state, Evaluation evaluation) {
        return new Game(rules, binder, state, evaluation, cardEffects);
    }

    /** Plays the turn of {@code state}: every phase in order. Moves {@code state} to the next turn if the match goes on. */
    public TurnReport playTurn(MatchState state) {
        int turn = state.turn();
        Game g = game(state, null);
        List<MatchEvent> opening = new ArrayList<>();
        phase(g, "Valutazione", opening);
        Evaluation evaluation = evaluate(state);
        g.evaluation = evaluation;
        List<MatchEvent> afterHands = opening;
        opening = new ArrayList<>();
        opening.add(new HandEvaluated(turn, Side.A, evaluation.a(), evaluation.multiplierA()));
        opening.add(new HandEvaluated(turn, Side.B, evaluation.b(), evaluation.multiplierB()));
        opening.addAll(afterHands);
        forceEvolutions(g, opening);
        phase(g, "Dichiarazione", opening);

        List<Attack> attacks = declare(state, evaluation);
        for (Attack attack : attacks) {
            opening.add(new TargetDeclared(attack));
        }
        attacks = declareTriggers(g, attacks, opening);
        List<MatchEvent> cycle1 = new ArrayList<>();
        cycle1(g, attacks, cycle1);
        List<MatchEvent> cycle2 = new ArrayList<>();
        cycle2(g, attacks, cycle2);
        List<MatchEvent> refill = new ArrayList<>();
        refill(g, refill);
        List<MatchEvent> closing = new ArrayList<>();
        Optional<MatchResult> result = endOfTurn(g, closing);
        BoardSnapshot board = BoardSnapshot.of(state);
        if (result.isEmpty() && state.turn() < MatchState.LAST_TURN) {
            state.nextTurn();
        }
        return new TurnReport(turn, evaluation, attacks, opening, cycle1, cycle2, refill, closing, board, result);
    }

    /** T2 effects at the start of {@code phase}. */
    private static void phase(Game g, String phase, List<MatchEvent> log) {
        g.fireAndResolve(List.of(new GameEvent.PhaseStart(phase)), log);
    }

    /**
     * Evaluate (7): each player's hand from their living creatures in lane order, with the effects that bend it
     * ({@link Hands}), and the multiplier; the suit synergy tiers are bound first.
     */
    public Evaluation evaluate(MatchState state) {
        Synergies synergies = SynergyTiers.evaluate(rules, state);
        SynergyTiers.bind(binder, rules, state, synergies);
        Board b = new Board(binder, state, null, 0);
        Set<Creature> excluded = Hands.excluded(b);
        HandResult handA = Hands.best(b, Side.A, excluded, handEvaluator, handComparator);
        HandResult handB = Hands.best(b, Side.B, excluded, handEvaluator, handComparator);

        int multiplierA = handA.type() == HandType.NONE ? 0 : rules.handMultiplierPermille(handA.type().name());
        int multiplierB = handB.type() == HandType.NONE ? 0 : rules.handMultiplierPermille(handB.type().name());
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

    // ================================================================ Declare: triggers and redirects

    /**
     * After Declare, in attack order: T9 of the declared target, T8 of the creatures that watch declarations, T35 of
     * the target's adjacent allies (the attacker is the creature that triggered them); then the stack resolves; then
     * the redirects (E24) take effect. Returns the attacks as they stand, in the same order: a redirected attack keeps
     * its place and its cycle (ruleset 9.4).
     */
    public List<Attack> declareTriggers(MatchState state, List<Attack> attacks, List<MatchEvent> log) {
        return declareTriggers(game(state, null), attacks, log);
    }

    private List<Attack> declareTriggers(Game g, List<Attack> attacks, List<MatchEvent> log) {
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
                    push(g, b, stack, targetSide, target, e, ctx);
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
                            push(g, b, stack, side, watcher, e, ctx);
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
                        push(g, b, stack, targetSide, ally, e, ctx);
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

    private static void push(Game g, Board b, EffectStack stack, Side side, Creature source, BoundExpansion e,
            TriggerContext ctx) {
        stack.push(EffectStack.Item.triggered(side, source, e, g.triggers.scaleOf(b, side, source, e), ctx));
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

    // ================================================================ Evaluate: T5

    /**
     * After Evaluate: the T5 effects of living creatures whose player reached the suit level, then T5 with E17
     * (Spira 5): the Evolutions of the player's living creatures of the suit fire now, once, whatever their own
     * trigger. An Evolution that already fired does nothing. The log shows T5 as the trigger.
     */
    public void forceEvolutions(MatchState state, List<MatchEvent> log) {
        forceEvolutions(game(state, null), log);
    }

    private void forceEvolutions(Game g, List<MatchEvent> log) {
        MatchState state = g.state;
        EffectStack stack = g.stack(log);
        Board b = g.board();
        for (Side side : prioritySides(state)) {
            for (Creature creature : state.player(side).creatures()) {
                if (creature.destroyed()) {
                    continue;
                }
                for (BoundExpansion e : b.abilities(creature, "T5")) {
                    Suit suit = Suit.valueOf(((Value.StrV) e.triggerParams().get("suit")).v().toUpperCase(java.util.Locale.ROOT));
                    long level = ((Value.IntV) e.triggerParams().get("level")).v();
                    if (!e.effectId().equals("E17") && state.player(side).suitCount(suit) >= level
                            && b.holds(e, side, creature, Board.Ctx.NONE)) {
                        push(g, b, stack, side, creature, e, TriggerContext.NONE);
                    }
                }
            }
            for (BoundExpansion force : state.player(side).playerAbilities()) {
                if (!force.triggerId().equals("T5") || !force.effectId().equals("E17")) {
                    continue;
                }
                Suit suit = Suit.valueOf(((Value.StrV) force.targetParams().get("suit")).v().toUpperCase(java.util.Locale.ROOT));
                for (Creature creature : state.player(side).creatures()) {
                    if (creature.destroyed() || creature.card().suit() != suit) {
                        continue;
                    }
                    for (BoundExpansion evolution : b.abilities(creature)) {
                        if (evolution.evolution()) {
                            stack.push(EffectStack.Item.triggered(side, creature, evolution, "T5",
                                    g.triggers.scaleOf(b, side, creature, evolution), TriggerContext.NONE));
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

    // ================================================================ Combat

    /**
     * Cycle 1 (9.2, 9.5 to 9.7): attacks on the opposite lane, simultaneous. Before the blows, each attack's
     * replacements (T39, T22) and its T6/T7 triggers. Once every blow has landed, the destructions trigger, then the
     * damage, then the survivors, each time the stack resolving. Appends events to {@code log}.
     */
    public void cycle1(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        cycle1(game(state, evaluation), attacks, log);
    }

    private void cycle1(Game g, List<Attack> attacks, List<MatchEvent> log) {
        record Blow(Attack attack, Creature attacker, Creature target, Strike strike) {}
        MatchState state = g.state;
        g.cycle = 1;
        phase(g, "Inizio del combattimento", log);
        phase(g, "Ciclo 1", log);

        List<Attack> landing = new ArrayList<>();
        for (Attack attack : attacks) {
            if (!attack.inCycle1() || attack.isRedirected()) continue;
            Target targetRef = attack.target().orElseThrow();
            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
            if (attacker == null || target == null) continue;
            if (attacker.destroyed()) {
                log.add(new AttackSkipped(attack, 1, SkipReason.ATTACKER_DESTROYED));
                continue;
            }
            if (target.destroyed()) {
                log.add(new AttackSkipped(attack, 1, SkipReason.TARGET_DESTROYED));
                continue;
            }
            if (beforeAttack(g, attack, attacker, target, 1, log)) {
                landing.add(attack);
            }
        }

        // Every blow is worked out from the state at the start of the cycle, then they all land.
        Board b = g.board();
        CycleStart start = CycleStart.of(b);
        List<Blow> blows = new ArrayList<>();
        for (Attack attack : landing) {
            Target targetRef = attack.target().orElseThrow();
            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
            if (attacker == null || target == null || attacker.destroyed() || target.destroyed()) continue;
            Strike strike = Strike.of(g, b, start, attack.attacker(), attacker, targetRef.side(), target, true);
            blows.add(new Blow(attack, attacker, target, strike));
        }

        Windows windows = new Windows();
        for (Blow blow : blows) {
            boolean mutualKill = blow.strike().killed() && blows.stream().anyMatch(other ->
                    other != blow && other.strike().killed()
                            && other.attacker() == blow.target() && other.target() == blow.attacker());
            Strike strike = mutualKill ? blow.strike().withoutSpill() : blow.strike();
            land(g, blow.attack(), blow.attacker(), blow.target(), strike, 1, mutualKill, log, windows);
        }
        windows.fire(g, log);

        // 9.4: a redirected attack of Cycle 1 resolves after all the simultaneous ones, one at a time, in attack order.
        for (Attack attack : attacks) {
            if (attack.inCycle1() && attack.isRedirected()) {
                sequentialAttack(g, start, attack, 1, log);
            }
        }
    }

    /**
     * Cycle 2 (9.3): the other attacks, one at a time in attacker lane order. After each attack its triggers resolve
     * before the next. Then the end of Combat: its T2 effects, and the end of what lasts until it. Appends events to
     * {@code log}.
     */
    public void cycle2(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        cycle2(game(state, evaluation), attacks, log);
    }

    private void cycle2(Game g, List<Attack> attacks, List<MatchEvent> log) {
        g.cycle = 2;
        phase(g, "Ciclo 2", log);
        CycleStart start = CycleStart.of(g.board());
        for (Attack attack : attacks) {
            if (!attack.inCycle1()) {
                sequentialAttack(g, start, attack, 2, log);
            }
        }
        g.cycle = 0;
        phase(g, "Fine del combattimento", log);
        g.expire(Lasting.END_OF_COMBAT);
    }

    /**
     * One attack resolved on its own with immediate effect: the attacks of Cycle 2 (9.3) and the redirected ones of
     * Cycle 1 (9.4). An attacker already destroyed does not attack, nor does one whose target is gone, so a redirect
     * to a creature destroyed meanwhile does nothing. After the blow its triggers resolve before the next attack.
     */
    private void sequentialAttack(Game g, CycleStart start, Attack attack, int cycle, List<MatchEvent> log) {
        MatchState state = g.state;
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
        if (!beforeAttack(g, attack, attacker, target, cycle, log)) {
            return;
        }
        if (attacker.destroyed() || attacker.leftPlay()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.ATTACKER_DESTROYED));
            return;
        }
        if (target.destroyed() || target.leftPlay()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.TARGET_DESTROYED));
            return;
        }
        Board b = g.board();
        Strike strike = Strike.of(g, b, start, attack.attacker(), attacker, targetRef.side(), target, true);
        Windows windows = new Windows();
        land(g, attack, attacker, target, strike, cycle, false, log, windows);
        windows.fire(g, log);
    }

    /**
     * Before an attack is made: T39 of the target and T22 of the attacker (replacements: E25 stops the attack, E23
     * stops the target from damaging the attacker for the rest of the Combat), then T6 of the attacker and T7 of the
     * target, resolved. Returns false if the attack does not happen (logged).
     */
    private boolean beforeAttack(Game g, Attack attack, Creature attacker, Creature target, int cycle,
            List<MatchEvent> log) {
        Side attackerSide = attack.attacker();
        Side targetSide = attack.target().orElseThrow().side();
        Board b = g.board();
        for (Board.Active a : b.replacements("T39", target)) {
            if (a.e().effectId().equals("E25") && attackerSide != targetSide) {
                g.destruction.markReplacement(a, target);
                log.add(new AttackSkipped(attack, cycle, SkipReason.PREVENTED));
                return false;
            }
        }
        Boolean wouldKill = null;
        TriggerContext ctx = TriggerContext.attacking(targetSide, target, attackerSide, attacker);
        for (BoundExpansion e : b.abilities(attacker, "T22")) {
            if (!attacker.canTrigger(e, g.turn())) {
                continue;
            }
            if (wouldKill == null && e.conditions().stream().anyMatch(c -> c.id().equals("C21"))) {
                wouldKill = Strike.of(g, b, CycleStart.of(b), attackerSide, attacker, targetSide, target, false).killed();
            }
            if (!b.holds(e, attackerSide, attacker, new Board.Ctx(target, null, ctx, wouldKill))) {
                continue;
            }
            switch (e.effectId()) {
                case "E25" -> {
                    attacker.markTriggered(e, g.turn());
                    log.add(new AttackSkipped(attack, cycle, SkipReason.PREVENTED));
                    return false;
                }
                case "E23" -> {
                    attacker.markTriggered(e, g.turn());
                    target.preventDamageTo(attacker, e.abilityId(), g.lasting(e.duration(), attacker));
                }
                default -> { }
            }
        }
        g.fireAndResolve(List.of(new GameEvent.Attacks(attackerSide, attacker, targetSide, target)), log);
        return true;
    }

    /** What a blow sets off, fired once the blows of a cycle (or one sequential attack) have landed. */
    private static final class Windows {
        final List<GameEvent> destroyed = new ArrayList<>();
        final List<GameEvent> damaged = new ArrayList<>();
        final List<GameEvent> survived = new ArrayList<>();

        /** Destructions (T10, T19, T36, T37), then damage (T13, T38, T11), then survivors (T12). */
        void fire(Game g, List<MatchEvent> log) {
            g.fireAndResolve(destroyed, log);
            g.fireAndResolve(damaged, log);
            g.fireAndResolve(survived, log);
        }
    }

    /** A blow lands: rescue (T20), damage, spill, the log, and the triggers it sets off. */
    private void land(Game g, Attack attack, Creature attacker, Creature target, Strike strike, int cycle,
            boolean mutualKill, List<MatchEvent> log, Windows windows) {
        Side attackerSide = attack.attacker();
        Target targetRef = attack.target().orElseThrow();
        Board.Active rescue = strike.killed() ? g.destruction.rescue(targetRef.side(), target) : null;
        if (rescue != null) {
            boolean keepSpill = rescue.e().effectId().equals("E11") && rescue.e().boolParam("spill_on_prevent", false);
            if (rescue.e().effectId().equals("E11")) {
                strike = strike.saved(target.currentDef(), keepSpill);
            } else {
                strike = strike.saved(target.currentDef(), false);
            }
        } else {
            target.takeDamage(strike.damage());
            if (strike.lethal()) {
                target.markDestroyed();
            }
        }
        if (strike.spill() > 0) {
            g.state.player(attackerSide.other()).loseLife(strike.spill());
        }
        log.add(strike.event(attack, cycle, mutualKill));
        if (strike.damage() > 0) {
            windows.damaged.add(new GameEvent.Damaged(targetRef.side(), target, strike.damage(), attackerSide, attacker, true));
            windows.damaged.add(new GameEvent.Dealt(attackerSide, attacker, targetRef.side(), target, strike.damage(),
                    strike.killed()));
        }
        if (strike.spill() > 0) {
            windows.damaged.add(new GameEvent.Spilled(attackerSide, attacker));
        }
        if (rescue != null) {
            log.add(new CreatureSaved(targetRef.side(), targetRef.lane(), target.card().id(), rescue.e().abilityId()));
        } else if (strike.killed()) {
            log.add(new CreatureDestroyed(targetRef.side(), targetRef.lane(), target.card().id()));
            windows.destroyed.add(new GameEvent.Destroyed(targetRef.side(), target, attackerSide, attacker));
            attacker.endUntilKill();
        } else if (strike.damage() > 0 && !target.destroyed()) {
            windows.survived.add(new GameEvent.Survived(targetRef.side(), target));
        }
    }

    // ================================================================ Refill

    /** Refill (10): discard destroyed creatures, draw replacements in lane order. Appends events to {@code log}. */
    public void refill(MatchState state, List<MatchEvent> log) {
        refill(game(state, null), log);
    }

    private void refill(Game g, List<MatchEvent> log) {
        MatchState state = g.state;
        Side[] order = prioritySides(state);

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
        for (Side owner : prioritySides(state)) {
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
    private void startOfRefill(Game g, Side[] order, List<MatchEvent> log) {
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
                    push(g, b, stack, side, source, e, TriggerContext.NONE);
                }
                continue;
            }
            for (Creature target : targetsOf(g.state, side, e, source)) {
                if (b.holds(e, side, target, Board.Ctx.NONE)) {
                    stack.push(new EffectStack.Item(side, source != null ? source : target, target, e, e.triggerId(),
                            source != null ? g.triggers.scaleOf(b, side, source, e) : 1000, TriggerContext.NONE));
                }
            }
        }
    }

    /** The living creatures an effect of the player or of {@code source} (null for the player) is aimed at (S1, S12). */
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

    // ================================================================ End of turn

    /**
     * End of turn (11): its T2 effects, then the result if the match is over, empty if it goes on. When it goes on,
     * the T26 effects ("at the end of the turn", after the defeat checks), then the end of what lasts until the end
     * of the turn. Appends events to {@code log}.
     */
    public Optional<MatchResult> endOfTurn(MatchState state, Evaluation evaluation, List<MatchEvent> log) {
        return endOfTurn(game(state, evaluation), log);
    }

    private Optional<MatchResult> endOfTurn(Game g, List<MatchEvent> log) {
        MatchState state = g.state;
        Evaluation evaluation = g.evaluation;
        phase(g, "Fine del turno", log);
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
            g.fireAndResolve(List.of(new GameEvent.EndOfTurn()), log);
            g.expire(Lasting.END_OF_TURN);
            for (Side side : Side.values()) {
                state.player(side).creatures().forEach(Creature::newTurn);
            }
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
