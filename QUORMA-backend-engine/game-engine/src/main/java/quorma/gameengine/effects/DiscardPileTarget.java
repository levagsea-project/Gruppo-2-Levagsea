package quorma.gameengine.effects;

/** S32: the owner's discard pile. */
public final class DiscardPileTarget implements Target {

    @Override
    public String id() {
        return TargetIds.DISCARD_PILE;
    }
}
