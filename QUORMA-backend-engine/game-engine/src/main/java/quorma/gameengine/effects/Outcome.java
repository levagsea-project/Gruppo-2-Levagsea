package quorma.gameengine.effects;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import quorma.gameengine.log.StatChange;

/**
 * What resolving an effect on a creature changed.
 *
 * @param changes the target's stats before and after, empty when the effect changed nothing
 * @param stacks  the count for the effect's stack key after resolving, empty if it has none
 */
public record Outcome(List<StatChange> changes, OptionalInt stacks) {

    /** Nothing changed and nothing was counted. */
    public static final Outcome NONE = new Outcome(List.of(), OptionalInt.empty());

    public Outcome {
        changes = List.copyOf(changes);
        Objects.requireNonNull(stacks, "stacks");
    }
}
