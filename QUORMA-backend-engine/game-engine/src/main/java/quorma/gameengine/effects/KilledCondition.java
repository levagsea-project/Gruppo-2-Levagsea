package quorma.gameengine.effects;

import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C27: the damage that triggered the ability destroyed its target (T38). */
public final class KilledCondition implements Condition {

    @Override
    public String id() {
        return "C27";
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return ctx.trigger().killed();
    }
}
