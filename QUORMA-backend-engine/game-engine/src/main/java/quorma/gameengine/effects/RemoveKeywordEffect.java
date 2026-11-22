package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;

/**
 * E15: the creature loses a keyword for as long as the duration says. As a static effect it takes the keyword away
 * from the creature's abilities while it reaches it.
 */
public final class RemoveKeywordEffect implements Effect, AbilityLayerEffect {

    public static final String ID = "E15";

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
        return Map.of("", ParamRule.none().strings(Map.of("keyword", Set.of())));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        target.removeKeyword(use.expansion().stringParam("keyword").orElseThrow(), use.lasting());
        return Outcome.NONE;
    }

    @Override
    public void layer(AbilityLayer layer, BoundExpansion e) {
        layer.remove(e.stringParam("keyword").orElseThrow());
    }
}
