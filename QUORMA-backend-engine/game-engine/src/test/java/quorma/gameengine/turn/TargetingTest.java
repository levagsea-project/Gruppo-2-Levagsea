package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;

/**
 * Lane targeting, ruleset 8.1: the opposite lane first, then a scan upward from the attacker's own lane number,
 * wrapping from lane 5 to lane 1 (lane 4 checks 4, 5, 1, 2, 3). Expected lanes are worked out by hand.
 */
class TargetingTest {

    /** A targetable-lanes mask with the given lanes (1..5) set. */
    private static boolean[] lanes(int... targetable) {
        boolean[] mask = new boolean[5];
        for (int lane : targetable) {
            mask[lane - 1] = true;
        }
        return mask;
    }

    private static int target(int attackerLane, boolean[] mask) {
        return Targeting.targetLane(attackerLane, mask).orElseThrow();
    }

    @Test
    void aTargetableOppositeLaneIsAlwaysChosen() {
        assertEquals(3, target(3, lanes(3)));
        assertEquals(3, target(3, lanes(1, 2, 3, 4, 5)));
        assertEquals(1, target(1, lanes(1, 5)));
        assertEquals(5, target(5, lanes(1, 5)));
    }

    @Test
    void anEmptyOppositeLaneScansUpward() {
        assertEquals(4, target(2, lanes(4)));
        assertEquals(3, target(2, lanes(3, 4)));
        assertEquals(3, target(2, lanes(5, 3)));
        assertEquals(3, target(2, lanes(1, 3)));
    }

    @Test
    void theScanWrapsFromLaneFiveToLaneOne() {
        assertEquals(1, target(5, lanes(1)));
        assertEquals(2, target(5, lanes(2, 3)));
        assertEquals(2, target(4, lanes(2)));
    }

    @Test
    void laneFourChecksFourFiveOneTwoThree() {
        // ruleset 8.1 example: the order is 4, 5, 1, 2, 3, so each of these finds the first lane in that order
        assertEquals(5, target(4, lanes(5, 1, 2, 3)));
        assertEquals(1, target(4, lanes(1, 2, 3)));
        assertEquals(2, target(4, lanes(2, 3)));
        assertEquals(3, target(4, lanes(3)));
    }

    @Test
    void laneOneChecksUpToLaneFive() {
        assertEquals(5, target(1, lanes(5)));
        assertEquals(2, target(1, lanes(2, 5)));
    }

    @Test
    void aSingleTargetableLaneIsFoundFromEveryAttackerLane() {
        for (int attacker = 1; attacker <= 5; attacker++) {
            for (int enemy = 1; enemy <= 5; enemy++) {
                assertEquals(enemy, target(attacker, lanes(enemy)), "attacker lane " + attacker);
            }
        }
    }

    @Test
    void noTargetableLaneGivesNoTarget() {
        for (int attacker = 1; attacker <= 5; attacker++) {
            assertEquals(OptionalInt.empty(), Targeting.targetLane(attacker, lanes()), "attacker lane " + attacker);
        }
    }

    @Test
    void invariantsHoldForEveryEnemyBoard() {
        for (int bits = 0; bits < 32; bits++) {
            boolean[] mask = new boolean[5];
            for (int i = 0; i < 5; i++) {
                mask[i] = (bits & (1 << i)) != 0;
            }
            for (int attacker = 1; attacker <= 5; attacker++) {
                OptionalInt result = Targeting.targetLane(attacker, mask);
                String where = "mask " + bits + ", attacker lane " + attacker;
                assertEquals(bits != 0, result.isPresent(), where);
                if (result.isPresent()) {
                    assertTrue(mask[result.getAsInt() - 1], where + ": the target lane must be targetable");
                    if (mask[attacker - 1]) {
                        assertEquals(attacker, result.getAsInt(), where + ": an occupied opposite lane always wins");
                    }
                }
            }
        }
    }

    @Test
    void theMaskIsNotChanged() {
        boolean[] mask = lanes(2, 4);
        Targeting.targetLane(3, mask);
        assertEquals(java.util.Arrays.toString(lanes(2, 4)), java.util.Arrays.toString(mask));
    }

    @Test
    void destroyedCreaturesAreNotTargets() {
        // lane 3 holds a destroyed creature, lane 5 a live one
        PlayerState enemy = Boards.side(Subfaction.SEKTAC, 12000, "3:9S(1000/1000)! 5:6V(1000/1000)");
        assertEquals(5, target(3, enemy.targetableLanes(false)));
        assertEquals(3, target(3, enemy.targetableLanes(true)), "unless an effect says destroyed creatures count");
    }

    @Test
    void anEnemyWithOnlyDestroyedCreaturesGivesNoTarget() {
        PlayerState enemy = Boards.side(Subfaction.SEKTAC, 12000, "2:9S(1000/1000)!");
        assertEquals(OptionalInt.empty(), Targeting.targetLane(2, enemy.targetableLanes(false)));
    }

    @Test
    void badInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Targeting.targetLane(0, lanes(1)));
        assertThrows(IllegalArgumentException.class, () -> Targeting.targetLane(6, lanes(1)));
        assertThrows(IllegalArgumentException.class, () -> Targeting.targetLane(1, new boolean[4]));
        assertThrows(IllegalArgumentException.class, () -> Targeting.targetLane(1, new boolean[6]));
    }
}
