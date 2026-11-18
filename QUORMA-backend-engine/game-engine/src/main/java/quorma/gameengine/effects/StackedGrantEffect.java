package quorma.gameengine.effects;

import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;
import quorma.gameengine.model.Lasting;

/** E28: grants the creature a static copy of the effect (aimed at itself), up to {@code max_stacks} times. */
public final class StackedGrantEffect implements Effect {

    @Override
    public String id() {
        return "E28";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        return Stacking.apply(e, target, () -> {
            target.grant(new Grant(e.abilityId(), List.of(e.asStatic("S1", Lasting.PERMANENT)), use.lasting(), false));
            return List.of();
        });
    }
}
