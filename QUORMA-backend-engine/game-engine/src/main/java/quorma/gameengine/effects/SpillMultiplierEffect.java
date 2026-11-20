package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E21 (T34: multiplies by {@code factor} (per mille) the hand multiplier a spill uses.) Read by {@code Strike}; it has no resolution of its own.
 */
public final class SpillMultiplierEffect implements EffectDefinition {

    public static final String ID = "E21";

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
        return Map.of("", ParamRule.ints(Map.of("factor", NON_NEGATIVE)));
    }
}
