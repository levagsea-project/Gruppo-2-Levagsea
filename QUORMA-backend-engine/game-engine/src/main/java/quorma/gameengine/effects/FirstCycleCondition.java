package quorma.gameengine.effects;

import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C6: it is the first cycle of combat. */
public final class FirstCycleCondition implements Condition {

    @Override
    public String id() {
        return "C6";
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return ctx.firstCycle();
    }
}
