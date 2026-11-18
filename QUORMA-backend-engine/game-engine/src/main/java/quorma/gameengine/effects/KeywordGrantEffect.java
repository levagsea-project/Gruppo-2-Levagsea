package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;

/** E14: the creature gains a keyword (with the numbers in {@code keyword_params}) for as long as the duration says. */
public final class KeywordGrantEffect implements Effect {

    @Override
    public String id() {
        return "E14";
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        String keyword = e.stringParam("keyword").orElseThrow();
        target.grant(new Grant(keyword, ctx.binder().bindKeyword("effect " + e.abilityId() + " E14", keyword,
                e.nested("keyword_params")), use.lasting(), true));
        return Outcome.NONE;
    }
}
