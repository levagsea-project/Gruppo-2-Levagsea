package quorma.gameengine.effects;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/**
 * What an effect does to its target, whatever started it: the stack, a static ability or a spell. Only the
 * effects that change a creature's state are here.
 *
 * <p>{@link EffectRegistry} has already refused anything else when the card was bound, so an unknown effect here
 * is an engine bug.</p>
 */
public final class EffectResolver {

    /**
     * What resolving changed.
     *
     * @param changes the target's stats before and after, empty when the effect changed nothing
     * @param stacks  the count for the effect's stack key after resolving, empty if it has none
     */
    public record Outcome(List<StatChange> changes, OptionalInt stacks) {

        public Outcome {
            changes = List.copyOf(changes);
            Objects.requireNonNull(stacks, "stacks");
        }
    }

    private EffectResolver() {}

    /** Applies {@code expansion} to {@code target}, which must not be destroyed. */
    public static Outcome resolve(BoundExpansion expansion, Creature target) {
        if (target.destroyed()) {
            throw new IllegalArgumentException("a destroyed creature is not affected: " + target);
        }
        String mode = expansion.stringParam("mode").orElse("");
        return switch (expansion.effectId() + "/" + mode) {
            case "E1/pct_base_atk" -> addShareOfBaseAtk(expansion, target);
            default -> throw new IllegalStateException("no resolution for effect " + expansion.effectId()
                    + (mode.isEmpty() ? "" : " mode " + mode) + " (EffectRegistry let it through)");
        };
    }

    /**
     * E1 mode {@code pct_base_atk} (Insaziabile): ATK + floor(base ATK × permille / 1000). At {@code max_stacks}
     * it resolves with no change (vocab {@code common_params.max_stacks}).
     */
    private static Outcome addShareOfBaseAtk(BoundExpansion e, Creature target) {
        OptionalInt stacks = OptionalInt.empty();
        if (e.stackKey().isPresent()) {
            String key = e.stackKey().get();
            int applied = target.stacks(key);
            if (e.maxStacks().isPresent() && applied >= e.maxStacks().getAsInt()) {
                return new Outcome(List.of(), OptionalInt.of(applied));
            }
            stacks = OptionalInt.of(target.addStack(key));
        }
        int before = target.currentAtk();
        target.addAtk(Math.toIntExact(Math.floorDiv((long) target.card().atk() * e.intParam("permille"), 1000L)));
        return new Outcome(List.of(new StatChange(StatChange.Stat.ATK, before, target.currentAtk())), stacks);
    }
}
