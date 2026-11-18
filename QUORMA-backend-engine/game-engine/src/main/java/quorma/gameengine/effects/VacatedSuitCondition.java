package quorma.gameengine.effects;

import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C25: the creature that left the source's lane was of {@code suit}. */
public final class VacatedSuitCondition implements Condition {

    @Override
    public String id() {
        return "C25";
    }

    @Override
    public Optional<String> validate(ConditionDef def) {
        return ConditionParams.suitProblem(def);
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return source != null && ctx.vacatedSuit(side, source.lane(), ConditionParams.suit(def));
    }
}
