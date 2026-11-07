package quorma.gameengine.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static quorma.gameengine.hand.Hands.hand;
import static quorma.gameengine.hand.Hands.result;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What {@link HandEvaluator#compare} must return. Only the sign counts: positive when the first hand is better,
 * negative when the second is, zero for a tie. Most tests build the {@link HandResult} directly,
 * so they do not depend on {@code evaluate}. The ones that need real boards say so.
 * Tiebreak layout per hand type is in {@link HandEvaluatorTest}.
 */
class HandCompareTest {

    private static HandResult r(HandType type, Integer... tiebreak) {
        return result(type, List.of(), tiebreak);
    }

    private static void assertBeats(HandResult better, HandResult worse) {
        assertTrue(HandEvaluator.compare(better, worse) > 0, better + " should beat " + worse);
        assertTrue(HandEvaluator.compare(worse, better) < 0, worse + " should lose to " + better);
    }

    private static void assertTie(HandResult a, HandResult b) {
        assertEquals(0, HandEvaluator.compare(a, b), a + " should tie " + b);
        assertEquals(0, HandEvaluator.compare(b, a), b + " should tie " + a);
    }

    // ---- ranking of hand types ----

    @Test
    void everyTypeBeatsEveryWorseTypeWhateverTheTiebreak() {
        HandType[] types = HandType.values(); // ordinal 0 = best
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                // the better type has the weakest tiebreak, the worse type the strongest
                assertBeats(r(types[i], 2), r(types[j], 14));
            }
        }
    }

    @Test
    void rankingFollowsTheRulesetOrder() {
        // the ruleset list, best to worst, written out so a reordered enum is caught
        List<HandType> ruleset = List.of(
                HandType.FIVE_OF_A_KIND, HandType.ROYAL_FLUSH, HandType.STRAIGHT_FLUSH, HandType.FOUR_OF_A_KIND,
                HandType.FULL_HOUSE, HandType.FLUSH, HandType.STRAIGHT, HandType.THREE_OF_A_KIND,
                HandType.FOUR_OF_A_SUIT, HandType.TWO_PAIR, HandType.PAIR, HandType.HIGH_CARD, HandType.NONE);
        assertEquals(ruleset, List.of(HandType.values()));
    }

    @Test
    void fourOfASuitSitsBetweenThreeOfAKindAndTwoPair() {
        assertBeats(r(HandType.THREE_OF_A_KIND, 2, 3, 4), r(HandType.FOUR_OF_A_SUIT, 13, 12, 11, 10));
        assertBeats(r(HandType.FOUR_OF_A_SUIT, 5, 4, 3, 2), r(HandType.TWO_PAIR, 14, 13, 12));
    }

    @Test
    void noHandLosesToHighCardAndTiesNoHand() {
        assertBeats(r(HandType.HIGH_CARD, 2), HandResult.none());
        assertTie(HandResult.none(), HandResult.none());
    }

    @Test
    void aHandTiesItself() {
        HandResult pair = r(HandType.PAIR, 9, 13, 5, 2);
        assertTie(pair, pair);
    }

    // ---- tie-breaks within a type ----

    @Test
    void pairOfNinesBeatsPairOfEights() {
        assertBeats(r(HandType.PAIR, 9, 3, 2, 4), r(HandType.PAIR, 8, 14, 13, 12));
    }

    @Test
    void equalPairsGoToTheHighestKicker() {
        assertBeats(r(HandType.PAIR, 9, 13, 5, 2), r(HandType.PAIR, 9, 13, 4, 3));
        assertBeats(r(HandType.PAIR, 9, 13, 5, 2), r(HandType.PAIR, 9, 12, 11, 10));
    }

    @Test
    void twoPairComparesHighPairThenLowPairThenKicker() {
        assertBeats(r(HandType.TWO_PAIR, 13, 4, 2), r(HandType.TWO_PAIR, 12, 11, 14));
        assertBeats(r(HandType.TWO_PAIR, 9, 8, 3), r(HandType.TWO_PAIR, 9, 7, 14));
        assertBeats(r(HandType.TWO_PAIR, 9, 8, 5), r(HandType.TWO_PAIR, 9, 8, 4));
    }

    @Test
    void threeOfAKindComparesTripsThenKickers() {
        assertBeats(r(HandType.THREE_OF_A_KIND, 8, 3, 2), r(HandType.THREE_OF_A_KIND, 7, 14, 13));
        assertBeats(r(HandType.THREE_OF_A_KIND, 7, 13, 2), r(HandType.THREE_OF_A_KIND, 7, 12, 14));
    }

    @Test
    void fullHouseComparesTripsFirst() {
        assertBeats(r(HandType.FULL_HOUSE, 13, 4), r(HandType.FULL_HOUSE, 12, 14));
        assertBeats(r(HandType.FULL_HOUSE, 9, 5), r(HandType.FULL_HOUSE, 9, 4));
    }

    @Test
    void fourOfAKindComparesTheQuadThenTheKicker() {
        assertBeats(r(HandType.FOUR_OF_A_KIND, 10, 2), r(HandType.FOUR_OF_A_KIND, 9, 14));
        assertBeats(r(HandType.FOUR_OF_A_KIND, 9, 13), r(HandType.FOUR_OF_A_KIND, 9, 12));
    }

    @Test
    void flushComparesCardByCard() {
        assertBeats(r(HandType.FLUSH, 14, 9, 7, 5, 3), r(HandType.FLUSH, 14, 9, 7, 5, 2));
        assertBeats(r(HandType.FLUSH, 14, 9, 7, 5, 2), r(HandType.FLUSH, 13, 12, 11, 10, 8));
    }

    @Test
    void straightComparesItsHighCard() {
        assertBeats(r(HandType.STRAIGHT, 9), r(HandType.STRAIGHT, 8));
        assertBeats(r(HandType.STRAIGHT, 14), r(HandType.STRAIGHT, 13));
    }

    @Test
    void straightFlushComparesItsHighCard() {
        assertBeats(r(HandType.STRAIGHT_FLUSH, 9), r(HandType.STRAIGHT_FLUSH, 8));
    }

    @Test
    void fourOfASuitComparesItsFourRanksThenTheKicker() {
        // the four suited ranks decide first, even against a much better kicker
        assertBeats(r(HandType.FOUR_OF_A_SUIT, 13, 11, 6, 3, 2), r(HandType.FOUR_OF_A_SUIT, 13, 11, 6, 2, 14));
        // equal four ranks: the fifth creature breaks the tie
        assertBeats(r(HandType.FOUR_OF_A_SUIT, 13, 11, 6, 3, 9), r(HandType.FOUR_OF_A_SUIT, 13, 11, 6, 3, 8));
        assertBeats(r(HandType.FOUR_OF_A_SUIT, 13, 11, 6, 3), r(HandType.FOUR_OF_A_SUIT, 13, 11, 6, 2));
        assertBeats(r(HandType.FOUR_OF_A_SUIT, 13, 11, 6, 3), r(HandType.FOUR_OF_A_SUIT, 13, 10, 9, 8));
        assertBeats(r(HandType.FOUR_OF_A_SUIT, 14, 3, 2, 2), r(HandType.FOUR_OF_A_SUIT, 13, 12, 11, 10));
    }

    @Test
    void fiveOfAKindComparesItsRank() {
        assertBeats(r(HandType.FIVE_OF_A_KIND, 10), r(HandType.FIVE_OF_A_KIND, 9));
    }

    @Test
    void royalFlushesTie() {
        assertTie(r(HandType.ROYAL_FLUSH, 14), r(HandType.ROYAL_FLUSH, 14));
    }

    @Test
    void highCardComparesAllRanksHighToLow() {
        assertBeats(r(HandType.HIGH_CARD, 14, 9, 7, 5, 3), r(HandType.HIGH_CARD, 14, 9, 7, 5, 2));
        assertBeats(r(HandType.HIGH_CARD, 12, 3), r(HandType.HIGH_CARD, 11, 10));
    }

    // ---- real boards: aces and suits (needs evaluate) ----

    @Test
    void aceLowStraightLosesToTwoThroughSix() {
        assertBeats(
                HandEvaluator.evaluate(hand("2V 3F 4S 5T 6V")),
                HandEvaluator.evaluate(hand("AV 2F 3S 4T 5V")));
    }

    @Test
    void aceLowStraightBeatsHighCardAndTiesAnotherAceLowStraight() {
        HandResult wheel = HandEvaluator.evaluate(hand("AV 2F 3S 4T 5V"));
        assertBeats(wheel, HandEvaluator.evaluate(hand("KV QF JS 9T 2V")));
        assertTie(wheel, HandEvaluator.evaluate(hand("5S 4V 3T 2F AS")));
    }

    @Test
    void aceHighStraightBeatsNineThroughKing() {
        assertBeats(
                HandEvaluator.evaluate(hand("10V JF QS KT AV")),
                HandEvaluator.evaluate(hand("9V 10F JS QT KV")));
    }

    @Test
    void royalFlushBeatsStraightFlush() {
        assertBeats(
                HandEvaluator.evaluate(hand("10V JV QV KV AV")),
                HandEvaluator.evaluate(hand("9F 10F JF QF KF")));
    }

    @Test
    void flushBeatsStraightAndStraightFlushBeatsFourOfAKind() {
        assertBeats(
                HandEvaluator.evaluate(hand("2F 9F JF KF 5F")),
                HandEvaluator.evaluate(hand("5V 6F 7S 8T 9V")));
        assertBeats(
                HandEvaluator.evaluate(hand("5F 6F 7F 8F 9F")),
                HandEvaluator.evaluate(hand("9V KF 9F 9S 9T")));
    }

    @Test
    void fourOfASuitBeatsThreeOfAKindOnlyWhenNoTripsExist() {
        // 3 3 3 + two cards is Three of a kind; four Vincolo with no trips is Four of a suit
        assertBeats(
                HandEvaluator.evaluate(hand("3V 3F 3S KT 2V")),
                HandEvaluator.evaluate(hand("2V 5V 9V KV 7F")));
    }

    @Test
    void suitsNeverBreakTies() {
        // same ranks, different suits
        assertTie(
                HandEvaluator.evaluate(hand("9V 9F KS 5T 2V")),
                HandEvaluator.evaluate(hand("9S 9T KV 5F 2S")));
        assertTie(
                HandEvaluator.evaluate(hand("3V 9V JV 6V")),
                HandEvaluator.evaluate(hand("3F 9F JF 6F")));
    }

    @Test
    void lanesNeverBreakTies() {
        assertTie(
                HandEvaluator.evaluate(hand("9V 9F KS 5T 2V")),
                HandEvaluator.evaluate(hand("KS 2V 9F 5T 9V")));
    }
}
