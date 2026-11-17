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
            case "E1/flat" -> addFlatAtk(expansion, target);
            case "E6/flat" -> heal(expansion, target);
            case "E2/pct_base_def" -> addShareOfBaseDef(expansion, target);
            default -> throw new IllegalStateException("no resolution for effect " + expansion.effectId()
                    + (mode.isEmpty() ? "" : " mode " + mode) + " (EffectRegistry let it through)");
        };
    }

    /**
     * E1 mode {@code flat}: + {@code amount} ATK. With duration {@code D_FinoAUccisione} it lasts until the creature
     * destroys a creature, otherwise it stays.
     */
    private static Outcome addFlatAtk(BoundExpansion e, Creature target) {
        int before = target.currentAtk();
        if (e.duration().equals("D_FinoAUccisione")) {
            target.addUntilKillAtk(e.intParam("amount"));
        } else {
            target.addAtk(e.intParam("amount"));
        }
        return new Outcome(List.of(new StatChange(StatChange.Stat.ATK, before, target.currentAtk())), OptionalInt.empty());
    }

    /** E2 mode {@code pct_base_def}: DEF + floor(base DEF x permille / 1000), with no cap (Trincea). */
    private static Outcome addShareOfBaseDef(BoundExpansion e, Creature target) {
        int before = target.currentDef();
        target.addDef(Math.toIntExact(Math.floorDiv((long) target.card().def() * e.intParam("permille"), 1000L)));
        return new Outcome(List.of(new StatChange(StatChange.Stat.DEF, before, target.currentDef())), OptionalInt.empty());
    }

    /** E6 mode {@code flat}: + {@code amount} DEF, up to the base DEF. */
    private static Outcome heal(BoundExpansion e, Creature target) {
        int before = target.currentDef();
        target.heal(e.intParam("amount"));
        return new Outcome(List.of(new StatChange(StatChange.Stat.DEF, before, target.currentDef())), OptionalInt.empty());
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
