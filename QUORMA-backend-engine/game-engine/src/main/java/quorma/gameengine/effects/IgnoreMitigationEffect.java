package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.range;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E56 (The combat damage the source deals ignores {@code pct}% of the target's damage reductions (E28); several add up, at most 100.) Read by {@code Strike}; it has no resolution of its own.
 */
public final class IgnoreMitigationEffect implements EffectDefinition {

    public static final String ID = "E56";

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
        return Map.of("", ParamRule.ints(Map.of("pct", range(0, 100))));
    }
}
