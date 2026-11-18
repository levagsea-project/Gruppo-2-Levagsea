package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;

/** E4: the creature's ATK goes back to its base value (it replaces the ATK bonuses while it lasts). */
public final class AtkToBaseEffect implements Effect {

    @Override
    public String id() {
        return "E4";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        target.setAtkToBase(use.expansion().abilityId(), use.lasting());
        return Outcome.NONE;
    }
}
