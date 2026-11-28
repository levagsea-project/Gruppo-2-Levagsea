package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;
import static quorma.gameengine.effects.ParamRule.range;

import java.util.List;
import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;
import quorma.gameengine.model.Lasting;

/**
 * E28: reduces the damage the creature takes, by a percentage ({@code reduction_pct}), a factor in per mille ({@code permille}: 725 takes 27.5% off) or a flat amount with a minimum
 * ({@code flat}). It only means something as a static effect, which the damage calculation reads through
 * {@link #reduction}; resolving it (a trigger, a spell) grants the creature a static copy for the duration, up to
 * {@code max_stacks} times.
 */
public final class DamageReductionEffect implements Effect {

    public static final String ID = "E28";

    /**
     * One reduction of the damage a creature takes.
     *
     * @param flat      true for a flat amount, false for a percentage
     * @param value     the amount (flat) or the percentage ({@code reduction_pct}), or the share of the damage that
     *                  is removed, in per mille ({@code permille} mode: a factor of 725 removes 275)
     * @param minDamage the least damage a flat reduction leaves, 0 for a percentage
     * @param fine      true when {@code value} is in per mille, which allows halves of a percent (27.5%)
     */
    public record Reduction(boolean flat, int value, int minDamage, boolean fine) {

        public Reduction(boolean flat, int value, int minDamage) {
            this(flat, value, minDamage, false);
        }

        /** The share of the damage this percentage reduction removes, in per mille. */
        public int reductionPermille() {
            return fine ? value : 10 * value;
        }
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.C, ResClass.T);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of(
                "reduction_pct", ParamRule.ints(Map.of("pct", range(0, 100))),
                "permille", ParamRule.ints(Map.of("permille", range(0, 1000))),
                "flat", ParamRule.ints(Map.of("amount", NON_NEGATIVE, "min_damage", NON_NEGATIVE))
                        .defaults(Map.of("min_damage", 1)));
    }

    /** What the static effect {@code e} reduces. */
    public static Reduction reduction(BoundExpansion e) {
        if (e.stringParam("mode").orElseThrow().equals("flat")) {
            return new Reduction(true, e.intParam("amount"), e.intParam("min_damage"));
        }
        if (e.stringParam("mode").orElseThrow().equals("permille")) {
            return new Reduction(false, 1000 - e.intParam("permille"), 0, true);
        }
        return new Reduction(false, e.intParam("pct"), 0);
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        return Stacking.apply(e, target, () -> {
            target.grant(new Grant(e.abilityId(), List.of(e.asStatic(TargetIds.SELF, Lasting.PERMANENT)), use.lasting(), false));
            return List.of();
        });
    }
}
