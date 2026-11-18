package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.ANY;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/** E3: the creature's base DEF changes by {@code amount} for good (an equipment). Immediate effects only. */
public final class BaseDefEffect implements Effect {

    @Override
    public String id() {
        return "E3";
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.ints(Map.of("amount", ANY)));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        target.addBaseDef(use.expansion().intParam("amount"));
        return new Outcome(List.of(new StatChange(StatChange.Stat.DEF, target.currentDef(), target.currentDef())),
                OptionalInt.empty());
    }
}
