package quorma.gameengine.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShuffleTest {

    private static List<Integer> range(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(i);
        }
        return l;
    }

    @Test
    void sameSeedGivesSameOrder() {
        List<Integer> a = Shuffle.shuffled(range(52), new SplitMix64(2026L));
        List<Integer> b = Shuffle.shuffled(range(52), new SplitMix64(2026L));
        assertEquals(a, b);
    }

    @Test
    void goldenOrdersComputedIndependentlyFromTheSpec() {
        assertEquals(List.of(5, 6, 1, 4, 8, 3, 2, 9, 0, 7), Shuffle.shuffled(range(10), new SplitMix64(0L)));
        assertEquals(List.of(3, 8, 4, 2, 9, 5, 7, 1, 0, 6), Shuffle.shuffled(range(10), new SplitMix64(42L)));
        assertEquals(List.of(0, 3, 1, 4, 2), Shuffle.shuffled(range(5), new SplitMix64(12345L)));
    }

    @Test
    void differentSeedsGiveDifferentOrders() {
        assertNotEquals(
                Shuffle.shuffled(range(52), new SplitMix64(1L)),
                Shuffle.shuffled(range(52), new SplitMix64(2L)));
    }

    @Test
    void resultIsAPermutation() {
        List<Integer> out = Shuffle.shuffled(range(52), new SplitMix64(99L));
        assertEquals(52, out.size());
        assertEquals(new HashSet<>(range(52)), new HashSet<>(out));
    }

    @Test
    void shuffledLeavesInputUntouchedAndInPlaceVariantMutates() {
        List<Integer> in = range(10);
        Shuffle.shuffled(in, new SplitMix64(7L));
        assertEquals(range(10), in);
        Shuffle.fisherYates(in, new SplitMix64(0L));
        assertEquals(List.of(5, 6, 1, 4, 8, 3, 2, 9, 0, 7), in);
    }

    @Test
    void edgeCases() {
        assertEquals(List.of(), Shuffle.shuffled(List.of(), new SplitMix64(1L)));
        assertEquals(List.of(1), Shuffle.shuffled(List.of(1), new SplitMix64(1L)));
        assertEquals(2, Shuffle.shuffled(List.of(1, 2), new SplitMix64(1L)).size());
    }

    @Test
    void playerOneThenPlayerTwoFromOneRngIsStable() {
        SplitMix64 rng = new SplitMix64(0L);
        List<Integer> p1 = Shuffle.shuffled(range(10), rng);
        List<Integer> p2 = Shuffle.shuffled(range(10), rng);
        assertEquals(List.of(5, 6, 1, 4, 8, 3, 2, 9, 0, 7), p1);
        SplitMix64 again = new SplitMix64(0L);
        Shuffle.shuffled(range(10), again);
        assertEquals(p2, Shuffle.shuffled(range(10), again));
    }
}
