package quorma.gameengine.effects;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/** The stack key of an effect ({@code max_stacks}), shared by the effects that can stack. */
final class Stacking {

    private Stacking() {}

    /** Applies {@code change} unless the effect's stack key reached {@code max_stacks}; counts the stack. */
    static Outcome apply(BoundExpansion e, Creature t, Supplier<List<StatChange>> change) {
        OptionalInt stacks = OptionalInt.empty();
        if (e.stackKey().isPresent()) {
            String key = e.stackKey().get();
            int applied = t.stacks(key);
            if (e.maxStacks().isPresent() && applied >= e.maxStacks().getAsInt()) {
                return new Outcome(List.of(), OptionalInt.of(applied));
            }
            stacks = OptionalInt.of(t.addStack(key));
        }
        return new Outcome(change.get(), stacks);
    }
}
