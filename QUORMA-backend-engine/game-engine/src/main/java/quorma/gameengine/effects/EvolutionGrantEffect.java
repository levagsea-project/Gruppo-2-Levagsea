package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Grant;

/** E16: the creature gains an Evolution it does not already have, for as long as the duration says. */
public final class EvolutionGrantEffect implements Effect {

    @Override
    public String id() {
        return "E16";
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
