package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E25 (The attacker does not make its attack.) Read by {@code Combat.beforeAttack}; it has no resolution of its own.
 */
public final class PreventAttackEffect implements EffectDefinition {

    public static final String ID = "E25";

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
