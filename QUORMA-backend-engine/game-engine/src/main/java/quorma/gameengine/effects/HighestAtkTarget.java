package quorma.gameengine.effects;

import java.util.Comparator;
import quorma.gameengine.model.Creature;

/** S35: the creature with the highest ATK. */
public final class HighestAtkTarget implements RankedTarget {

    @Override
    public String id() {
        return "S35";
    }

    @Override
    public Comparator<Creature> order(TargetView view) {
        return Comparator.comparingInt((Creature c) -> -c.currentAtk()).thenComparing(RankedTarget.byLane())
                .thenComparing(RankedTarget.byFaction(view)).thenComparing(RankedTarget.rankHigh())
                .thenComparing(RankedTarget.bySuit());
    }
}
