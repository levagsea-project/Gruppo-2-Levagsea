package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.range;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E11 (T20: prevents the next destruction of the target, {@code count} times, and sets its current DEF to {@code set_def}; the spill is kept only with {@code spill_on_prevent}.) Read by {@code Destruction.rescue} and {@code Combat}; it has no resolution of its own.
 */
public final class SetDefRescueEffect implements EffectDefinition {

    public static final String ID = "E11";

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
        return Map.of("", ParamRule.ints(Map.of("count", range(1, Integer.MAX_VALUE)))
                .optInts(Map.of("set_def", range(1, Integer.MAX_VALUE))).bools(Set.of("spill_on_prevent"))
                .defaults(Map.of("set_def", 1)));
    }
}
