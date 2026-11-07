package quorma.gameengine.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.RulesetLoader;

/** Expected values are copied from the table in ruleset, not from the engine. */
class SpillMultiplierTest {

    private static final Ruleset RULES = RulesetLoader.fromClasspath();

    private static final Map<HandType, Integer> RULESET = Map.ofEntries(
            Map.entry(HandType.HIGH_CARD, 1000),
            Map.entry(HandType.PAIR, 1100),
            Map.entry(HandType.TWO_PAIR, 1300),
            Map.entry(HandType.FOUR_OF_A_SUIT, 1350),
            Map.entry(HandType.THREE_OF_A_KIND, 1400),
            Map.entry(HandType.STRAIGHT, 1600),
            Map.entry(HandType.FLUSH, 1700),
            Map.entry(HandType.FULL_HOUSE, 1800),
            Map.entry(HandType.FOUR_OF_A_KIND, 2200),
            Map.entry(HandType.STRAIGHT_FLUSH, 2600),
            Map.entry(HandType.ROYAL_FLUSH, 3000),
            Map.entry(HandType.FIVE_OF_A_KIND, 3500));

    @Test
    @Disabled
    void everyHandMatchesRuleset() {
        for (Map.Entry<HandType, Integer> e : RULESET.entrySet()) {
            assertEquals(e.getValue(), HandEvaluator.spillMultiplierPermille(RULES, e.getKey()), e.getKey().name());
        }
    }

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
