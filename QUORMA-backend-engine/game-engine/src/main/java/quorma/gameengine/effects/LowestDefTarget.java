package quorma.gameengine.effects;

import java.util.Comparator;
import quorma.gameengine.model.Creature;

/** S38: the creature with the lowest DEF. */
public final class LowestDefTarget implements RankedTarget {

    @Override
    public String id() {
        return "S38";
    }

    @Override
    public Comparator<Creature> order(TargetView view) {
        return Comparator.comparingInt(Creature::currentDef).thenComparing(RankedTarget.byLane())
                .thenComparing(RankedTarget.byFaction(view)).thenComparing(RankedTarget.rankLow())
                .thenComparing(RankedTarget.bySuit());
    }
}
