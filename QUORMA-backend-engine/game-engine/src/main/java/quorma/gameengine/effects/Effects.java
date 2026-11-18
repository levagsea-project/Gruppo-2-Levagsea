package quorma.gameengine.effects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link Effect} classes by vocabulary ID. Filled in ID order and never iterated for a result, so the order is
 * only for reading.
 */
public final class Effects {

    private static final Map<String, Effect> BY_ID = register(
            new HealEffect());

    private Effects() {}

    /** The class for {@code effectId}, empty while that effect is still resolved by the old switch in {@code Game}. */
    public static Optional<Effect> find(String effectId) {
        return Optional.ofNullable(BY_ID.get(effectId));
    }

    private static Map<String, Effect> register(Effect... effects) {
        Map<String, Effect> map = new LinkedHashMap<>();
        for (Effect effect : effects) {
            if (map.put(effect.id(), effect) != null) {
                throw new IllegalStateException("two classes for effect " + effect.id());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
