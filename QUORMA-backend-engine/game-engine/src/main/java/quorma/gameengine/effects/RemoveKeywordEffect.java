package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;

/** E15: the creature loses a keyword for as long as the duration says. */
public final class RemoveKeywordEffect implements Effect {

    @Override
    public String id() {
        return "E15";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        target.removeKeyword(use.expansion().stringParam("keyword").orElseThrow(), use.lasting());
        return Outcome.NONE;
    }
}
