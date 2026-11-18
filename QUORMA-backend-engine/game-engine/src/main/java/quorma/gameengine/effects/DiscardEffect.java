package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;

/** E13: puts the creature in its owner's discard pile, with no destruction (no T10, no T20). */
public final class DiscardEffect implements Effect {

    @Override
    public String id() {
        return "E13";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        ctx.discard(use.targetSide(), target, use.after());
        return Outcome.NONE;
    }
}
