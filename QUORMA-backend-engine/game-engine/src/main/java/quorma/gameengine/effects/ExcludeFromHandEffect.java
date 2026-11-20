package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E63 (T4: the creature does not count for its player's hand; it stays in play and fights.) Read by {@code Hands.excluded}; it has no resolution of its own.
 */
public final class ExcludeFromHandEffect implements EffectDefinition {

    public static final String ID = "E63";

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
