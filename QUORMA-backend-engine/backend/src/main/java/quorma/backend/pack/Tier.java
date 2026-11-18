package quorma.backend.pack;

/**
 * Rarity of a creature card. It follows from the rank (ruleset 3.2.2): 2-6 Tier 1, 7-10 Tier 2, Jack/Queen/King
 * Tier 3, Ace Tier 4. Ranks in the database are 1..13 (Ace = 1, Jack = 11, Queen = 12, King = 13).
 */
public enum Tier {
    TIER_1,
    TIER_2,
    TIER_3,
    TIER_4;

    public static Tier ofRank(short rank) {
        if (rank == 1) {
            return TIER_4;
        }
        if (rank >= 11) {
            return TIER_3;
        }
        return rank >= 7 ? TIER_2 : TIER_1;
    }
}
