package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E51 (T23: multiplies the healing the target is about to receive by {@code permille} (1000: unchanged, 0: prevented).) Read by {@code Board.healPermille}; it has no resolution of its own.
 */
public final class HealFactorEffect implements EffectDefinition {

    public static final String ID = "E51";

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
        return Map.of("", ParamRule.ints(Map.of("permille", NON_NEGATIVE)));
    }
}
