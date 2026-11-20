package quorma.gameengine.effects;

/** S4: the lane of the creature whose ability it is (a pending effect for the next creature there). */
public final class LaneTarget implements Target {

    @Override
    public String id() {
        return TargetIds.LANE;
    }
}
