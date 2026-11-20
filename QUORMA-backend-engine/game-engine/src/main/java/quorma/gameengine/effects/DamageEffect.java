package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;

/** E7: damage by an effect, {@code amount} flat. */
public final class DamageEffect implements Effect {

    public static final String ID = "E7";

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
        return Map.of("", ParamRule.ints(Map.of("amount", NON_NEGATIVE)));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        return new Outcome(ctx.damage(use.ownerSide(), use.source(), use.targetSide(), target,
                use.expansion().intParam("amount"), use.after()), OptionalInt.empty());
    }
}
