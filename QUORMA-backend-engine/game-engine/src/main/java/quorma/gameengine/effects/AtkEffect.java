package quorma.gameengine.effects;

import java.util.List;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Lasting;

/** E1: ATK + {@code amount} or a share of the base ATK, for as long as the duration says. */
public final class AtkEffect implements Effect {

    @Override
    public String id() {
        return "E1";
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
