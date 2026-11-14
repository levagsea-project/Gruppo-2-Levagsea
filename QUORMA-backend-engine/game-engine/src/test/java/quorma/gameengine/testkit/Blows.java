package quorma.gameengine.testkit;

import java.util.List;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.model.Attack;

/** Test helper: the {@code AttackResolved} of a blow between creatures with no abilities. */
public final class Blows {

    private Blows() {}

    /**
     * No reductions (mitigation 1000, damage = effective ATK), the spill multiplier is the hand multiplier, no
     * modifiers.
     */
    public static AttackResolved plain(Attack attack, int cycle, int atk, int advantagePermille, int effectiveAtk,
            int defBefore, int defAfter, boolean killed, int spill, boolean mutualKill, int handMultiplierPermille) {
        return new AttackResolved(attack, cycle, atk, advantagePermille, effectiveAtk, 1000, effectiveAtk, defBefore,
                defAfter, killed, mutualKill, handMultiplierPermille, handMultiplierPermille, spill, List.of());
    }
}
