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
import quorma.gameengine.model.Lasting;

/** E1: ATK + {@code amount} or a share of the base ATK, for as long as the duration says. */
public final class AtkEffect implements Effect {

    private static final Set<String> SCALE_BY =
            Set.of("own_evolutions_triggered", "non_attackers", "creatures_in_play", "own_empty_lanes");

    @Override
    public String id() {
        return "E1";
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
    public Optional<String> validate(String mode, Map<String, Value> params) {
        if (mode.equals("pct_base_atk") && !params.containsKey("permille") && !params.containsKey("pct")) {
            return Optional.of("effect E1 mode pct_base_atk needs param 'permille' (or 'pct')");
        }
        return Optional.empty();
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        Lasting lasting = use.lasting();
        return Stacking.apply(e, target, () -> {
            int before = target.currentAtk();
            int amount = PerMille.baseAmount(e, target.card().atk());
            boolean sourceGone = lasting.duration().equals(Lasting.WHILE_SOURCE) && lasting.source() == null;
            target.addAtk(amount, sourceGone ? Lasting.permanent() : lasting);
            return List.of(new StatChange(StatChange.Stat.ATK, before, target.currentAtk()));
        });
    }
}
