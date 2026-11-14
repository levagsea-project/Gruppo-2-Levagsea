package quorma.gameengine.turn;

import java.util.List;

/**
 * Damage arithmetic. Pure functions, integers only, every step floored. Order (Regola d'Oro 2): subfaction advantage,
 * flat reductions, percentage reductions, rounding, then the minimum damage of a flat reduction.
 */
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
     * What is left of a reduction after Perforante: {@code reduction - floor(reduction * piercePct / 100)}.
     *
     * @param piercePct 0..100
     */
    public static int pierce(int reduction, int piercePct) {
        return Math.toIntExact(reduction - (long) reduction * piercePct / 100);
    }

    /**
     * Share of the damage that goes through, in per mille, after the target's percentage reductions:
     * 
     * @param reductionPcts the target's reduction percentages, 0..100 each
     * @param piercePct     the attacker's pierce percentage, 0..100
     * @param floorPermille {@code balance.globals.mitigation_floor_permille}
     */
    public static int mitigationPermille(List<Integer> reductionPcts, int piercePct, int floorPermille) {
        long m = 1000;
        for (int pct : reductionPcts) {
            m = m * (1000 - 10L * pct) / 1000;
        }
        int reduction = Math.min((int) (1000 - m), 1000 - floorPermille);
        return 1000 - pierce(reduction, piercePct);
    }

    /**
     * Damage dealt to the target: {@code floor(max(0, effectiveAtk - flatReduction) * mitigationPermille / 1000)},
     * then at least {@code minDamage} if the effective ATK was above 0. 0 when the effective ATK is 0.
     *
     * @param flatReduction the target's flat reductions, already pierced
     * @param minDamage     the largest {@code min_damage} of the target's flat reductions, 0 if it has none
     */
    public static int damage(int effectiveAtk, int flatReduction, int mitigationPermille, int minDamage) {
        if (effectiveAtk <= 0) {
            return 0;
        }
        long afterFlat = Math.max(0L, (long) effectiveAtk - flatReduction);
        int damage = (int) (afterFlat * mitigationPermille / 1000);
        return Math.max(damage, minDamage);
    }

    /**
     * The multiplier used for spill: the hand multiplier times each replacement factor, in order, each step floored.
     */
    public static int spillMultiplier(int handMultiplierPermille, List<Integer> factorsPermille) {
        long m = handMultiplierPermille;
        for (int factor : factorsPermille) {
            m = m * factor / 1000;
        }
        return Math.toIntExact(m);
    }

    /**
     * Spill damage to the opponent's life when an attack kills:
     * {@code floor(max(0, damage - targetDefBeforeBlow) * multiplierPermille / 1000)}.
     *
     * @param damage              damage dealt by the blow, after reductions
     * @param targetDefBeforeBlow the target's DEF just before this blow
     * @param multiplierPermille  the spill multiplier, for example 1100 for a Pair
     */
    public static int spill(int damage, int targetDefBeforeBlow, int multiplierPermille) {
        long excess = Math.max(0L, (long) damage - targetDefBeforeBlow);
        return (int) (excess * multiplierPermille / 1000);
    }
}
