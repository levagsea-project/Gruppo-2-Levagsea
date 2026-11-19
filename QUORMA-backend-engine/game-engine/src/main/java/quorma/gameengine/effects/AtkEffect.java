package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.ANY;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Lasting;

/**
 * E1: ATK + {@code amount} or a share of the base ATK, for as long as the duration says. A static copy (trigger T0)
 * adds the same amount to the creature it reaches, times {@code scale_by} when it has it: the turn's {@code Board}
 * asks {@link #scale} and {@link #amount}, so the arithmetic is in one place.
 */
public final class AtkEffect implements Effect {

    public static final String ID = "E1";

    private static final Set<String> SCALE_BY =
            Set.of("own_evolutions_triggered", "non_attackers", "creatures_in_play", "own_empty_lanes");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.C, ResClass.T, ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of(
                "pct_base_atk", ParamRule.none().optInts(Map.of("permille", ANY, "pct", ANY))
                        .optStrings(Map.of("scale_by", SCALE_BY)),
                "flat", ParamRule.ints(Map.of("amount", ANY)).optStrings(Map.of("scale_by", SCALE_BY)));
    }

    @Override
    public Optional<String> validate(Shape shape) {
        Map<String, Value> params = shape.params();
        if (shape.mode().equals("pct_base_atk") && !params.containsKey("permille") && !params.containsKey("pct")) {
            return Optional.of("effect E1 mode pct_base_atk needs param 'permille' (or 'pct')");
        }
        if (shape.cls() != ResClass.C && params.containsKey("scale_by")) {
            return Optional.of("effect E1 takes param 'scale_by' only in a continuous (class C) effect, got class " + shape.cls());
        }
        return Optional.empty();
    }

    /** The ATK the effect adds: {@code amount}, or a share of {@code baseAtk}, times {@code scale}. */
    public static int amount(BoundExpansion e, int baseAtk, int scale) {
        return Math.multiplyExact(PerMille.baseAmount(e, baseAtk), scale);
    }

    /**
     * The multiplier of {@code scale_by}, counted now for the player {@code side}; 1 without it. Only a static (class
     * C) effect may write {@code scale_by}: {@link #validate} refuses it in a triggered or immediate one (IC014).
     */
    public static int scale(TargetView view, Side side, BoundExpansion e) {
        String by = e.stringParam("scale_by").orElse(null);
        if (by == null) {
            return 1;
        }
        List<Creature> creatures = view.creatures(side);
        return switch (by) {
            case "own_evolutions_triggered" -> creatures.stream().filter(c -> !c.destroyed())
                    .mapToInt(Creature::evolutionsTriggered).sum();
            case "non_attackers" -> (int) creatures.stream().filter(c -> !c.destroyed() && !view.attacker(side, c)).count();
            case "creatures_in_play" -> (int) creatures.stream().filter(c -> !c.destroyed()).count();
            case "own_empty_lanes" -> 5 - creatures.size();
            default -> throw new IllegalStateException("scale_by " + by);
        };
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        Lasting lasting = use.lasting();
        return Stacking.apply(e, target, () -> {
            int before = target.currentAtk();
            int amount = amount(e, target.card().atk(), 1); // a triggered or immediate E1 has no scale_by
            boolean sourceGone = lasting.duration().equals(Lasting.WHILE_SOURCE) && lasting.source() == null;
            target.addAtk(amount, sourceGone ? Lasting.permanent() : lasting);
            return List.of(new StatChange(StatChange.Stat.ATK, before, target.currentAtk()));
        });
    }
}
