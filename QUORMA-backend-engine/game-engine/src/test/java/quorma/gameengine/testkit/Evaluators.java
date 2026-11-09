package quorma.gameengine.testkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.turn.Evaluation;

/**
 * Test helper: stand-ins for the hand evaluator and comparator.
 * Each helper gives fixed answers.
 */
public final class Evaluators {

    private Evaluators() {}

    /** A hand of the given type with the given attacker lanes and an empty tiebreak. */
    public static HandResult hand(HandType type, int... attackerLanes) {
        return new HandResult(type, Arrays.stream(attackerLanes).boxed().toList(), List.of());
    }

    /**
     * An evaluator that ignores the cards and returns the given results in turn, one per call, starting again
     * from the first after the last. {@code TurnResolver} evaluates player A first, then player B, so
     * {@code sequence(handA, handB)} gives A its hand and B its hand on every turn.
     */
    public static Function<List<HandCard>, HandResult> sequence(HandResult... results) {
        return new Function<>() {
            private int next;

            @Override
            public HandResult apply(List<HandCard> cards) {
                HandResult result = results[next % results.length];
                next++;
                return result;
            }
        };
    }

    /** Wraps an evaluator and remembers the card lists it was called with, in call order. */
    public static final class Recording implements Function<List<HandCard>, HandResult> {

        private final Function<List<HandCard>, HandResult> delegate;
        private final List<List<HandCard>> calls = new ArrayList<>();

        public Recording(Function<List<HandCard>, HandResult> delegate) {
            this.delegate = delegate;
        }

        @Override
        public HandResult apply(List<HandCard> cards) {
            calls.add(List.copyOf(cards));
            return delegate.apply(cards);
        }

        public List<List<HandCard>> calls() {
            return calls;
        }
    }

    /**
     * A comparator that only looks at the hand type: the better type wins, same type is a tie. Positive when
     * the first hand is better, like {@code HandEvaluator.compare}.
     */
    public static Comparator<HandResult> byType() {
        return (a, b) -> Integer.compare(b.type().ordinal(), a.type().ordinal());
    }

    /** An {@link Evaluation} with the given hands and multipliers (per mille). */
    public static Evaluation evaluation(HandResult a, int multiplierA, HandResult b, int multiplierB) {
        return new Evaluation(a, b, multiplierA, multiplierB);
    }
}
