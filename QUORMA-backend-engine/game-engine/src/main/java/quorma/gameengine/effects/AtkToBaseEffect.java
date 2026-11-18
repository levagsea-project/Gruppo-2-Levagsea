package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;

/** E4: the creature's ATK goes back to its base value (it replaces the ATK bonuses while it lasts). */
public final class AtkToBaseEffect implements Effect {

    @Override
    public String id() {
        return "E4";
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.T);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none().optInts(Map.of("value", NON_NEGATIVE))
                .optStrings(Map.of("value", Set.of("base"))));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        target.setAtkToBase(use.expansion().abilityId(), use.lasting());
        return Outcome.NONE;
    }
}
