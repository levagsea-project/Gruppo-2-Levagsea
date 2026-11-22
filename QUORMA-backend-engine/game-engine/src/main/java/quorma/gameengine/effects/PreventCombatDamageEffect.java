package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E23 (Prevents the combat damage the target would deal to the source (Primo Colpo in Cycle 1).) Read by {@code Combat.beforeAttack} and {@code Strike}; it has no resolution of its own.
 */
public final class PreventCombatDamageEffect implements EffectDefinition {

    public static final String ID = "E23";

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
        return Map.of("", ParamRule.none());
    }
}
