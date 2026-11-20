package quorma.gameengine.effects;

import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C22: at the start of the cycle the blow's target had more DEF than the source had ATK (ruleset 7.10.6). */
public final class TargetDefAboveAtkCondition implements Condition {

    @Override
    public String id() {
        return "C22";
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return source != null && ctx.targetDefAboveAtkAtCycleStart(source);
    }
}
