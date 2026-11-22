package quorma.gameengine.effects;

/**
 * Vocabulary IDs of the targets the engine compares by name. The IDs themselves never change (data files keep them);
 * these names say what they mean where the code tests for them.
 */
public final class TargetIds {

    /** S1: the creature whose ability it is. */
    public static final String SELF = "S1";

    /** S4: the lane of the creature whose ability it is. */
    public static final String LANE = "S4";

    /** S9: the creatures of the ability's owner. */
    public static final String OWN_CREATURES = "S9";

    /** S10: the creatures of the other player. */
    public static final String ENEMY_CREATURES = "S10";

    /** S11: every creature in play. */
    public static final String ALL_CREATURES = "S11";

    /** S12: the creatures of a suit. */
    public static final String SUIT_CREATURES = "S12";

    /** S27: the creature the trigger placed (a replacement, or an equipment's receiver). */
    public static final String REPLACEMENT = "S27";

    /** S30: the other player. */
    public static final String OPPONENT_PLAYER = "S30";

    /** S31: the ability's owner, as a player. */
    public static final String OWN_PLAYER = "S31";

    /** S32: the owner's discard pile. */
    public static final String DISCARD_PILE = "S32";

    /** S39: the creature with the highest rank. */
    public static final String HIGHEST_RANK = "S39";

    /** S47: the top cards of the owner's deck. */
    public static final String DECK_TOP = "S47";

    private TargetIds() {}
}
