package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Damage arithmetic: effective ATK (ruleset 9.1) and spill (9.6). Every expected value is worked out by hand;
 * multipliers are the ruleset's, per mille (Pair 1100, Four of a suit 1350, ...).
 */
class DamageTest {

    // ---- effective ATK: floorDiv(currentAtk * permille, 1000) ----

    @Test
    void aStrongAttackerGetsOnePointOne() {
        assertEquals(4400, Damage.effectiveAtk(4000, 1100));
    }

    @Test
    void aWeakAttackerGetsNinePointZero() {
        assertEquals(3600, Damage.effectiveAtk(4000, 900));
    }

    @Test
    void aNeutralAttackerIsUnchanged() {
        assertEquals(4000, Damage.effectiveAtk(4000, 1000));
    }

    @Test
    void theResultIsFlooredNotRounded() {
        assertEquals(3666, Damage.effectiveAtk(3333, 1100)); // 3666.3
        assertEquals(2999, Damage.effectiveAtk(3333, 900)); // 2999.7
        assertEquals(0, Damage.effectiveAtk(1, 900)); // 0.9
        assertEquals(1, Damage.effectiveAtk(1, 1100)); // 1.1
    }

    @Test
    void aNegativeAtkNeverHealsTheTarget() {
        // effective ATK is clamped at 0, whatever the debuff and the advantage
        assertEquals(0, Damage.effectiveAtk(-1, 1100));
        assertEquals(0, Damage.effectiveAtk(-1, 900));
        assertEquals(0, Damage.effectiveAtk(-4000, 1000));
    }

    @Test
    void zeroAtkStaysZero() {
        assertEquals(0, Damage.effectiveAtk(0, 1100));
    }

    @Test
    void theProductDoesNotOverflowAnInt() {
        // 3_000_000 * 1000 = 3_000_000_000 is above Integer.MAX_VALUE; the result itself fits
        assertEquals(3_000_000, Damage.effectiveAtk(3_000_000, 1000));
    }

    // ---- spill: floor(max(0, effectiveAtk - targetDefBeforeBlow) * multiplier / 1000) ----

    @Test
    void spillIsTheExcessTimesTheMultiplier() {
        assertEquals(2500, Damage.spill(4000, 1500, 1000)); // High card: 2500 * 1.0
        assertEquals(2750, Damage.spill(4000, 1500, 1100)); // Pair: 2500 * 1.1
        assertEquals(2200, Damage.spill(5000, 3000, 1100)); // 2000 * 1.1
        assertEquals(34993, Damage.spill(9999, 1, 3500)); // 9998 * 3.5
    }

    @Test
    void spillIsFloored() {
        // 2501 * 1350 / 1000 = 3376.35
        assertEquals(3376, Damage.spill(4001, 1500, 1350));
    }

    @Test
    void anAtkEqualToDefKillsWithNoSpill() {
        assertEquals(0, Damage.spill(1500, 1500, 2200));
    }

    @Test
    void anAtkBelowDefNeverGivesNegativeSpill() {
        assertEquals(0, Damage.spill(1000, 1500, 2200));
    }

    @Test
    void aMultiplierOfZeroGivesNoSpill() {
        // the multiplier of a board with no hand (HandType.NONE)
        assertEquals(0, Damage.spill(4000, 1500, 0));
    }

    @Test
    void theProductDoesNotOverflowAnIntInSpill() {
        // 1_999_999 * 3500 = 6_999_996_500 is above Integer.MAX_VALUE; 6_999_996_500 / 1000 = 6_999_996.5
        assertEquals(6_999_996, Damage.spill(2_000_000, 1, 3500));
    }

    // ---- abilities: decision 2026-11-14-damage-modifiers ----

    @Test
    void scudoAgainstPerforanteLeavesPartOfTheReduction() {
        // reduction 250; Perforante 30 ignores floor(250 * 30 / 100) = 75; 1000 - 175 = 825
        assertEquals(825, Damage.mitigationPermille(List.of(25), 30, 700));
        assertEquals(750, Damage.mitigationPermille(List.of(25), 0, 700));
        assertEquals(1000, Damage.mitigationPermille(List.of(), 30, 700));
        assertEquals(1000, Damage.mitigationPermille(List.of(25), 100, 700));
    }

    @Test
    void reductionsMultiplyAndAreCappedBeforePerforante() {
        // 1000 * 750 / 1000 = 750, then 750 * 800 / 1000 = 600: reduction 400, capped at 300
        assertEquals(700, Damage.mitigationPermille(List.of(25, 20), 0, 700));
        // Perforante works on the capped 300: 300 - 90 = 210
        assertEquals(790, Damage.mitigationPermille(List.of(25, 20), 30, 700));
        // each step floored: 1000 * 970 / 1000 = 970, 970 * 970 / 1000 = 940.9 -> 940
        assertEquals(940, Damage.mitigationPermille(List.of(3, 3), 0, 700));
    }

    @Test
    void piercingIsFloored() {
        assertEquals(700, Damage.pierce(1000, 30));
        assertEquals(175, Damage.pierce(250, 30));
        assertEquals(1, Damage.pierce(1, 30)); // 1 - floor(0.3)
        assertEquals(0, Damage.pierce(0, 50));
    }

    @Test
    void damageIsFlatThenPercentageThenTheMinimum() {
        assertEquals(4000, Damage.damage(4000, 0, 1000, 0));
        assertEquals(3300, Damage.damage(4000, 0, 825, 0));
        assertEquals(3200, Damage.damage(4000, 800, 1000, 1));
        assertEquals(2240, Damage.damage(4000, 800, 700, 1)); // (4000 - 800) * 0.7
        assertEquals(1, Damage.damage(500, 800, 1000, 1), "a flat reduction leaves at least min_damage");
        assertEquals(1, Damage.damage(1, 0, 700, 1), "floor(0.7) = 0, raised to min_damage 1");
        assertEquals(0, Damage.damage(1, 0, 700, 0), "no flat reduction, no minimum");
        assertEquals(0, Damage.damage(0, 800, 1000, 1), "no damage from an effective ATK of 0");
        assertEquals(3999, Damage.damage(3999, 0, 1000, 0));
    }

    @Test
    void spillFactorsMultiplyTheHandMultiplier() {
        assertEquals(1100, Damage.spillMultiplier(1100, List.of()));
        assertEquals(1320, Damage.spillMultiplier(1100, List.of(1200)));
        assertEquals(1584, Damage.spillMultiplier(1100, List.of(1200, 1200))); // 1320 * 1.2
        assertEquals(1, Damage.spillMultiplier(1, List.of(1999)), "floored: 1.999");
    }
}
