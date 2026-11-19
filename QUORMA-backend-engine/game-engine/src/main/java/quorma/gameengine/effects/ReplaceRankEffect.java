package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import quorma.gameengine.data.ResClass;

/**
 * E32 (For the hand, the creature's rank is replaced by {@code rank} ({@code highest_ally}: the highest rank among its living allies).) Read by {@code Hands}; it has no resolution of its own.
 */
public final class ReplaceRankEffect implements EffectDefinition {

    public static final String ID = "E32";

    private static final Set<String> RANKS_OR_ALLY = Stream.concat(ExtraRankEffect.RANKS.stream(), Stream.of("highest_ally"))
            .collect(Collectors.toUnmodifiableSet());

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
        return Map.of("", ParamRule.none().strings(Map.of("rank", RANKS_OR_ALLY)));
    }
}
