package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S43: the creature an attack was declared on. */
public final class DeclaredTargetTarget implements Target {

    @Override
    public String id() {
        return "S43";
    }

    @Override
    public boolean single() {
        return true;
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        TargetSupport.addLiving(out, ctx.declaredTarget());
        return out;
    }
}
