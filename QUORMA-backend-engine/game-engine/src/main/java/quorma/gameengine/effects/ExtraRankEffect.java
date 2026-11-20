package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E30 (For the hand, the creature counts as {@code rank} as well as its own rank (one such substitution per hand).) Read by {@code Hands}; it has no resolution of its own.
 */
public final class ExtraRankEffect implements EffectDefinition {

    public static final String ID = "E30";

    /** The ranks the data may name. */
    static final Set<String> RANKS = Set.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K");

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
        return Map.of("", ParamRule.none().strings(Map.of("rank", RANKS)));
    }
}
