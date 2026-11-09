package quorma.gameengine.testkit;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.hand.HandCard;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.turn.CardEffects;
import quorma.gameengine.turn.TurnResolver;

/** Test helper: builds a {@link TurnResolver} with the real ruleset and stand-in hand logic. */
public final class TurnResolvers {

    /** The real ruleset, as bundled by the build ({@code data/*.json}). */
    public static final Ruleset RULES = RulesetLoader.fromClasspath();

    private TurnResolvers() {}

    /** Evaluator and comparator as given, spell and equipment hooks that do nothing. */
    public static TurnResolver of(Function<List<HandCard>, HandResult> evaluator, Comparator<HandResult> comparator) {
        return new TurnResolver(RULES, evaluator, comparator, CardEffects.NONE);
    }

    /** The given evaluator and {@link Evaluators#byType()}. */
    public static TurnResolver with(Function<List<HandCard>, HandResult> evaluator) {
        return of(evaluator, Evaluators.byType());
    }

    /**
     * For tests of a single phase that gets its hands in an {@code Evaluation}: calling the evaluator is a
     * test failure, because that phase must not evaluate hands.
     */
    public static TurnResolver noEvaluation() {
        return with(cards -> {
            throw new AssertionError("this phase must not evaluate hands");
        });
    }
}
