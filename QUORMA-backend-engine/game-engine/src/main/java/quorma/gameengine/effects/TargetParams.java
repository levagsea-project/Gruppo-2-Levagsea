package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import quorma.gameengine.data.Value;

/** The target params every target reads ({@code filters}, {@code from}) and the check of a target's own params. */
final class TargetParams {

    private TargetParams() {}

    /**
     * True if every param is one the target reads, with a value it accepts: {@code own} says the target's own params,
     * {@code filters} and {@code from} are the common ones.
     */
    static boolean accepts(Map<String, Value> params, Map<String, Predicate<Value>> own) {
        for (Map.Entry<String, Value> e : params.entrySet()) {
            Predicate<Value> check = own.get(e.getKey());
            boolean ok = check != null ? check.test(e.getValue()) : switch (e.getKey()) {
                case "filters" -> e.getValue() instanceof Value.ListV l && l.items().stream()
                        .allMatch(v -> v instanceof Value.StrV f && EffectRegistry.TARGET_FILTERS.contains(f.v()));
                case "from" -> e.getValue() instanceof Value.StrV s && Set.of(TargetIds.OWN_CREATURES,
                        TargetIds.ENEMY_CREATURES, TargetIds.ALL_CREATURES).contains(s.v());
                default -> false;
            };
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
