package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E19 (Multiplies by {@code factor} (per mille) the numbers of the Evolution effects of the creature it reaches (Spira 4).) Read by {@code TriggerDispatcher.scaleOf}; it has no resolution of its own.
 */
public final class EvolutionFactorEffect implements EffectDefinition {

    public static final String ID = "E19";

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
        return Map.of("", ParamRule.ints(Map.of("factor", NON_NEGATIVE)));
    }
}
