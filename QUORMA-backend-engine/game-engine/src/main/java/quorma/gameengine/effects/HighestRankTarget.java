package quorma.gameengine.effects;

import java.util.Comparator;
import quorma.gameengine.model.Creature;

/** S39: the creature with the highest rank. */
public final class HighestRankTarget implements RankedTarget {

    @Override
    public String id() {
        return TargetIds.HIGHEST_RANK;
    }

    @Override
    public Comparator<Creature> order(TargetView view) {
        return RankedTarget.rankHigh().thenComparing(RankedTarget.byLane())
                .thenComparing(RankedTarget.byFaction(view)).thenComparing(RankedTarget.bySuit());
    }
}
