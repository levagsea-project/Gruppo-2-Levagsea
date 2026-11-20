package quorma.gameengine.effects;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link PlayerEffect} classes by vocabulary ID. Filled in ID order and never iterated for a result, so the order
 * is only for reading.
 */
public final class PlayerEffects {

    private static final Map<String, PlayerEffect> BY_ID = register(
            new LoseLifeEffect(),
            new GainLifeEffect(),
            new DrawEffect(),
            new ReturnCreatureEffect(),
            new ReorderDeckEffect(),
            new MillEffect(),
            new PendingLaneEffect());

    private PlayerEffects() {}

    /** The class for {@code effectId}, empty for an ID that does not act on a player, the deck or a lane. */
    public static Optional<PlayerEffect> find(String effectId) {
        return Optional.ofNullable(BY_ID.get(effectId));
    }

    /** Every class, in the order they are registered. */
    public static Collection<PlayerEffect> all() {
        return BY_ID.values();
    }

    private static Map<String, PlayerEffect> register(PlayerEffect... effects) {
        Map<String, PlayerEffect> map = new LinkedHashMap<>();
        for (PlayerEffect effect : effects) {
            if (map.put(effect.id(), effect) != null) {
                throw new IllegalStateException("two classes for effect " + effect.id());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
