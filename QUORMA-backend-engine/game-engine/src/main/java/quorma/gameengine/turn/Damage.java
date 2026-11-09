package quorma.gameengine.turn;

/** Damage arithmetic (ruleset 9.1 and 9.6). Pure functions, integers only. */
public final class Damage {

    private Damage() {}

    /**
     * Effective ATK after the subfaction advantage: {@code floorDiv(currentAtk * advantagePermille, 1000)}.
     * Strong 1100, weak 900, neutral 1000. Intermediate products use {@code long}.
     */
    public static int effectiveAtk(int currentAtk, int advantagePermille) {
        long atk = Math.max(0, currentAtk);
        return (int) (atk * advantagePermille / 1000L);
    }

    /**
     * Spill damage to the opponent's life when an attack kills (ruleset 9.6):
     * {@code floor(max(0, effectiveAtk - targetDefBeforeBlow) * multiplierPermille / 1000)}.
     *
     * @param targetDefBeforeBlow the target's DEF just before this blow
     * @param multiplierPermille  the attacker's hand multiplier, for example 1100 for a Pair
     */
    public static int spill(int effectiveAtk, int targetDefBeforeBlow, int multiplierPermille) {
        long excess = Math.max(0L, (long) effectiveAtk - targetDefBeforeBlow);
        return (int) (excess * multiplierPermille / 1000);
    }
}
