package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;

/** E6 on its own: the healing, the cap at the base DEF and the heal factor, worked out by hand (base DEF 3000). */
class HealEffectTest {

    private static final AbilityBinder BINDER = new AbilityBinder(RulesetLoader.fromClasspath());

    private static BoundExpansion heal(Map<String, Value> params) {
        ExpansionDef def = new ExpansionDef("T19", Map.of(), "E6", params, "S1", Map.of(), "D_Permanente", List.of(),
                ResClass.T);
        CreatureCard card = new CreatureCard("X", 5, Suit.VINCOLO, Subfaction.ADATTATI, 1000, 3000,
                List.of(new InlineEffect(List.of(def))));
        return new Creature(card, 1, BINDER.bind(card)).abilities("T19").get(0);
    }

    private static Creature wounded(int damage) {
        CreatureCard card = new CreatureCard("Y", 6, Suit.VINCOLO, Subfaction.ADATTATI, 1000, 3000, List.of());
        Creature c = new Creature(card, 2, BINDER.bind(card));
        c.takeDamage(damage);
        return c;
    }

    private static Outcome apply(BoundExpansion e, Creature target, int healPermille) {
        EffectContext ctx = t -> healPermille;
        return new HealEffect().apply(ctx, new Effect.Use(e, Side.A, Side.A, null, Lasting.permanent(),
                TriggerContext.NONE, new Followups()), target);
    }

    private static Outcome def(int before, int after) {
        return new Outcome(List.of(new StatChange(StatChange.Stat.DEF, before, after)), OptionalInt.empty());
    }

    @Test
    void aFlatHealAddsTheAmount() {
        Creature t = wounded(2000);
        assertEquals(def(1000, 1800), apply(heal(Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(800))), t, 1000));
        assertEquals(1800, t.currentDef());
    }

    @Test
    void aHealNeverGoesAboveTheBaseDef() {
        Creature t = wounded(500);
        assertEquals(def(2500, 3000), apply(heal(Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(800))), t, 1000));
    }

    @Test
    void aShareOfBaseDefIsFloored() {
        Creature t = wounded(2000);
        // 3000 * 333 / 1000 = 999.0 -> 999
        assertEquals(def(1000, 1999), apply(heal(Map.of("mode", new Value.StrV("pct_base_def"), "permille", new Value.IntV(333))), t, 1000));
    }

    @Test
    void theHealFactorScalesTheAmountAndZeroStopsIt() {
        BoundExpansion e = heal(Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(800)));
        assertEquals(def(1000, 1400), apply(e, wounded(2000), 500));
        assertEquals(def(1000, 1000), apply(e, wounded(2000), 0));
    }
}
