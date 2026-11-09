package quorma.gameengine.data;

import java.util.List;
import java.util.Map;

/** One (trigger, effect, target) tuple. JSON key {@code class} is {@code cls} here. Params may contain {@link Value.Ref}. */
public record ExpansionDef(
        String triggerId, Map<String, Value> triggerParams,
        String effectId, Map<String, Value> effectParams,
        String targetId, Map<String, Value> targetParams,
        String duration,
        List<ConditionDef> conditions,
        ResClass cls) {}
