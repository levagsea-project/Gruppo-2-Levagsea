package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E55 (For flushes and four of a suit, the creature counts as any suit.) Read by {@code Board.wildSuit} and {@code Hands}; it has no resolution of its own.
 */
public final class WildSuitEffect implements EffectDefinition {

    public static final String ID = "E55";

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
