package quorma.gameengine.effects;

import java.util.OptionalInt;
import quorma.gameengine.model.Creature;

/** E58: damage by an effect, a share ({@code permille}) of the source's ATK or of the amount that triggered it. */
public final class ScaledDamageEffect implements Effect {

    @Override
    public String id() {
        return "E58";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        int base = switch (e.stringParam("source").orElseThrow()) {
            case "own_atk" -> use.source().currentAtk();
            default -> use.context().amount();
        };
        int amount = PerMille.floor(base, e.intParam("permille"));
        return new Outcome(ctx.damage(use.ownerSide(), use.source(), use.targetSide(), target, amount, use.after()),
                OptionalInt.empty());
    }
}
