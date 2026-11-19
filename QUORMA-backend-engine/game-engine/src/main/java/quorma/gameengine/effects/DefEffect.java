package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.ANY;

import java.util.List;
import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/**
 * E2: DEF + {@code amount} or a share of the base DEF, times {@code scale_by} when the effect has it. A bonus does not
 * apply to a creature reached by E61. A malus that brings DEF to 0 or less destroys the creature (by effect).
 */
public final class DefEffect implements Effect {

    public static final String ID = "E2";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.C, ResClass.T, ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of(
                "pct_base_def", ParamRule.ints(Map.of("permille", ANY))
                        .optStrings(Map.of("scale_by", Set.of("adjacent_allies"))),
                "flat", ParamRule.ints(Map.of("amount", ANY)).optStrings(Map.of("scale_by", Set.of("adjacent_allies"))));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        return Stacking.apply(e, target, () -> {
            int scale = e.stringParam("scale_by").isPresent() && use.source() != null
                    ? ctx.adjacentAllyCount(use.ownerSide(), use.source()) : 1;
            int before = target.currentDef();
            int amount = Math.multiplyExact(PerMille.baseAmount(e, target.baseDef()), scale);
            if (amount > 0 && ctx.noBonus(target)) {
                return List.of(new StatChange(StatChange.Stat.DEF, before, before));
            }
            if (amount < 0 && before + amount <= 0 && ctx.saveFromDestruction(use.targetSide(), target, use.after())) {
                return List.of(new StatChange(StatChange.Stat.DEF, before, target.currentDef()));
            }
            target.addDef(amount, use.lasting());
            if (target.destroyed()) {
                ctx.destroyed(use.targetSide(), target, use.ownerSide(), use.source(), use.after());
            }
            return List.of(new StatChange(StatChange.Stat.DEF, before, target.currentDef()));
        });
    }
}
