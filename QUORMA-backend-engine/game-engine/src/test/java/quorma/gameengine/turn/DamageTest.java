package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
