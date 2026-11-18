package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S2: the creature that caused the trigger. */
public final class TriggererTarget implements Target {

    @Override
    public String id() {
        return "S2";
    }

    @Override
    public boolean single() {
        return true;
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        TargetSupport.addLiving(out, ctx.triggerer());
        return out;
    }
}
