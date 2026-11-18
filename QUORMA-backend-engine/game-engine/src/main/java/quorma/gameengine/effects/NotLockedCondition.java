package quorma.gameengine.effects;

import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C24: the lockout named by {@code key} is not active (Custode's delayed destruction is data). */
public final class NotLockedCondition implements Condition {

    @Override
    public String id() {
        return "C24";
    }

    @Override
    public Optional<String> validate(ConditionDef def) {
        if (!(def.params().get("key") instanceof Value.StrV k && !k.v().isEmpty())) {
            return Optional.of("condition C24 needs a text param 'key'");
        }
        return Optional.empty();
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return !ctx.locked(side, ((Value.StrV) def.params().get("key")).v());
    }
}
