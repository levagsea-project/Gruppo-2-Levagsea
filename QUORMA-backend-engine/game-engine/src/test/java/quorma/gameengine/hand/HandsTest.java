package quorma.gameengine.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The helper is test infrastructure for everyone, so it is tested too. */
class HandsTest {

    @Test
    void explicitLanes() {
        assertEquals(
                List.of(
                        new HandCard(1, 1, Suit.VINCOLO),
                        new HandCard(2, 13, Suit.FRATTURA),
                        new HandCard(3, 10, Suit.SPIRA),
                        new HandCard(4, 12, Suit.VESTIGIO),
                        new HandCard(5, 2, Suit.FRATTURA)),
                Hands.hand("1:AV 2:KF 3:10S 4:QT 5:2F"));
    }

    @Test
    void lanesDefaultToPosition() {
        assertEquals(Hands.hand("1:9V 2:9F 3:JS"), Hands.hand("9V 9F JS"));
    }

    @Test
    void gapsInLanesAreKept() {
        assertEquals(
                List.of(new HandCard(2, 9, Suit.VINCOLO), new HandCard(5, 9, Suit.FRATTURA)),
                Hands.hand("2:9V 5:9F"));
    }

    @Test
    void emptyAndBlankBoards() {
        assertTrue(Hands.hand("").isEmpty());
        assertTrue(Hands.hand("   ").isEmpty());
    }

    @Test
    void caseAndExtraSpacesAreIgnored() {
        assertEquals(Hands.hand("1:AV 2:jf"), Hands.hand("  1:av   2:JF "));
    }

    @Test
    void malformedTokensFail() {
        assertThrows(IllegalArgumentException.class, () -> Hands.hand("1:AX"));   // bad suit
        assertThrows(IllegalArgumentException.class, () -> Hands.hand("1:ZV"));   // bad rank
        assertThrows(IllegalArgumentException.class, () -> Hands.hand("1:V"));    // no rank
        assertThrows(IllegalArgumentException.class, () -> Hands.hand("9:AV"));   // lane out of range
        assertThrows(IllegalArgumentException.class, () -> Hands.hand("1:14V"));  // rank out of range
    }

    @Test
    void resultBuilderAndAssertion() {
        HandResult r = new HandResult(HandType.PAIR, List.of(2, 4), List.of(9, 13, 7, 3));
        Hands.assertResult(r, HandType.PAIR, List.of(2, 4), 9, 13, 7, 3);
        assertEquals(r, Hands.result(HandType.PAIR, List.of(2, 4), 9, 13, 7, 3));
    }

    @Test
    void permutationsAreComplete() {
        List<List<HandCard>> perms = Hands.permutations(Hands.hand("AV KF 10S QT 2F"));
        assertEquals(120, perms.size());
        assertEquals(120, new HashSet<>(perms).size());
    }
}
