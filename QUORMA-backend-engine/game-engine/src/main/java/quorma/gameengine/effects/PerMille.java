package quorma.gameengine.effects;

/** Integer per-mille math in one place, so that "floor every result" is enforced in one spot. */
public final class PerMille {

    private PerMille() {}

    /** {@code floor(value × permille / 1000)}, correct for a negative {@code value} or {@code permille}. */
    public static int floor(long value, int permille) {
        return Math.toIntExact(Math.floorDiv(value * permille, 1000L));
    }

    /** The amount of an E1 or E2: {@code amount} (flat) or a share of {@code base} ({@code permille}, or {@code pct}). */
    public static int baseAmount(BoundExpansion e, int base) {
        if ("flat".equals(e.stringParam("mode").orElse("flat"))) {
            return e.intParam("amount");
        }
        int permille = e.effectParams().containsKey("permille") ? e.intParam("permille") : e.intParam("pct") * 10;
        return floor(base, permille);
    }
}
