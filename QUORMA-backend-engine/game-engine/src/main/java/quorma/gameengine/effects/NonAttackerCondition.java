package quorma.gameengine.effects;

import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C2: the creature does not attack this turn. */
public final class NonAttackerCondition implements Condition {

    @Override
    public String id() {
        return "C2";
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return source != null && !ctx.attacker(side, source);
    }
}
