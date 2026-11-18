package quorma.gameengine.effects;

import java.util.List;
import java.util.OptionalInt;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/** E59: cancels the bonuses on the creature's ATK and DEF. */
public final class CancelBonusesEffect implements Effect {

    @Override
    public String id() {
        return "E59";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        int atk = target.currentAtk();
        int def = target.currentDef();
        target.cancelBonuses();
        return new Outcome(List.of(new StatChange(StatChange.Stat.ATK, atk, target.currentAtk()),
                new StatChange(StatChange.Stat.DEF, def, target.currentDef())), OptionalInt.empty());
    }
}
