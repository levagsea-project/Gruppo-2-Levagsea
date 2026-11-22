package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E29 (T21: prevents {@code amount} damage, or all of the next damage with {@code once}.) Read by {@code Destruction.preventDamage}; it has no resolution of its own.
 */
public final class PreventDamageEffect implements EffectDefinition {

    public static final String ID = "E29";

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
        return Map.of("", ParamRule.none().optInts(Map.of("amount", NON_NEGATIVE)).bools(Set.of("once")));
    }
}
