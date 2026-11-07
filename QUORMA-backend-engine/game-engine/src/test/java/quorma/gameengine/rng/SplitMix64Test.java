package quorma.gameengine.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SplitMix64Test {

    @Test
    void referenceVectorForSeedZero() {
        // Published SplitMix64 reference outputs for seed 0.
        SplitMix64 rng = new SplitMix64(0L);
        assertEquals(0xE220A8397B1DCDAFL, rng.nextLong());
        assertEquals(0x6E789E6AA1B965F4L, rng.nextLong());
    }

    @Test
    void sameSeedSameSequence() {
        SplitMix64 a = new SplitMix64(42L);
        SplitMix64 b = new SplitMix64(42L);
        for (int i = 0; i < 1000; i++) {
            assertEquals(a.nextInt(32), b.nextInt(32));
        }
    }

    @Test
    void nextIntStaysInBounds() {
        SplitMix64 rng = new SplitMix64(7L);
        for (int i = 0; i < 10_000; i++) {
            int v = rng.nextInt(5);
            assertTrue(v >= 0 && v < 5);
        }
    }

    @Test
    void rejectsNonPositiveBound() {
        assertThrows(IllegalArgumentException.class, () -> new SplitMix64(1L).nextInt(0));
    }
}
