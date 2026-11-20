package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.range;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E22 (T34: for the spill, the target's DEF before the killing blow counts as {@code permille} of its value.) Read by {@code Strike}; it has no resolution of its own.
 */
public final class SpillDefShareEffect implements EffectDefinition {

    public static final String ID = "E22";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.R);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.ints(Map.of("permille", range(0, 1000))));
    }
}
