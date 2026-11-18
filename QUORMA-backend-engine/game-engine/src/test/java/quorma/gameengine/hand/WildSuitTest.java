package quorma.gameengine.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static quorma.gameengine.hand.Hands.hand;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** A wild card takes the suit that gives the best hand; ties keep the lowest suit ordinal. */
class WildSuitTest {

    @Test
    void noWildCardIsPlainEvaluation() {
        var cards = hand("1:AV 2:3F 3:6S 4:9T 5:KV");
        assertEquals(HandEvaluator.evaluate(cards), WildSuit.evaluate(cards, Set.of()));
    }

    @Test
    void wildCardCompletesAFlush() {
        // Four Vincolo and one Frattura: the Frattura card takes Vincolo.
        var result = WildSuit.evaluate(hand("1:AV 2:3V 3:5V 4:7V 5:9F"), Set.of(5));
        assertEquals(HandType.FLUSH, result.type());
    }

    @Test
    void wildCardCompletesFourOfASuit() {
        var result = WildSuit.evaluate(hand("1:2V 2:5V 3:8V 4:10F 5:4S"), Set.of(4));
        assertEquals(HandType.FOUR_OF_A_SUIT, result.type());
    }

    @Test
    void twoWildCardsAreChosenTogether() {
        // Three Spira plus two wild cards: only both taking Spira makes a Flush.
        var result = WildSuit.evaluate(hand("1:AS 2:3S 3:5S 4:7V 5:9F"), Set.of(4, 5));
        assertEquals(HandType.FLUSH, result.type());
    }

    @Test
    void tieKeepsTheLowestSuitOrdinal() {
        // No suit changes the hand (High card), so the result equals the one with the wild card as Vincolo,
        // whatever suit the input carried.
        var expected = HandEvaluator.evaluate(hand("1:AV 2:3F 3:6S 4:9T 5:KV"));
        assertEquals(expected, WildSuit.evaluate(hand("1:AT 2:3F 3:6S 4:9T 5:KV"), Set.of(1)));
    }

    @Test
    void inputOrderDoesNotMatter() {
        var a = WildSuit.evaluate(hand("1:AV 2:3V 3:5V 4:7V 5:9F"), Set.of(5));
        var shuffled = hand("5:9F 3:5V 1:AV 4:7V 2:3V");
        assertEquals(a, WildSuit.evaluate(shuffled, Set.of(5)));
    }
}
