package quorma.gameengine.effects;

import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C28: the creature destroyed (T10, T36, T37) was adjacent to the source when it was destroyed. */
public final class DestroyedAdjacentCondition implements Condition {

    @Override
    public String id() {
        return "C28";
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        TriggerContext trigger = ctx.trigger();
        return source != null && trigger.destroyed() != null
                && ctx.wasAdjacent(side, source, trigger.destroyed(), trigger.destroyedLane());
    }
}
