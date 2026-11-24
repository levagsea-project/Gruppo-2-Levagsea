package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.ForceEvolutionsEffect;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandEvaluator;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.BoardSnapshot;
import quorma.gameengine.log.MatchEvent.HandEvaluated;
import quorma.gameengine.log.MatchEvent.TargetDeclared;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

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

    /** Telemetry of the turn being played: the tiers that decided a blow (see {@link Game#decisive}). */
    private final Map<Side, Set<String>> decisive = new EnumMap<>(Side.class);

    private Game game(MatchState state, Evaluation evaluation) {
        return new Game(rules, binder, state, evaluation, cardEffects, decisive);
    }

    /** Plays the turn of {@code state}: every phase in order. Moves {@code state} to the next turn if the match goes on. */
    public TurnReport playTurn(MatchState state) {
        int turn = state.turn();
        decisive.clear();
        Game g = game(state, null);
        List<MatchEvent> opening = new ArrayList<>();
        g.phase("Valutazione", opening);
        Evaluation evaluation = evaluate(state);
        g.evaluation = evaluation;
        List<MatchEvent> afterHands = opening;
        opening = new ArrayList<>();
        opening.add(new HandEvaluated(turn, Side.A, evaluation.a(), evaluation.multiplierA()));
        opening.add(new HandEvaluated(turn, Side.B, evaluation.b(), evaluation.multiplierB()));
        opening.addAll(afterHands);
        forceEvolutions(g, opening);
        g.phase("Dichiarazione", opening);

        List<Attack> attacks = declare(state, evaluation);
        for (Attack attack : attacks) {
            opening.add(new TargetDeclared(attack));
        }
        attacks = Declaration.declareTriggers(g, attacks, opening);
        List<MatchEvent> cycle1 = new ArrayList<>();
        Combat.cycle1(g, attacks, cycle1);
        List<MatchEvent> cycle2 = new ArrayList<>();
        Combat.cycle2(g, attacks, cycle2);
        List<MatchEvent> refill = new ArrayList<>();
        Refill.refill(g, refill);
        List<MatchEvent> closing = new ArrayList<>();
        Optional<MatchResult> result = TurnEnd.endOfTurn(g, handComparator, closing);
        BoardSnapshot board = BoardSnapshot.of(state);
        if (result.isEmpty() && state.turn() < MatchState.LAST_TURN) {
            state.nextTurn();
        }
        Map<Side, List<String>> decided = new EnumMap<>(Side.class);
        decisive.forEach((side, ids) -> decided.put(side, List.copyOf(ids)));
        return new TurnReport(turn, evaluation, attacks, opening, cycle1, cycle2, refill, closing, board, result,
                new Synergies(decided));
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
        return Declaration.declare(state, evaluation);
    }

    // ================================================================ Declare: triggers and redirects

    /**
     * After Declare, in attack order: T9 of the declared target, T8 of the creatures that watch declarations, T35 of
     * the target's adjacent allies (the attacker is the creature that triggered them); then the stack resolves; then
     * the redirects (E24) take effect. Returns the attacks as they stand, in the same order: a redirected attack keeps
     * its place and its cycle (ruleset 9.4).
     */
    public List<Attack> declareTriggers(MatchState state, List<Attack> attacks, List<MatchEvent> log) {
        return Declaration.declareTriggers(game(state, null), attacks, log);
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
                    if (!e.effectId().equals(ForceEvolutionsEffect.ID) && state.player(side).suitCount(suit) >= level
                            && b.holds(e, side, creature, Board.Ctx.NONE)) {
                        g.triggers.push(stack, b, side, creature, e, TriggerContext.NONE);
                    }
                }
            }
            for (BoundExpansion force : state.player(side).playerAbilities()) {
                if (!force.triggerId().equals("T5") || !force.effectId().equals(ForceEvolutionsEffect.ID)) {
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
        Combat.cycle1(game(state, evaluation), attacks, log);
    }

    /**
     * Cycle 2 (9.3): the other attacks, one at a time in attacker lane order. After each attack its triggers resolve
     * before the next. Then the end of Combat: its T2 effects, and the end of what lasts until it. Appends events to
     * {@code log}.
     */
    public void cycle2(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        Combat.cycle2(game(state, evaluation), attacks, log);
    }

    // ================================================================ Refill

    /** Refill (10): discard destroyed creatures, draw replacements in lane order. Appends events to {@code log}. */
    public void refill(MatchState state, List<MatchEvent> log) {
        Refill.refill(game(state, null), log);
    }

    // ================================================================ End of turn

    /**
     * End of turn (11): its T2 effects, then the result if the match is over, empty if it goes on. When it goes on,
     * the T26 effects ("at the end of the turn", after the defeat checks), then the end of what lasts until the end
     * of the turn. Appends events to {@code log}.
     */
    public Optional<MatchResult> endOfTurn(MatchState state, Evaluation evaluation, List<MatchEvent> log) {
        return TurnEnd.endOfTurn(game(state, evaluation), handComparator, log);
    }
}
