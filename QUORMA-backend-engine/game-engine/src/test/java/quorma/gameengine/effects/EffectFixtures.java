package quorma.gameengine.effects;

import java.util.List;
import java.util.Map;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;

/** Test helpers: a creature and a bound effect to hand to an {@link Effect} together with a {@link FakeContext}. */
final class EffectFixtures {

    static final AbilityBinder BINDER = new AbilityBinder(RulesetLoader.fromClasspath());

    private EffectFixtures() {}

    static Creature creature(int atk, int def) {
        CreatureCard card = new CreatureCard("X", 5, Suit.VINCOLO, Subfaction.ADATTATI, atk, def, List.of());
        return new Creature(card, 1, BINDER.bind(card));
    }

    /** A creature with base DEF 3000 that has taken {@code damage}. */
    static Creature wounded(int damage) {
        Creature c = creature(1000, 3000);
        c.takeDamage(damage);
        return c;
    }

    /** The effect {@code effectId} with {@code params}, as the binder bakes it into a creature's inline ability. */
    static BoundExpansion expansion(String effectId, Map<String, Value> params) {
        return expansion(effectId, params, "S1", Map.of());
    }

    /** Like {@link #expansion(String, Map)} with another target and its params. */
    static BoundExpansion expansion(String effectId, Map<String, Value> params, String targetId,
            Map<String, Value> targetParams) {
        ExpansionDef def = new ExpansionDef("T19", Map.of(), effectId, params, targetId, targetParams, "D_Permanente",
                List.of(), ResClass.T);
        CreatureCard card = new CreatureCard("S", 5, Suit.VINCOLO, Subfaction.ADATTATI, 1000, 3000,
                List.of(new InlineEffect(List.of(def))));
        return new Creature(card, 1, BINDER.bind(card)).abilities("T19").get(0);
    }

    /** A resolution started by {@code source}, owned by side A, aimed at a creature of side B, permanent. */
    static Effect.Use use(String effectId, Map<String, Value> params, Creature source) {
        return use(effectId, params, source, TriggerContext.NONE);
    }

    static Effect.Use use(String effectId, Map<String, Value> params, Creature source, TriggerContext context) {
        return new Effect.Use(expansion(effectId, params), Side.A, Side.B, source, Lasting.permanent(), context,
                new Followups());
    }
}
