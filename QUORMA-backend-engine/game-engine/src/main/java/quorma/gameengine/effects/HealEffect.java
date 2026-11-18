package quorma.gameengine.effects;

import java.util.List;
import java.util.OptionalInt;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/** E6: heal a flat amount or a share of the base DEF, times the E51 factor of the target. */
public final class HealEffect implements Effect {

    @Override
    public String id() {
        return "E6";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        int before = target.currentDef();
        int amount = "flat".equals(e.stringParam("mode").orElse("flat")) ? e.intParam("amount")
                : Math.toIntExact(Math.floorDiv((long) target.baseDef() * e.intParam("permille"), 1000L));
        int factor = ctx.healPermille(target);
        target.heal(Math.toIntExact((long) amount * factor / 1000));
        return new Outcome(List.of(new StatChange(StatChange.Stat.DEF, before, target.currentDef())),
                OptionalInt.empty());
    }
}
