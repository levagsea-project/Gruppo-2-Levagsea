package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandEvaluator;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.HandEvaluated;
import quorma.gameengine.log.MatchEvent.TargetDeclared;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.SkipReason;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.EndReason;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Outcome;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Target;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.MatchEvent.MatchEnded;
import quorma.gameengine.log.MatchEvent.EquipmentApplied;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.EquipmentPending;
import quorma.gameengine.log.MatchEvent.SpellResolved;

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

    public TurnResolver(
            Ruleset rules,
            Function<List<HandCard>, HandResult> handEvaluator,
            Comparator<HandResult> handComparator,
            CardEffects cardEffects) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.handEvaluator = Objects.requireNonNull(handEvaluator, "handEvaluator");
        this.handComparator = Objects.requireNonNull(handComparator, "handComparator");
        this.cardEffects = Objects.requireNonNull(cardEffects, "cardEffects");
    }

    
    /** The real evaluator and comparator, and spell and equipment hooks that do nothing. */
    public TurnResolver(Ruleset rules) {
        this(rules, HandEvaluator::evaluate, HandEvaluator::compare, CardEffects.NONE);
    }

    /** Plays the turn of {@code state}: every phase in order. Moves {@code state} to the next turn if the match goes on. */
    public TurnReport playTurn(MatchState state) {
        int turn = state.turn();
        List<MatchEvent> log = new java.util.ArrayList<>();
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
        List<Attack> attacks = new java.util.ArrayList<>();
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

    /** Cycle 1 (9.2, 9.5 to 9.7): attacks on the opposite lane, simultaneous. Appends events to {@code log}. */
    public void cycle1(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        record Blow(Attack attack, Creature attacker, Creature target, int effectiveAtk, int advantage,
                int defBefore, int defAfter, boolean killed, int spill) {}

        List<Blow> blows = new java.util.ArrayList<>();
        for (Attack attack : attacks) {
            if (!attack.inCycle1()) continue;

            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            Creature target = state.player(attack.target().orElseThrow().side())
                    .creatureAt(attack.target().orElseThrow().lane()).orElse(null);

            if (attacker == null || target == null) continue;

            String attackerSubfaction = attacker.card().subfaction().dataName();
            String targetSubfaction = target.card().subfaction().dataName();
            int advantage = rules.advantagePermille(attackerSubfaction, targetSubfaction);
            int effectiveAtk = Damage.effectiveAtk(attacker.currentAtk(), advantage);
            int defBefore = target.currentDef();
            int defAfter = defBefore - effectiveAtk;
            boolean killed = defAfter <= 0;

            int spill = killed ? Damage.spill(effectiveAtk, defBefore, evaluation.multiplier(attack.attacker())) : 0;

            blows.add(new Blow(attack, attacker, target, effectiveAtk, advantage, defBefore, defAfter, killed, spill));
        }

        for (Blow blow : blows) {
            boolean mutualKill = blow.killed() && blows.stream().anyMatch(other ->
                    other != blow && other.killed() && other.attacker() == blow.target() && other.target() == blow.attacker());
            int spill = mutualKill ? 0 : blow.spill();

            blow.target().takeDamage(blow.effectiveAtk());

            if (spill > 0) {
                state.player(blow.attack().attacker().other()).loseLife(spill);
            }
            log.add(new AttackResolved(blow.attack(), 1, blow.effectiveAtk(), blow.advantage(), blow.defBefore(),
                    blow.defAfter(), blow.killed(), spill, mutualKill));
            if (blow.killed()) {
                log.add(new CreatureDestroyed(blow.attack().target().orElseThrow().side(),
                        blow.attack().target().orElseThrow().lane(), blow.target().card().id()));
            }
        }
    }

    /** Cycle 2 (9.3): the other attacks, one at a time in attacker lane order. Appends events to {@code log}. */
    public void cycle2(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        for (Attack attack : attacks) {
            if (attack.inCycle1()) continue;
            if (attack.target().isEmpty()) {
                log.add(new AttackSkipped(attack, 2, SkipReason.NO_TARGET));
                continue;
            }

            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            if (attacker == null || attacker.destroyed()) {
                log.add(new AttackSkipped(attack, 2, SkipReason.ATTACKER_DESTROYED));
                continue;
            }
            Target targetRef = attack.target().orElseThrow();
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
            if (target == null || target.destroyed()) {
                log.add(new AttackSkipped(attack, 2, SkipReason.TARGET_DESTROYED));
                continue;
            }

            int advantage = rules.advantagePermille(
                    attacker.card().subfaction().dataName(), target.card().subfaction().dataName());
            int effectiveAtk = Damage.effectiveAtk(attacker.currentAtk(), advantage);
            int defBefore = target.currentDef();
            int defAfter = defBefore - effectiveAtk;
            boolean killed = defAfter <= 0;
            int spill = killed ? Damage.spill(effectiveAtk, defBefore, evaluation.multiplier(attack.attacker())) : 0;

            target.takeDamage(effectiveAtk);
            if (spill > 0) {
                state.player(attack.attacker().other()).loseLife(spill);
            }
            log.add(new AttackResolved(attack, 2, effectiveAtk, advantage, defBefore, defAfter, killed, spill, false));
            if (killed) {
                log.add(new CreatureDestroyed(targetRef.side(), targetRef.lane(), target.card().id()));
            }
        }
    }

    /** Refill (10): discard destroyed creatures, draw replacements in lane order. Appends events to {@code log}. */
    public void refill(MatchState state, List<MatchEvent> log) {
        Side first = state.player(Side.A).faction() == Faction.POTENZE_ARTIFICIALI
                ? Side.A
                : state.player(Side.B).faction() == Faction.POTENZE_ARTIFICIALI ? Side.B : Side.A;
        Side second = first.other();

        for (Side side : new Side[] {first, second}) {
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

        for (Side side : new Side[] {first, second}) {
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
                    Creature drawn = new Creature(creatureCard, lane);
                    player.place(drawn);
                    log.add(new CardDrawn(side, lane, card.id()));
                    if (pendingEquipment != null) {
                        cardEffects.onEquipment(pendingEquipment, side, drawn);
                        log.add(new EquipmentApplied(side, lane, pendingEquipment.id(), card.id()));
                        pendingEquipment = null;
                    }
                } else if (card instanceof SpellCard spell) {
                    cardEffects.onSpell(spell, side);
                    player.discard().add(spell);
                    log.add(new SpellResolved(side, lane, spell.id()));
                    lane--;
                } else if (card instanceof EquipmentCard equipment) {
                    pendingEquipment = equipment;
                    player.discard().add(equipment);
                    log.add(new EquipmentPending(side, equipment.id()));
                    lane--;
                }
            }
            if (pendingEquipment != null) {
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
