package quorma.gameengine.effects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;

/**
 * An expansion of one ability of one card, ready to run: every {@code $param} and {@code $tune} reference is
 * resolved and the conditions that depend only on the card ({@code K_PARAM}) are already settled. Built by
 * {@link AbilityBinder} when a creature enters play.
 *
 * @param abilityId   the keyword the expansion comes from, for example {@code K_SCUDO}
 * @param effectParams resolved effect params, in file order: only {@link Value.IntV}, {@link Value.StrV} and
 *                    {@link Value.BoolV}
 * @param maxStacks   {@code max_stacks}, empty when the effect has no limit
 * @param stackKey    {@code stack_key}, empty when the expansion does not stack
 */
public record BoundExpansion(
        String abilityId,
        String triggerId,
        String effectId,
        String targetId,
        String duration,
        ResClass cls,
        Map<String, Value> effectParams,
        OptionalInt maxStacks,
        Optional<String> stackKey) {

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
