package quorma.gameengine.effects;

import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C11: a living creature of {@code suit} is adjacent to the source. */
public final class AdjacentSuitCondition implements Condition {

    @Override
    public String id() {
        return "C11";
    }

    @Override
    public Optional<String> validate(ConditionDef def) {
        return ConditionParams.suitProblem(def);
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return source != null && ctx.hasAdjacentOfSuit(side, source, ConditionParams.suit(def));
    }
}
