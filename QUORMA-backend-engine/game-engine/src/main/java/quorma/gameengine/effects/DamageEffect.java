package quorma.gameengine.effects;

import java.util.OptionalInt;
import quorma.gameengine.model.Creature;

/** E7: damage by an effect, {@code amount} flat. */
public final class DamageEffect implements Effect {

    @Override
    public String id() {
        return "E7";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        return new Outcome(ctx.damage(use.ownerSide(), use.source(), use.targetSide(), target,
                use.expansion().intParam("amount"), use.after()), OptionalInt.empty());
    }
}
