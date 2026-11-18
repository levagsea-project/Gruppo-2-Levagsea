package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.Value;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/** E6 on its own: the healing, the cap at the base DEF and the heal factor, worked out by hand (base DEF 3000). */
class HealEffectTest {

    private static Outcome apply(Map<String, Value> params, Creature target, int healPermille) {
        FakeContext ctx = new FakeContext();
        ctx.healPermille = healPermille;
        return new HealEffect().apply(ctx, EffectFixtures.use("E6", params, EffectFixtures.creature(1000, 3000)),
                target);
    }

    private static Outcome def(int before, int after) {
        return new Outcome(List.of(new StatChange(StatChange.Stat.DEF, before, after)), OptionalInt.empty());
    }

    private static Map<String, Value> flat(int amount) {
        return Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(amount));
    }

    @Test
    void aFlatHealAddsTheAmount() {
        Creature t = EffectFixtures.wounded(2000);
        assertEquals(def(1000, 1800), apply(flat(800), t, 1000));
        assertEquals(1800, t.currentDef());
    }

    @Test
    void aHealNeverGoesAboveTheBaseDef() {
        assertEquals(def(2500, 3000), apply(flat(800), EffectFixtures.wounded(500), 1000));
    }

    @Test
    void aShareOfBaseDefIsFloored() {
        // 3000 * 333 / 1000 = 999.0 -> 999
        Map<String, Value> share = Map.of("mode", new Value.StrV("pct_base_def"), "permille", new Value.IntV(333));
        assertEquals(def(1000, 1999), apply(share, EffectFixtures.wounded(2000), 1000));
    }

    @Test
    void theHealFactorScalesTheAmountAndZeroStopsIt() {
        assertEquals(def(1000, 1400), apply(flat(800), EffectFixtures.wounded(2000), 500));
        assertEquals(def(1000, 1000), apply(flat(800), EffectFixtures.wounded(2000), 0));
    }
}
