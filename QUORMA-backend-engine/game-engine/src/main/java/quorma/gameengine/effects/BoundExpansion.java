package quorma.gameengine.effects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;

/**
 * An expansion of one ability of one card, ready to run: every {@code $param} and {@code $tune} reference is
 * resolved and the conditions that depend only on the card ({@code K_PARAM}) are already settled. Built by
 * {@link AbilityBinder} when a creature enters play.
 *
 * @param abilityId   the keyword the expansion comes from, for example {@code K_SCUDO}
 * @param triggerId   null for a class I expansion of a spell or equipment, which has no trigger
 * @param triggerParams the trigger params, for example {@code phase} of T2
 * @param targetParams the target params as written, for example {@code filters} of S41
 * @param conditions  the conditions left to check while the creature is in play (everything but {@code K_PARAM}),
 *                    with literal params
 * @param effectParams resolved effect params, in file order: only {@link Value.IntV}, {@link Value.StrV} and
 *                    {@link Value.BoolV}; an object param becomes one param per field, named {@code parent.field}
 * @param maxStacks   {@code max_stacks}, empty when the effect has no limit
 * @param stackKey    {@code stack_key}, empty when the expansion does not stack
 * @param evolution   true for an expansion of an Evolution: {@code triggerId} is then the real trigger inside
 *                    {@code T_EVO}, and the ability fires at most {@code maxTriggers} times per creature
 * @param maxTriggers how many times the expansion may fire per card instance for the whole match ({@code max_triggers},
 *                    1 for an Evolution); 0 when there is no limit
 * @param oncePerTurn {@code once_per_turn}: at most one resolution per turn
 * @param index       position of the expansion in its ability, from 0: with {@code abilityId} it names the
 *                    expansion for its trigger counters
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
        int maxTriggers,
        boolean oncePerTurn,
        int index) {

    public BoundExpansion {
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(cls, "cls");
        if (triggerId == null && cls != ResClass.I) {
            throw new NullPointerException("triggerId: only a class I expansion (a spell or equipment effect) has no trigger");
        }
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
                duration, cls, effectParams, maxStacks, stackKey, true, maxTriggers, oncePerTurn, index);
    }

    /** The same expansion with other conditions. */
    public BoundExpansion withConditions(List<ConditionDef> newConditions) {
        return new BoundExpansion(abilityId, triggerId, triggerParams, effectId, targetId, targetParams, newConditions,
                duration, cls, effectParams, maxStacks, stackKey, evolution, maxTriggers, oncePerTurn, index);
    }

    /** The same expansion with another target (an equipment's S27 is the creature that carries it: S1). */
    public BoundExpansion withTarget(String newTargetId) {
        return new BoundExpansion(abilityId, triggerId, triggerParams, effectId, newTargetId, targetParams, conditions,
                duration, cls, effectParams, maxStacks, stackKey, evolution, maxTriggers, oncePerTurn, index);
    }

    /** The same expansion as a static one (trigger T0, class C) with {@code newDuration}: an effect granted to a creature. */
    public BoundExpansion asStatic(String newTargetId, String newDuration) {
        return new BoundExpansion(abilityId, "T0", Map.of(), effectId, newTargetId, Map.of(), conditions, newDuration,
                ResClass.C, effectParams, OptionalInt.empty(), Optional.empty(), false, 0, false, index);
    }

    /** The same expansion with no trigger, class I: applied at once (an equipment's lasting DEF change). */
    public BoundExpansion asImmediate(String newTargetId, String newDuration) {
        return new BoundExpansion(abilityId, null, Map.of(), effectId, newTargetId, Map.of(), conditions, newDuration,
                ResClass.I, effectParams, maxStacks, stackKey, false, 0, false, index);
    }

    /** The key of this expansion's trigger counters ({@code max_triggers}, {@code once_per_turn}). */
    public String counterKey() {
        return evolution ? abilityId : abilityId + "#" + index;
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
                duration, cls, scaled, maxStacks, stackKey, evolution, maxTriggers, oncePerTurn, index);
    }

    /** An integer effect param. */
    public int intParam(String name) {
        if (effectParams.get(name) instanceof Value.IntV n) {
            return Math.toIntExact(n.v());
        }
        throw new IllegalStateException(abilityId + " " + effectId + ": param '" + name + "' is not an integer");
    }

    /** An integer effect param, or {@code otherwise} when absent. */
    public int intParam(String name, int otherwise) {
        return effectParams.get(name) instanceof Value.IntV n ? Math.toIntExact(n.v()) : otherwise;
    }

    /** A string effect param, empty if absent. */
    public Optional<String> stringParam(String name) {
        return effectParams.get(name) instanceof Value.StrV s ? Optional.of(s.v()) : Optional.empty();
    }

    /** A boolean effect param, or {@code otherwise} when absent. */
    public boolean boolParam(String name, boolean otherwise) {
        return effectParams.get(name) instanceof Value.BoolV b ? b.v() : otherwise;
    }

    /** A string trigger param, empty if absent. */
    public Optional<String> triggerParam(String name) {
        return triggerParams.get(name) instanceof Value.StrV s ? Optional.of(s.v()) : Optional.empty();
    }

    /**
     * The effect params whose name starts with {@code prefix.} (for example {@code keyword_params.x} of E14), with
     * the prefix taken off: the params an E14 gives the keyword it grants.
     */
    public Map<String, Value> nested(String prefix) {
        Map<String, Value> out = new LinkedHashMap<>();
        effectParams.forEach((name, value) -> {
            if (name.startsWith(prefix + ".")) {
                out.put(name.substring(prefix.length() + 1), value);
            }
        });
        return out;
    }

    /** The {@code filters} of the target params, empty if none. */
    public List<String> filters() {
        if (targetParams.get("filters") instanceof Value.ListV l) {
            return l.items().stream().filter(v -> v instanceof Value.StrV).map(v -> ((Value.StrV) v).v()).toList();
        }
        return List.of();
    }
}
