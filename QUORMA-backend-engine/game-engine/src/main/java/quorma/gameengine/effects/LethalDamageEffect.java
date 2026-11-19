package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E26 (Any combat damage the source deals (above 0) destroys the creature, whatever its DEF.) Read by {@code Board.lethal}; it has no resolution of its own.
 */
public final class LethalDamageEffect implements EffectDefinition {

    public static final String ID = "E26";

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
