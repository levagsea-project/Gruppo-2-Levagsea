package quorma.gameengine.effects;

import java.util.Comparator;
import quorma.gameengine.model.Creature;

/** S33: the creature in the lowest lane. */
public final class LowestLaneTarget implements RankedTarget {

    @Override
    public String id() {
        return "S33";
    }

    @Override
    public Comparator<Creature> order(TargetView view) {
        return RankedTarget.byLane().thenComparing(RankedTarget.byFaction(view));
    }
}
