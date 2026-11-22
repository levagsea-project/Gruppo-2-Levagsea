package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E61 (Positive ATK and DEF modifiers do not apply to the creature (the mirror of E52).) Read by {@code Board.noBonus}; it has no resolution of its own.
 */
public final class NoBonusEffect implements EffectDefinition {

    public static final String ID = "E61";

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
