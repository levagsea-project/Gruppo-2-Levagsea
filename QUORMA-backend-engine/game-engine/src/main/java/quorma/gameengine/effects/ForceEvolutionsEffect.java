package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E17 (Fires the Evolutions of the player's creatures of a suit now, once, whatever their own trigger (Spira 5).) Read by {@code TurnResolver.forceEvolutions}; the stack never resolves it; it has no resolution of its own.
 */
public final class ForceEvolutionsEffect implements EffectDefinition {

    public static final String ID = "E17";

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
        return Map.of("", ParamRule.none());
    }
}
