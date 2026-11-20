package quorma.gameengine.effects;

import java.util.Comparator;
import quorma.gameengine.model.Creature;

/** S40: the creature with the lowest rank. */
public final class LowestRankTarget implements RankedTarget {

    @Override
    public String id() {
        return "S40";
    }

    @Override
    public Comparator<Creature> order(TargetView view) {
        return RankedTarget.rankLow().thenComparing(RankedTarget.byLane())
                .thenComparing(RankedTarget.byFaction(view)).thenComparing(RankedTarget.bySuit());
    }
}
