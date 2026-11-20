package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S7: the adjacent ally in the lowest lane. */
public final class NextAdjacentTarget implements Target {

    @Override
    public String id() {
        return "S7";
    }

    @Override
    public boolean single() {
        return true;
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        view.creatures(side).stream()
                .filter(c -> c != source && !c.destroyed() && view.adjacent(side, source, c))
                .min(Comparator.comparingInt(Creature::lane)).ifPresent(out::add);
        return out;
    }
}
