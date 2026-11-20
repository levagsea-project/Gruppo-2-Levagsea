package quorma.gameengine.effects;

import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C4: the player has at least {@code level} creatures of {@code suit} (counted at Valutazione). */
public final class SuitCountCondition implements Condition {

    @Override
    public String id() {
        return "C4";
    }

    @Override
    public Optional<String> validate(ConditionDef def) {
        Optional<String> suit = ConditionParams.suitProblem(def);
        if (suit.isPresent()) {
            return suit;
        }
        if (!(def.params().get("level") instanceof Value.IntV n && n.v() >= 1 && n.v() <= 5)) {
            return Optional.of("condition C4 needs an integer param 'level' between 1 and 5");
        }
        return Optional.empty();
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return ctx.suitCount(side, ConditionParams.suit(def)) >= ((Value.IntV) def.params().get("level")).v();
    }
}
