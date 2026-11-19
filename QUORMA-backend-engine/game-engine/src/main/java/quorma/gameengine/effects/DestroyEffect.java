package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;

/** E10: destroys the creature (not damage), unless a T20 replacement saves it. */
public final class DestroyEffect implements Effect {

    public static final String ID = "E10";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.T, ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none());
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        ctx.destroy(use.ownerSide(), use.source(), use.targetSide(), target, use.after());
        return Outcome.NONE;
    }
}
