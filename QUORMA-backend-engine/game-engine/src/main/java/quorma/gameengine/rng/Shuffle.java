package quorma.gameengine.rng;

import java.util.ArrayList;
import java.util.List;

/** Explicit Fisher-Yates shuffle driven by the module's own deterministic PRNG. */
public final class Shuffle {

    private Shuffle() {}

    /** Shuffles in place: for i = n-1 down to 1, swap element i with element j, j uniform in [0, i]. */
    public static <T> void fisherYates(List<T> items, SplitMix64 rng) {
        for (int i = items.size() - 1; i >= 1; i--) {
            int j = rng.nextInt(i + 1);
            T tmp = items.get(i);
            items.set(i, items.get(j));
            items.set(j, tmp);
        }
    }

    /** Returns a shuffled copy and leaves the input untouched. */
    public static <T> List<T> shuffled(List<T> items, SplitMix64 rng) {
        List<T> copy = new ArrayList<>(items);
        fisherYates(copy, rng);
        return copy;
    }
}
