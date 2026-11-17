package quorma.gameengine.effects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.List;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;

/**
 * An expansion of one ability of one card, ready to run: every {@code $param} and {@code $tune} reference is
 * resolved and the conditions that depend only on the card ({@code K_PARAM}) are already settled. Built by
 * {@link AbilityBinder} when a creature enters play.
 *
 * @param abilityId   the keyword the expansion comes from, for example {@code K_SCUDO}
 * @param triggerParams the trigger params, for example {@code phase} of T2
 * @param targetParams the target params as written, for example {@code filters} of S41
 * @param conditions  the conditions left to check while the creature is in play (everything but {@code K_PARAM}),
 *                    with literal params
 * @param effectParams resolved effect params, in file order: only {@link Value.IntV}, {@link Value.StrV} and
 *                    {@link Value.BoolV}
 * @param maxStacks   {@code max_stacks}, empty when the effect has no limit
 * @param stackKey    {@code stack_key}, empty when the expansion does not stack
 * @param evolution   true for an expansion of an Evolution: {@code triggerId} is then the real trigger inside
 *                    {@code T_EVO}, and the ability fires at most {@code maxTriggers} times per creature
 * @param maxTriggers how many times an Evolution may fire per creature (1), unused when not an evolution
 */
public record BoundExpansion(
        String abilityId,
        String triggerId,
        Map<String, Value> triggerParams,
        String effectId,
        String targetId,
        Map<String, Value> targetParams,
        List<ConditionDef> conditions,
        String duration,
        ResClass cls,
        Map<String, Value> effectParams,
        OptionalInt maxStacks,
        Optional<String> stackKey,
        boolean evolution,
        int maxTriggers) {

    public BoundExpansion {
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(triggerId, "triggerId");
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(cls, "cls");
        Objects.requireNonNull(maxStacks, "maxStacks");
        Objects.requireNonNull(stackKey, "stackKey");
        effectParams = Collections.unmodifiableMap(new LinkedHashMap<>(effectParams));
        triggerParams = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(triggerParams, "triggerParams")));
        targetParams = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(targetParams, "targetParams")));
        conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
    }

    /** The same expansion marked as an Evolution that fires at most {@code maxTriggers} times per creature. */
    public BoundExpansion asEvolution(int maxTriggers) {
        return new BoundExpansion(abilityId, triggerId, triggerParams, effectId, targetId, targetParams, conditions,
                duration, cls, effectParams, maxStacks, stackKey, true, maxTriggers);
    }

    /**
     * The same expansion with every integer effect param multiplied by {@code factorPermille} / 1000, floored
     * (effect E19, Spira 4). 1000 returns this.
     */
    public BoundExpansion scaled(int factorPermille) {
        if (factorPermille == 1000) {
            return this;
        }
        Map<String, Value> scaled = new LinkedHashMap<>();
        effectParams.forEach((name, value) -> scaled.put(name, value instanceof Value.IntV n
                ? new Value.IntV(Math.floorDiv(n.v() * factorPermille, 1000L)) : value));
        return new BoundExpansion(abilityId, triggerId, triggerParams, effectId, targetId, targetParams, conditions,
                duration, cls, scaled, maxStacks, stackKey, evolution, maxTriggers);
    }

    /** An integer effect param. */
    public int intParam(String name) {
        if (effectParams.get(name) instanceof Value.IntV n) {
            return Math.toIntExact(n.v());
        }
        throw new IllegalStateException(abilityId + " " + effectId + ": param '" + name + "' is not an integer");
    }

    /** A string effect param, empty if absent. */
    public Optional<String> stringParam(String name) {
        return effectParams.get(name) instanceof Value.StrV s ? Optional.of(s.v()) : Optional.empty();
    }
}
