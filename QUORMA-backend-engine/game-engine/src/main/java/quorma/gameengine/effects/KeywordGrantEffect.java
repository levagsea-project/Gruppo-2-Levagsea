package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;

/**
 * E14: the creature gains a keyword (with the numbers in {@code keyword_params}) for as long as the duration says.
 * As a static effect it adds the keyword to the creature's abilities while it reaches it.
 */
public final class KeywordGrantEffect implements Effect, AbilityLayerEffect {

    public static final String ID = "E14";

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
        return Map.of("", ParamRule.none().strings(Map.of("keyword", Set.of()))
                .prefixes(Set.of("keyword_params")));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        String keyword = e.stringParam("keyword").orElseThrow();
        target.grant(new Grant(keyword, ctx.binder().bindKeyword("effect " + e.abilityId() + " " + ID, keyword,
                e.nested("keyword_params")), use.lasting(), true));
        return Outcome.NONE;
    }

    @Override
    public void layer(AbilityLayer layer, BoundExpansion e) {
        layer.add(layer.binder().bindKeyword("static " + e.abilityId() + " " + ID, e.stringParam("keyword").orElseThrow(),
                e.nested("keyword_params")));
    }
}
