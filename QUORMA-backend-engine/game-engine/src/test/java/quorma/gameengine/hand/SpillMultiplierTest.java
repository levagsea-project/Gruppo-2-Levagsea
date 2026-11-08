package quorma.gameengine.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.RulesetLoader;

/**
 * Spill multipliers are tuned in {@code balance.json#hand_multipliers_permille}. {@code balance.json}
 * is the single source of truth for these numbers, so this test does not duplicate them. It only
 * guards the contract around them: {@link HandType#NONE} deals no spill, and every hand type has a
 * balance.json entry (a new HandType without an entry fails here, not in a match).
 */
class SpillMultiplierTest {

    private static final Ruleset RULES = RulesetLoader.fromClasspath();

    @Test
    void noHandDealsNoSpill() {
        assertEquals(0, HandEvaluator.spillMultiplierPermille(RULES, HandType.NONE));
    }

    @Test
    void everyHandTypeHasAnEntryInBalanceJson() {
        // a new HandType without a balance.json entry must fail here, not in a match
        for (HandType t : HandType.values()) {
            HandEvaluator.spillMultiplierPermille(RULES, t);
        }
    }
}
