package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.range;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E47 (T25: keeps {@code lane} (or every empty lane) of {@code player} empty in this Refill.) Read by {@code Refill}; it has no resolution of its own.
 */
public final class PreventRefillEffect implements EffectDefinition {

    public static final String ID = "E47";

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
        return Map.of("", ParamRule.none().optInts(Map.of("lane", range(1, 5)))
                .optStrings(Map.of("player", Set.of("controllore", "avversario"))));
    }
}
