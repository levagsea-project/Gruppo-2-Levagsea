package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.range;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E54 (For straights, the creature may count as its rank plus or minus {@code step} (default 1).) Read by {@code Board.rankStep} and {@code Hands}; it has no resolution of its own.
 */
public final class RankStepEffect implements EffectDefinition {

    public static final String ID = "E54";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.C);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none().optInts(Map.of("step", range(1, 12))));
    }
}
