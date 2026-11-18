package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;

/** E10: destroys the creature (not damage), unless a T20 replacement saves it. */
public final class DestroyEffect implements Effect {

    @Override
    public String id() {
        return "E10";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        ctx.destroy(use.ownerSide(), use.source(), use.targetSide(), target, use.after());
        return Outcome.NONE;
    }
}
