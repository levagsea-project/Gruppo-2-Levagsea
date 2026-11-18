package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;

/** E13: puts the creature in its owner's discard pile, with no destruction (no T10, no T20). */
public final class DiscardEffect implements Effect {

    @Override
    public String id() {
        return "E13";
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
        ctx.discard(use.targetSide(), target, use.after());
        return Outcome.NONE;
    }
}
