package quorma.gameengine.turn;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandEvaluator;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.MatchState;

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

    //TODO(task-2): implement ruleset 6 to 11
    
    /** The real evaluator and comparator, and spell and equipment hooks that do nothing. */
    public TurnResolver(Ruleset rules) {
        this(rules, HandEvaluator::evaluate, HandEvaluator::compare, CardEffects.NONE);
    }

    /** Plays the turn of {@code state}: every phase in order. Moves {@code state} to the next turn if the match goes on. */
    public TurnReport playTurn(MatchState state) {
        throw new UnsupportedOperationException("TurnResolver.playTurn is not implemented yet");
    }

    /** Evaluate (7): each player's hand from their creatures in lane order, and the multiplier. */
    public Evaluation evaluate(MatchState state) {
        throw new UnsupportedOperationException("TurnResolver.evaluate is not implemented yet");
    }

    /**
     * Declare (8): one attack for each attacker of each hand, with its frozen target. Ordered by attacker lane,
     * and within a lane by faction priority, then {@code Side.A} first.
     */
    public List<Attack> declare(MatchState state, Evaluation evaluation) {
        throw new UnsupportedOperationException("TurnResolver.declare is not implemented yet");
    }

    /** Cycle 1 (9.2, 9.5 to 9.7): attacks on the opposite lane, simultaneous. Appends events to {@code log}. */
    public void cycle1(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        throw new UnsupportedOperationException("TurnResolver.cycle1 is not implemented yet");
    }

    /** Cycle 2 (9.3): the other attacks, one at a time in attacker lane order. Appends events to {@code log}. */
    public void cycle2(MatchState state, Evaluation evaluation, List<Attack> attacks, List<MatchEvent> log) {
        throw new UnsupportedOperationException("TurnResolver.cycle2 is not implemented yet");
    }

    /** Refill (10): discard destroyed creatures, draw replacements in lane order. Appends events to {@code log}. */
    public void refill(MatchState state, List<MatchEvent> log) {
        throw new UnsupportedOperationException("TurnResolver.refill is not implemented yet");
    }

    /** End of turn (11): the result if the match is over, empty if it goes on. Appends events to {@code log}. */
    public Optional<MatchResult> endOfTurn(MatchState state, Evaluation evaluation, List<MatchEvent> log) {
        throw new UnsupportedOperationException("TurnResolver.endOfTurn is not implemented yet");
    }
}
