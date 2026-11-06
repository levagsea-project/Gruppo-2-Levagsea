package quorma.gameengine.rng;

/**
 * Deterministic PRNG (SplitMix64), implemented here on purpose: never a library default.
 * Same seed, same sequence, on every JVM.
 */
public final class SplitMix64 {

    private long state;

    public SplitMix64(long seed) {
        this.state = seed;
    }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform integer in [0, bound), unbiased (rejection sampling). */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long maxAccepted = Long.MAX_VALUE - (Long.MAX_VALUE % bound + 1) % bound;
        long r;
        do {
            r = nextLong() >>> 1;
        } while (r > maxAccepted);
        return (int) (r % bound);
    }
}
