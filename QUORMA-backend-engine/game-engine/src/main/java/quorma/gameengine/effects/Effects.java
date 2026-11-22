package quorma.gameengine.effects;

import java.util.Collection;
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
            new AtkEffect(),
            new DefEffect(),
            new BaseDefEffect(),
            new AtkToBaseEffect(),
            new HealEffect(),
            new DamageEffect(),
            new DestroyEffect(),
            new DiscardEffect(),
            new RemoveKeywordEffect(),
            new KeywordGrantEffect(),
            new EvolutionGrantEffect(),
            new SuppressEvolutionEffect(),
            new DamageReductionEffect(),
            new ScaledDamageEffect(),
            new CancelBonusesEffect());

    private Effects() {}

    /** The class for {@code effectId}; empty for an ID that resolves no effect on a creature (player, static, replacement). */
    public static Optional<Effect> find(String effectId) {
        return Optional.ofNullable(BY_ID.get(effectId));
    }

    /** Every class, in the order they are registered. */
    public static Collection<Effect> all() {
        return BY_ID.values();
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
