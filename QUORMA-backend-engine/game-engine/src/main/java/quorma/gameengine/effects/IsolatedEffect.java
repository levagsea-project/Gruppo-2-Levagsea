package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E60 (The creature counts as adjacent to no creature of its player (the opposite of E34).) Read by {@code Board.isolated}; it has no resolution of its own.
 */
public final class IsolatedEffect implements EffectDefinition {

    public static final String ID = "E60";

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
        return Map.of("", ParamRule.none());
    }
}
