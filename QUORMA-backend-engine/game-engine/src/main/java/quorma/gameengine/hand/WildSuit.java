package quorma.gameengine.hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Hand input for creatures that count as any suit for Colore and Quattro dello stesso seme (Camaleonte,
 * {@code 2026-11-01-camaleonte-suit}). The evaluator only sees one effective suit per card, so the choice is made
 * here, before it: every assignment of suits to the wild cards is tried and the best hand wins
 * ({@code 2026-11-17-camaleonte-best-hand}). On a tie the first assignment found wins, trying wild cards in
 * lane order and suits in {@link Suit} ordinal order, so the lowest ordinals are kept and the result is
 * deterministic. At most 4^5 = 1024 evaluations.
 */
public final class WildSuit {

    private WildSuit() {}

    /**
     * @param cards     0 to 5 cards with distinct lanes; the suit of a wild card is ignored
     * @param wildLanes lanes of the cards that take any suit
     */
    public static HandResult evaluate(List<HandCard> cards, Set<Integer> wildLanes) {
        return evaluate(cards, wildLanes, HandEvaluator::evaluate, HandEvaluator::compare);
    }

    /** Same, with the evaluator and comparator given (the turn resolver's own, so tests can replace them). */
    public static HandResult evaluate(List<HandCard> cards, Set<Integer> wildLanes,
                                      Function<List<HandCard>, HandResult> evaluator, Comparator<HandResult> comparator) {
        List<HandCard> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> Integer.compare(a.lane(), b.lane()));
        List<Integer> wildIndexes = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            if (wildLanes.contains(sorted.get(i).lane())) {
                wildIndexes.add(i);
            }
        }
        return search(sorted, wildIndexes, 0, null, evaluator, comparator);
    }

    private static HandResult search(List<HandCard> cards, List<Integer> wildIndexes, int next, HandResult best,
                                     Function<List<HandCard>, HandResult> evaluator, Comparator<HandResult> comparator) {
        if (next == wildIndexes.size()) {
            HandResult result = evaluator.apply(List.copyOf(cards));
            return best == null || comparator.compare(result, best) > 0 ? result : best;
        }
        int index = wildIndexes.get(next);
        HandCard original = cards.get(index);
        HandResult found = best;
        for (Suit suit : Suit.values()) {
            cards.set(index, new HandCard(original.lane(), original.rank(), suit));
            found = search(cards, wildIndexes, next + 1, found, evaluator, comparator);
        }
        cards.set(index, original);
        return found;
    }
}
