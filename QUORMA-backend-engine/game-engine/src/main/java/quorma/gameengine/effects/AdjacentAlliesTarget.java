package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S41: the allies adjacent to the source, after the filters. */
public final class AdjacentAlliesTarget implements Target {

    @Override
    public String id() {
        return "S41";
    }

    @Override
    public boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return source != null && side == abilitySide && receiver != source && view.adjacent(side, source, receiver);
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        for (Creature c : view.creatures(side)) {
            if (c != source && !c.destroyed() && view.adjacent(side, source, c)
                    && view.filtersHold(e.filters(), side, side, c)) {
                out.add(c);
            }
        }
        return out;
    }
}
