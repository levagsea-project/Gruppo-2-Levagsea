package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E52 (The creature's ATK cannot be lowered by effects: negative modifiers are ignored.) Read by {@code Board.noReduce}; it has no resolution of its own.
 */
public final class NoAtkReductionEffect implements EffectDefinition {

    public static final String ID = "E52";

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
