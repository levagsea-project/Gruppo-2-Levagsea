package quorma.gameengine.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static quorma.gameengine.hand.Hands.assertResult;
import static quorma.gameengine.hand.Hands.hand;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What {@link HandEvaluator#evaluate} must return: hand type, attacker lanes and tiebreak.
 * Every expected value is worked out by hand from ruleset, never from the engine's output.
 * The suit letters of {@link Hands} are V, F, S, T (Vestigio).
 *
 * <h2>Tiebreak encoding (Ace = 14), most significant first</h2>
 * <pre>
 *   FIVE_OF_A_KIND   [rank]
 *   ROYAL_FLUSH      [14]
 *   STRAIGHT_FLUSH   [high card]            A-2-3-4-5 is [5]
 *   FOUR_OF_A_KIND   [quad rank, kicker]    kicker only when there is a fifth creature
 *   FULL_HOUSE       [trips rank, pair rank]
 *   FLUSH            all 5 ranks, high to low
 *   STRAIGHT         [high card]            A-2-3-4-5 is [5]
 *   THREE_OF_A_KIND  [trips rank, kickers high to low]
 *   FOUR_OF_A_SUIT   the 4 suited ranks high to low, then the kicker    kicker only when there is a fifth creature
 *   TWO_PAIR         [high pair, low pair, kicker]    kicker only when there is a fifth creature
 *   PAIR             [pair rank, kickers high to low]
 *   HIGH_CARD        all ranks, high to low
 *   NONE             []
 * </pre>
 */
class HandEvaluatorTest {

    private static final List<Integer> ALL = List.of(1, 2, 3, 4, 5);

    // ---- one test per HandType, 5 creatures ----

    @Test
    void fiveOfAKind() {
        // only reachable when an effect changes ranks: fabricated input
        assertResult(HandEvaluator.evaluate(hand("9V 9F 9S 9T 9V")), HandType.FIVE_OF_A_KIND, ALL, 9);
    }

    @Test
    void fiveOfAKindBeatsFlushWhenAllSameSuit() {
        assertResult(HandEvaluator.evaluate(hand("9F 9F 9F 9F 9F")), HandType.FIVE_OF_A_KIND, ALL, 9);
    }

    @Test
    void royalFlush() {
        assertResult(HandEvaluator.evaluate(hand("10V JV QV KV AV")), HandType.ROYAL_FLUSH, ALL, 14);
    }

    @Test
    void straightFlush() {
        assertResult(HandEvaluator.evaluate(hand("5F 6F 7F 8F 9F")), HandType.STRAIGHT_FLUSH, ALL, 9);
    }

    @Test
    void straightFlushToTheKingIsNotRoyal() {
        assertResult(HandEvaluator.evaluate(hand("9F 10F JF QF KF")), HandType.STRAIGHT_FLUSH, ALL, 13);
    }

    @Test
    void fourOfAKindKickerDoesNotAttack() {
        // 9s in lanes 1, 3, 4, 5; the King in lane 2 is only the kicker
        assertResult(
                HandEvaluator.evaluate(hand("9V KF 9F 9S 9T")), HandType.FOUR_OF_A_KIND, List.of(1, 3, 4, 5), 9, 13);
    }

    @Test
    void fourOfAKindOfAces() {
        assertResult(
                HandEvaluator.evaluate(hand("AV AF AS AT 2V")), HandType.FOUR_OF_A_KIND, List.of(1, 2, 3, 4), 14, 2);
    }

    @Test
    void fullHouse() {
        // trips of Kings in lanes 1, 3, 5, pair of 4s in lanes 2, 4: all five attack
        assertResult(HandEvaluator.evaluate(hand("KV 4F KF 4S KS")), HandType.FULL_HOUSE, ALL, 13, 4);
    }

    @Test
    void flush() {
        assertResult(HandEvaluator.evaluate(hand("2F 9F JF KF 5F")), HandType.FLUSH, ALL, 13, 11, 9, 5, 2);
    }

    @Test
    void flushWithAceCountsAceHigh() {
        assertResult(HandEvaluator.evaluate(hand("AF 9F JF KF 5F")), HandType.FLUSH, ALL, 14, 13, 11, 9, 5);
    }

    @Test
    void straight() {
        assertResult(HandEvaluator.evaluate(hand("5V 6F 7S 8T 9V")), HandType.STRAIGHT, ALL, 9);
    }

    @Test
    void threeOfAKind() {
        // 7s in lanes 2, 4, 5; kickers King and 2 do not attack
        assertResult(
                HandEvaluator.evaluate(hand("KT 7V 2V 7F 7S")), HandType.THREE_OF_A_KIND, List.of(2, 4, 5), 7, 13, 2);
    }

    @Test
    void fourOfASuitWithFourCreatures() {
        assertResult(
                HandEvaluator.evaluate(hand("3V 9V JV 6V")), HandType.FOUR_OF_A_SUIT, List.of(1, 2, 3, 4), 11, 9, 6, 3);
    }

    @Test
    void fourOfASuitWithFiveCreaturesUsesTheOddOneAsKicker() {
        // Vincolo in lanes 1, 3, 4, 5; the Frattura 9 in lane 2 does not attack but is the kicker
        assertResult(
                HandEvaluator.evaluate(hand("3V 9F JV 6V KV")),
                HandType.FOUR_OF_A_SUIT,
                List.of(1, 3, 4, 5),
                13, 11, 6, 3, 9);
    }

    @Test
    void twoPairWithFiveCreatures() {
        // 4s in lanes 1, 3; Kings in lanes 2, 4; the 9 in lane 5 is the kicker and does not attack
        assertResult(
                HandEvaluator.evaluate(hand("4V KF 4F KS 9V")), HandType.TWO_PAIR, List.of(1, 2, 3, 4), 13, 4, 9);
    }

    @Test
    void twoPairWithFourCreatures() {
        assertResult(HandEvaluator.evaluate(hand("4V KF 4S KT")), HandType.TWO_PAIR, List.of(1, 2, 3, 4), 13, 4);
    }

    @Test
    void pair() {
        assertResult(HandEvaluator.evaluate(hand("9V 2F 9F KS 5T")), HandType.PAIR, List.of(1, 3), 9, 13, 5, 2);
    }

    @Test
    void pairOfAcesCountsAceHigh() {
        assertResult(HandEvaluator.evaluate(hand("AV 3F AF 9S 5T")), HandType.PAIR, List.of(1, 3), 14, 9, 5, 3);
    }

    @Test
    void highCardOnlyTheHighestAttacks() {
        assertResult(HandEvaluator.evaluate(hand("2V 9F JS 5T 7V")), HandType.HIGH_CARD, List.of(3), 11, 9, 7, 5, 2);
    }

    @Test
    void highCardAceIsHigh() {
        assertResult(HandEvaluator.evaluate(hand("AV 9F JS 5T 7V")), HandType.HIGH_CARD, List.of(1), 14, 11, 9, 7, 5);
    }

    @Test
    void noCreaturesIsNoHand() {
        assertEquals(HandResult.none(), HandEvaluator.evaluate(hand("")));
        assertEquals(HandResult.none(), HandEvaluator.evaluate(List.of()));
    }

    // ---- fewer than 5 creatures ----

    @Test
    void oneCreatureIsHighCardInItsOwnLane() {
        assertResult(HandEvaluator.evaluate(hand("3:7V")), HandType.HIGH_CARD, List.of(3), 7);
    }

    @Test
    void twoCreaturesHighCard() {
        assertResult(HandEvaluator.evaluate(hand("2:5V 4:9F")), HandType.HIGH_CARD, List.of(4), 9, 5);
    }

    @Test
    void twoCreaturesPairWithGapsInLanes() {
        assertResult(HandEvaluator.evaluate(hand("2:9V 5:9F")), HandType.PAIR, List.of(2, 5), 9);
    }

    @Test
    void threeCreaturesPairWithKicker() {
        assertResult(HandEvaluator.evaluate(hand("9V KF 9S")), HandType.PAIR, List.of(1, 3), 9, 13);
    }

    @Test
    void threeCreaturesThreeOfAKind() {
        assertResult(HandEvaluator.evaluate(hand("9V 9F 9S")), HandType.THREE_OF_A_KIND, List.of(1, 2, 3), 9);
    }

    @Test
    void threeCreaturesOfOneSuitAreOnlyHighCard() {
        // a Flush and a Four of a suit both need more creatures
        assertResult(HandEvaluator.evaluate(hand("2V 5V 9V")), HandType.HIGH_CARD, List.of(3), 9, 5, 2);
    }

    @Test
    void fourCreaturesFourOfAKind() {
        assertResult(HandEvaluator.evaluate(hand("9V 9F 9S 9T")), HandType.FOUR_OF_A_KIND, List.of(1, 2, 3, 4), 9);
    }

    @Test
    void fourCreaturesThreeOfAKindWithKicker() {
        assertResult(HandEvaluator.evaluate(hand("9V 9F 9S KT")), HandType.THREE_OF_A_KIND, List.of(1, 2, 3), 9, 13);
    }

    @Test
    void fourCreaturesCannotFormAStraightOrFlush() {
        // 5-6-7-8 of one suit: not a Straight flush (needs 5 creatures), it is Four of a suit
        assertResult(
                HandEvaluator.evaluate(hand("5V 6V 7V 8V")), HandType.FOUR_OF_A_SUIT, List.of(1, 2, 3, 4), 8, 7, 6, 5);
    }

    // ---- aces and straights ----

    @Test
    void aceLowStraightIsFiveHigh() {
        // list order is not lane order here: lanes are 1..5 by position, ranks 5 A 3 2 4
        assertResult(HandEvaluator.evaluate(hand("5V AV 3S 2F 4T")), HandType.STRAIGHT, ALL, 5);
    }

    @Test
    void aceHighStraightIsFourteenHigh() {
        assertResult(HandEvaluator.evaluate(hand("10V JF QS KT AV")), HandType.STRAIGHT, ALL, 14);
    }

    @Test
    void aceLowStraightFlushIsNotRoyal() {
        assertResult(HandEvaluator.evaluate(hand("AF 2F 3F 4F 5F")), HandType.STRAIGHT_FLUSH, ALL, 5);
    }

    @Test
    void straightsDoNotWrapQueenKingAceTwoThree() {
        // Q-K-A-2-3 is only High card: the Ace in lane 3 attacks
        assertResult(HandEvaluator.evaluate(hand("QV KF AS 2T 3V")), HandType.HIGH_CARD, List.of(3), 14, 13, 12, 3, 2);
    }

    @Test
    void straightsDoNotWrapKingAceTwoThreeFour() {
        assertResult(HandEvaluator.evaluate(hand("KV AF 2S 3T 4V")), HandType.HIGH_CARD, List.of(2), 14, 13, 4, 3, 2);
    }

    // ---- several hands match: the best-ranked one is used ----

    @Test
    void fourOfASuitContainingAPairIsFourOfASuit() {
        // 9V 9V 4V 2V is also a Pair, but Four of a suit ranks higher. Equal ranks in one suit are
        // fabricated input (a rank-changing effect)
        assertResult(
                HandEvaluator.evaluate(hand("9V 9V 4V 2V")), HandType.FOUR_OF_A_SUIT, List.of(1, 2, 3, 4), 9, 9, 4, 2);
    }

    @Test
    void fourOfASuitContainingTwoPairIsFourOfASuit() {
        assertResult(
                HandEvaluator.evaluate(hand("9V 9V 4V 4V")), HandType.FOUR_OF_A_SUIT, List.of(1, 2, 3, 4), 9, 9, 4, 4);
    }

    @Test
    void threeOfAKindBeatsFourOfASuit() {
        // 9V 9V 4V 2V are four Vincolo, but 9 9 9 is a Three of a kind and ranks higher
        assertResult(
                HandEvaluator.evaluate(hand("9V 9F 9V 4V 2V")), HandType.THREE_OF_A_KIND, List.of(1, 2, 3), 9, 4, 2);
    }

    @Test
    void straightBeatsFourOfASuit() {
        // four Vincolo plus a Frattura 9 that completes 5-6-7-8-9
        assertResult(HandEvaluator.evaluate(hand("5V 6V 7V 8V 9F")), HandType.STRAIGHT, ALL, 9);
    }

    // ---- determinism and input rules ----

    @Test
    void resultDoesNotDependOnListOrder() {
        for (String board : List.of("9V 2F 9F KS 5T", "KV 4F KF 4S KS", "5V AV 3S 2F 4T", "4V KF 4F KS 9V")) {
            List<HandCard> cards = hand(board);
            HandResult expected = HandEvaluator.evaluate(cards);
            for (List<HandCard> order : Hands.permutations(cards)) {
                assertEquals(expected, HandEvaluator.evaluate(order), board + " as " + order);
            }
        }
    }

    @Test
    void inputListIsNotModified() {
        // an immutable list throws if the evaluator tries to sort it in place
        List<HandCard> cards = List.copyOf(hand("9V 2F 9F KS 5T"));
        HandEvaluator.evaluate(cards);
        assertEquals(hand("9V 2F 9F KS 5T"), cards);
    }

    @Test
    void twoCreaturesInTheSameLaneAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HandEvaluator.evaluate(hand("1:9V 1:5F")));
    }

    @Test
    void moreThanFiveCreaturesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HandEvaluator.evaluate(hand("1:2V 2:3V 3:4V 4:5V 5:6V 1:9F")));
    }
}
