package quorma.gameengine.hand;

import java.util.List;
import quorma.gameengine.data.Ruleset;

/**
 * Poker hand evaluation for the Valutazione phase.<br>
 *
 * {@link #evaluate} and {@link #compare} are not
 * implemented yet; {@link #spillMultiplierPermille} is.
 */
public final class HandEvaluator {

    private HandEvaluator() {}

    /**
     * Finds the best hand formed by the creatures in play and the creatures that attack.
     *
     * <p>Input is 0 to 5 cards with distinct lanes, in any order. The result must not depend on the order
     * of the list. An empty list gives {@link HandResult#none()}.
     *
     * @throws IllegalArgumentException if there are more than 5 cards or two cards share a lane
     */
    public static HandResult evaluate(List<HandCard> creatures) {
        throw new UnsupportedOperationException("HandEvaluator.evaluate is not implemented yet");
    }

    /**
     * Compares two results: positive if {@code a} is the better hand, negative if {@code b} is, zero for a
     * tie. A lower {@link HandType} ordinal wins; equal types compare their {@link HandResult#tiebreak()}.
     * Suits and lanes never break a tie.
     */
    public static int compare(HandResult a, HandResult b) {
        //TODO: implement this method
        throw new UnsupportedOperationException("HandEvaluator.compare is not implemented yet");
    }

    /**
     * Hand multiplier for spill damage, in per mille (1100 = x1.1), read from
     * {@code balance.hand_multipliers_permille} in the ruleset. {@link HandType#NONE} is 0.
     *
     * @throws quorma.gameengine.data.RulesetException if the ruleset has no entry for {@code type}
     */
    public static int spillMultiplierPermille(Ruleset rules, HandType type) {
        //TODO: implement this method
        return rules.handMultiplierPermille(type.name());
    }
}
