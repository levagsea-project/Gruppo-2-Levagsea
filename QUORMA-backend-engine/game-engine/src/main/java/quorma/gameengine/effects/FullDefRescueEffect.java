package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.range;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E12 (T20: the target is not destroyed and keeps its base DEF; with {@code then: destroy_at_next_refill} the destruction waits for Refill, and {@code lockout} says when the effect works again (Custode).) Read by {@code Destruction.rescue}; it has no resolution of its own.
 */
public final class FullDefRescueEffect implements EffectDefinition {

    public static final String ID = "E12";

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
        return Map.of("", ParamRule.none()
                .optInts(Map.of("lockout.reactivate_below", range(1, 5)))
                .optStrings(Map.of("then", Set.of("none", "destroy_at_next_refill"), "lockout.key", Set.of(),
                        "lockout.suit", ConditionParams.SUITS)));
    }
}
