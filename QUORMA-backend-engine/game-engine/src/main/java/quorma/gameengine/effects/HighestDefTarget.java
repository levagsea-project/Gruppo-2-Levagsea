package quorma.gameengine.effects;

import java.util.Comparator;
import quorma.gameengine.model.Creature;

/** S37: the creature with the highest DEF. */
public final class HighestDefTarget implements RankedTarget {

    @Override
    public String id() {
        return "S37";
    }

    @Override
    public Comparator<Creature> order(TargetView view) {
        return Comparator.comparingInt((Creature c) -> -c.currentDef()).thenComparing(RankedTarget.byLane())
                .thenComparing(RankedTarget.byFaction(view)).thenComparing(RankedTarget.rankHigh())
                .thenComparing(RankedTarget.bySuit());
    }
}
