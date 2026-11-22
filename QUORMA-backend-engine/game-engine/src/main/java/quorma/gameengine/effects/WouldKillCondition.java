package quorma.gameengine.effects;

import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C21: the blow being evaluated would destroy its target. */
public final class WouldKillCondition implements Condition {

    @Override
    public String id() {
        return "C21";
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return ctx.wouldKill();
    }
}
