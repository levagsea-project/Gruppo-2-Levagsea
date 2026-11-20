package quorma.gameengine.effects;

import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C26: the number of living allies adjacent to the source is between {@code min} (default 0) and {@code max} (default 2). */
public final class AdjacentAlliesCondition implements Condition {

    @Override
    public String id() {
        return "C26";
    }

    @Override
    public Optional<String> validate(ConditionDef def) {
        for (String name : def.params().keySet()) {
            if (!(name.equals("min") || name.equals("max"))
                    || !(def.params().get(name) instanceof Value.IntV n) || n.v() < 0 || n.v() > 2) {
                return Optional.of("condition C26 takes integer params 'min' and 'max' between 0 and 2");
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        if (source == null) {
            return false;
        }
        long min = def.params().get("min") instanceof Value.IntV v ? v.v() : 0;
        long max = def.params().get("max") instanceof Value.IntV v ? v.v() : 2;
        int n = ctx.adjacentAllyCount(side, source);
        return n >= min && n <= max;
    }
}
