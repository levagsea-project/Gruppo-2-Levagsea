package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;
import static quorma.gameengine.effects.ParamRule.range;

import java.util.List;
import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;
import quorma.gameengine.model.Lasting;

/** E28: grants the creature a static copy of the effect (aimed at itself), up to {@code max_stacks} times. */
public final class StackedGrantEffect implements Effect {

    public static final String ID = "E28";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.C, ResClass.T);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of(
                "reduction_pct", ParamRule.ints(Map.of("pct", range(0, 100))),
                "flat", ParamRule.ints(Map.of("amount", NON_NEGATIVE, "min_damage", NON_NEGATIVE))
                        .defaults(Map.of("min_damage", 1)));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        return Stacking.apply(e, target, () -> {
            target.grant(new Grant(e.abilityId(), List.of(e.asStatic(TargetIds.SELF, Lasting.PERMANENT)), use.lasting(), false));
            return List.of();
        });
    }
}
