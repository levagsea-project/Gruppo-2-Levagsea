package quorma.gameengine.effects;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link Condition} classes by vocabulary ID. Filled in ID order and never iterated for a result, so the order is
 * only for reading.
 */
public final class Conditions {

    private static final Map<String, Condition> BY_ID = register(
            new AttackerCondition(),
            new NonAttackerCondition(),
            new SuitCountCondition(),
            new FirstCycleCondition(),
            new HandTypeCondition(),
            new AdjacentSuitCondition(),
            new WouldKillCondition(),
            new TargetDefAboveAtkCondition(),
            new NotLockedCondition(),
            new VacatedSuitCondition(),
            new AdjacentAlliesCondition(),
            new KilledCondition(),
            new DestroyedAdjacentCondition());

    private Conditions() {}

    /** The class for {@code conditionId}, empty for an ID the engine does not evaluate. */
    public static Optional<Condition> find(String conditionId) {
        return Optional.ofNullable(BY_ID.get(conditionId));
    }

    /** Every class, in the order they are registered. */
    public static Collection<Condition> all() {
        return BY_ID.values();
    }

    private static Map<String, Condition> register(Condition... conditions) {
        Map<String, Condition> map = new LinkedHashMap<>();
        for (Condition condition : conditions) {
            if (map.put(condition.id(), condition) != null) {
                throw new IllegalStateException("two classes for condition " + condition.id());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
