package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandEvaluator;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
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
        List<MatchEvent> log = new ArrayList<>();
        Evaluation evaluation = evaluate(state);
        log.add(new HandEvaluated(turn, Side.A, evaluation.a(), evaluation.multiplierA()));
        log.add(new HandEvaluated(turn, Side.B, evaluation.b(), evaluation.multiplierB()));

        List<Attack> attacks = declare(state, evaluation);
        for (Attack attack : attacks) {
            log.add(new TargetDeclared(attack));
        }
        cycle1(state, evaluation, attacks, log);
        cycle2(state, evaluation, attacks, log);
        refill(state, log);
        Optional<MatchResult> result = endOfTurn(state, evaluation, log);
        if (result.isEmpty() && state.turn() < MatchState.LAST_TURN) {
            state.nextTurn();
        }
        return new TurnReport(turn, evaluation, attacks, log, result);
    }

    /** Evaluate (7): each player's hand from their creatures in lane order, and the multiplier. */
    public Evaluation evaluate(MatchState state) {
        List<HandCard> cardsA = new ArrayList<>();
        List<HandCard> cardsB = new ArrayList<>();

        for (Creature creature : state.player(Side.A).creatures()) {
            cardsA.add(new HandCard(creature.lane(), creature.card().rank(), creature.card().suit()));
        }
        for (Creature creature : state.player(Side.B).creatures()) {
            cardsB.add(new HandCard(creature.lane(), creature.card().rank(), creature.card().suit()));
        }

        HandResult handA = handEvaluator.apply(cardsA);
        HandResult handB = handEvaluator.apply(cardsB);

        int multiplierA = 0;
        if (handA.type() != HandType.NONE) {
            multiplierA = rules.handMultiplierPermille(handA.type().name());
        }

        int multiplierB = 0;
        if (handB.type() != HandType.NONE) {
            multiplierB = rules.handMultiplierPermille(handB.type().name());
        }
        return new Evaluation(handA, handB, multiplierA, multiplierB);
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
        List<Blow> blows = new ArrayList<>();
        for (Attack attack : attacks) {
            if (!attack.inCycle1()) continue;

            Target targetRef = attack.target().orElseThrow();
            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);

            if (attacker == null || target == null) continue;

            Strike strike = Strike.of(rules, attack.attacker(), attacker, targetRef.side(), target,
                    evaluation.multiplier(attack.attacker()));
            blows.add(new Blow(attack, attacker, target, strike));
        }

        List<Kill> kills = new ArrayList<>();
        for (Blow blow : blows) {
            boolean mutualKill = blow.strike().killed() && blows.stream().anyMatch(other ->
                    other != blow && other.strike().killed()
                            && other.attacker() == blow.target() && other.target() == blow.attacker());
            Strike strike = mutualKill ? blow.strike().withoutSpill() : blow.strike();

            blow.target().takeDamage(strike.damage());
            if (strike.spill() > 0) {
                state.player(blow.attack().attacker().other()).loseLife(strike.spill());
            }
            log.add(strike.event(blow.attack(), 1, mutualKill));
            if (strike.killed()) {
                Target targetRef = blow.attack().target().orElseThrow();
                log.add(new CreatureDestroyed(targetRef.side(), targetRef.lane(), blow.target().card().id()));
                kills.add(new Kill(blow.attack().attacker(), blow.attacker()));
            }
        }
        triggerKills(state, kills, log);
    }

    /**
     * Cycle 2 (9.3): the other attacks, one at a time in attacker lane order. After an attack that kills, the kill
     * triggers and the stack resolves before the next attack. Appends events to {@code log}.
     */
    public void cycle2(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        for (Attack attack : attacks) {
            if (attack.inCycle1()) continue;
            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            if (attacker == null || attacker.destroyed()) {
                log.add(new AttackSkipped(attack, 2, SkipReason.ATTACKER_DESTROYED));
                continue;
            }
            if (attack.target().isEmpty()) {
                log.add(new AttackSkipped(attack, 2, SkipReason.NO_TARGET));
                continue;
            }
            Target targetRef = attack.target().orElseThrow();
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
            if (target == null || target.destroyed()) {
                log.add(new AttackSkipped(attack, 2, SkipReason.TARGET_DESTROYED));
                continue;
            }

            Strike strike = Strike.of(rules, attack.attacker(), attacker, targetRef.side(), target,
                    evaluation.multiplier(attack.attacker()));

            target.takeDamage(strike.damage());
            if (strike.spill() > 0) {
                state.player(attack.attacker().other()).loseLife(strike.spill());
            }
            log.add(strike.event(attack, 2, false));
            if (strike.killed()) {
                log.add(new CreatureDestroyed(targetRef.side(), targetRef.lane(), target.card().id()));
                triggerKills(state, List.of(new Kill(attack.attacker(), attacker)), log);
            }
        }
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
                            stack.push(side, kill.killer(), ability);
                        }
                    });
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

        for (Side side : order) {
            var player = state.player(side);
            for (int lane = 1; lane <= 5; lane++) {
                Creature creature = player.creatureAt(lane).orElse(null);
                if (creature != null && creature.destroyed()) {
                    player.remove(lane);
                    player.discard().add(creature.card());
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
                    if (pendingEquipment != null) {
                        cardEffects.onEquipment(pendingEquipment, side, drawn);
                        log.add(new EquipmentApplied(side, lane, pendingEquipment.id(), card.id()));
                        player.discard().add(pendingEquipment);
                        pendingEquipment = null;
                    }
                } else if (card instanceof SpellCard spell) {
                    cardEffects.onSpell(spell, side);
                    player.discard().add(spell);
                    log.add(new SpellResolved(side, lane, spell.id()));
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
