package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;

/** E16: the creature gains an Evolution it does not already have, for as long as the duration says. */
public final class EvolutionGrantEffect implements Effect {

    public static final String ID = "E16";

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
        return Map.of("", ParamRule.none().strings(Map.of("evolution_id", Set.of()))
                .prefixes(Set.of("evolution_params")));
    }

    @Override
    public Outcome apply(EffectContext ctx, Use use, Creature target) {
        BoundExpansion e = use.expansion();
        String evolution = e.stringParam("evolution_id").orElseThrow();
        if (!ctx.hasAbility(target, evolution)) {
            target.grant(new Grant(evolution, ctx.binder().bindEvolution("effect " + e.abilityId() + " E16",
                    evolution, e.nested("evolution_params")), use.lasting(), true));
        }
        return Outcome.NONE;
    }
}
