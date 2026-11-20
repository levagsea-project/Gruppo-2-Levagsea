package quorma.gameengine.effects;

import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C1: the creature attacks this turn. */
public final class AttackerCondition implements Condition {

    @Override
    public String id() {
        return "C1";
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return source != null && ctx.attacker(side, source);
    }
}
