package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;

/** E58: damage by an effect, a share ({@code permille}) of the source's ATK or of the amount that triggered it. */
public final class ScaledDamageEffect implements Effect {

    @Override
    public String id() {
        return "E58";
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.T);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.ints(Map.of("permille", NON_NEGATIVE))
                .strings(Map.of("source", DamageSource.ids())));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        int base = switch (DamageSource.of(e.stringParam("source").orElseThrow())) {
            case OWN_ATK -> use.source().currentAtk();
            case DAMAGE_TAKEN, DAMAGE_DEALT -> use.context().amount();
        };
        int amount = PerMille.floor(base, e.intParam("permille"));
        return new Outcome(ctx.damage(use.ownerSide(), use.source(), use.targetSide(), target, amount, use.after()),
                OptionalInt.empty());
    }
}
