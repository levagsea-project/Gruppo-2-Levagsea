package quorma.gameengine.effects;

import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;
import quorma.gameengine.model.Lasting;

/** E18: grants the creature a static copy of the effect (aimed at itself), once. See E28 for the stacking one. */
public final class SelfGrantEffect implements Effect {

    @Override
    public String id() {
        return "E18";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        target.grant(new Grant(e.abilityId(), List.of(e.asStatic("S1", Lasting.PERMANENT)), use.lasting(), false));
        return Outcome.NONE;
    }
}
