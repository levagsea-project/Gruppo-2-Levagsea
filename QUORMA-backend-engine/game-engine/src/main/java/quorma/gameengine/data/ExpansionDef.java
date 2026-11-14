package quorma.gameengine.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One (trigger, effect, target) tuple. JSON key {@code class} is {@code cls} here. Params may contain {@link Value.Ref}.
 *
 * @param common the optional common params allowed on any expansion ({@code vocab.common_params}):
 *               {@code max_triggers}, {@code once_per_turn}, {@code max_stacks}, {@code stack_key}. Only the keys
 *               present in the file, in file order. May contain {@link Value.Ref}
 */
public record ExpansionDef(
        String triggerId, Map<String, Value> triggerParams,
        String effectId, Map<String, Value> effectParams,
        String targetId, Map<String, Value> targetParams,
        String duration,
        List<ConditionDef> conditions,
        ResClass cls,
        Map<String, Value> common) {

    /** The keys of {@code vocab.common_params} that an expansion may carry. */
    public static final Set<String> COMMON_KEYS = Set.of("max_triggers", "once_per_turn", "max_stacks", "stack_key");

    public ExpansionDef {
        common = Collections.unmodifiableMap(new LinkedHashMap<>(common));
    }

    /** An expansion with no common params. */
    public ExpansionDef(
            String triggerId, Map<String, Value> triggerParams,
            String effectId, Map<String, Value> effectParams,
            String targetId, Map<String, Value> targetParams,
            String duration,
            List<ConditionDef> conditions,
            ResClass cls) {
        this(triggerId, triggerParams, effectId, effectParams, targetId, targetParams, duration, conditions, cls, Map.of());
    }
}
