package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E24 (Redirects the attack to {@code new_target_id} (S7: the adjacent creature in the lowest lane; S1: itself, Provocazione).) Read by {@code Declaration}; the stack never resolves it; it has no resolution of its own.
 */
public final class RedirectAttackEffect implements EffectDefinition {

    public static final String ID = "E24";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.T);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none().strings(Map.of("new_target_id", Set.of("S1", "S7"))));
    }
}
