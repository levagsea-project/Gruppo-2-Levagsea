package quorma.gameengine.effects;

/**
 * Vocabulary IDs of the targets the engine compares by name. The IDs themselves never change (data files keep them);
 * these names say what they mean where the code tests for them.
 */
public final class TargetIds {

    /** S1: the creature whose ability it is. */
    public static final String SELF = "S1";

    /** S12: the creatures of a suit. */
    public static final String SUIT_CREATURES = "S12";

    /** S27: the creature the trigger placed (a replacement, or an equipment's receiver). */
    public static final String REPLACEMENT = "S27";

    private TargetIds() {}
}
