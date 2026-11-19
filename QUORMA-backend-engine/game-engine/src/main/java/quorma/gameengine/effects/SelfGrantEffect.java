package quorma.gameengine.effects;

import java.util.List;
import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;
import quorma.gameengine.model.Lasting;

/** E18: grants the creature a static copy of the effect (aimed at itself), once. See E28 for the stacking one. */
public final class SelfGrantEffect implements Effect {

    public static final String ID = "E18";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.C, ResClass.T);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none());
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        target.grant(new Grant(e.abilityId(), List.of(e.asStatic(TargetIds.SELF, Lasting.PERMANENT)), use.lasting(), false));
        return Outcome.NONE;
    }
}
