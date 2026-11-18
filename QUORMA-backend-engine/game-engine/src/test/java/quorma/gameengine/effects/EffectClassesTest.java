package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.Value;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;

/**
 * One focused test per effect class that moved out of {@code Game}, without the stack or the turn: the effect, a
 * {@link FakeContext} and a creature. Numbers worked out by hand. E28 (stacking grant) and the
 * positive case of E16 need bound abilities of the real vocabulary and are covered by {@code BaseSetTest} and
 * {@code BaseSetEffectsTest}.
 */
class EffectClassesTest {

    private static Map<String, Value> flat(int amount) {
        return Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(amount));
    }

    private static Outcome apply(Effect effect, FakeContext ctx, Effect.Use use, Creature target) {
        return effect.apply(ctx, use, target);
    }

    private static Outcome change(StatChange.Stat stat, int before, int after) {
        return new Outcome(List.of(new StatChange(stat, before, after)), OptionalInt.empty());
    }

    // ---- E1

    @Test
    void e1FlatAddsAtk() {
        Creature t = EffectFixtures.creature(4000, 3000);
        Outcome o = apply(new AtkEffect(), new FakeContext(), EffectFixtures.use("E1", flat(700), t), t);
        assertEquals(change(StatChange.Stat.ATK, 4000, 4700), o);
    }

    @Test
    void e1ShareOfBaseAtkIsFloored() {
        Creature t = EffectFixtures.creature(1001, 3000); // 1001 * 150 / 1000 = 150.15
        Map<String, Value> share = Map.of("mode", new Value.StrV("pct_base_atk"), "permille", new Value.IntV(150));
        apply(new AtkEffect(), new FakeContext(), EffectFixtures.use("E1", share, t), t);
        assertEquals(1151, t.currentAtk());
    }

    @Test
    void e1MalusLowersAtk() {
        Creature t = EffectFixtures.creature(4000, 3000);
        apply(new AtkEffect(), new FakeContext(), EffectFixtures.use("E1", flat(-1500), t), t);
        assertEquals(2500, t.currentAtk());
    }

    // ---- E2

    @Test
    void e2FlatAddsDef() {
        Creature t = EffectFixtures.creature(1000, 3000);
        Outcome o = apply(new DefEffect(), new FakeContext(), EffectFixtures.use("E2", flat(500), t), t);
        assertEquals(change(StatChange.Stat.DEF, 3000, 3500), o);
    }

    @Test
    void e2BonusDoesNothingWhenTheTargetIsReachedByE61() {
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        ctx.noBonus = true;
        Outcome o = apply(new DefEffect(), ctx, EffectFixtures.use("E2", flat(500), t), t);
        assertEquals(change(StatChange.Stat.DEF, 3000, 3000), o);
        assertEquals(3000, t.currentDef());
    }

    @Test
    void e2MalusThatKillsDestroysTheCreatureByEffect() {
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        Outcome o = apply(new DefEffect(), ctx, EffectFixtures.use("E2", flat(-3000), t), t);
        assertEquals(List.of("save? B", "destroyed B by A"), ctx.calls);
        assertTrue(t.destroyed());
        assertEquals(1, o.changes().size());
    }

    @Test
    void e2MalusThatWouldKillIsStoppedByARescue() {
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        ctx.saves = true;
        apply(new DefEffect(), ctx, EffectFixtures.use("E2", flat(-3000), t), t);
        assertEquals(List.of("save? B"), ctx.calls);
        assertEquals(3000, t.currentDef(), "the replacement, not the effect, sets the DEF");
    }

    @Test
    void e2ScaleByAdjacentAlliesMultipliesTheAmount() {
        Creature source = EffectFixtures.creature(1000, 3000);
        Creature t = EffectFixtures.creature(1000, 3000);
        Map<String, Value> scaled = Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(200),
                "scale_by", new Value.StrV("adjacent_allies"));
        FakeContext ctx = new FakeContext();
        ctx.adjacentAllies = 2;
        apply(new DefEffect(), ctx, EffectFixtures.use("E2", scaled, source), t);
        assertEquals(3400, t.currentDef());
    }

    // ---- E3

    @Test
    void e3RaisesTheBaseDefWithoutHealing() {
        Creature t = EffectFixtures.creature(1000, 3000);
        // The fixture binds triggered abilities; E3 is immediate-only, so borrow an expansion with an "amount" param.
        Effect.Use use = EffectFixtures.use("E7", Map.of("amount", new Value.IntV(500)), t);
        apply(new BaseDefEffect(), new FakeContext(), use, t);
        assertEquals(3500, t.baseDef());
        assertEquals(3000, t.currentDef(), "the cap moves, the current DEF does not");
    }

    // ---- E4

    @Test
    void e4SetsAtkToBaseByTheAbility() {
        Creature t = EffectFixtures.creature(1000, 3000);
        Effect.Use use = EffectFixtures.use("E4", Map.of(), t);
        assertEquals(Outcome.NONE, apply(new AtkToBaseEffect(), new FakeContext(), use, t));
        assertEquals(use.expansion().abilityId(), t.atkSetToBaseBy());
    }

    // ---- E7, E58

    @Test
    void e7DealsItsAmountFromTheOwnerToTheTargetSide() {
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        apply(new DamageEffect(), ctx, EffectFixtures.use("E7", Map.of("amount", new Value.IntV(800)), t), t);
        assertEquals(List.of("damage 800 from A to B"), ctx.calls);
    }

    @Test
    void e58TakesAShareOfTheSourceAtkFlooredOrOfTheTriggeringAmount() {
        Creature source = EffectFixtures.creature(1001, 3000);
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        Map<String, Value> own = Map.of("permille", new Value.IntV(500), "source", new Value.StrV("own_atk"));
        apply(new ScaledDamageEffect(), ctx, EffectFixtures.use("E58", own, source), t);
        Map<String, Value> taken = Map.of("permille", new Value.IntV(500), "source", new Value.StrV("damage_taken"));
        apply(new ScaledDamageEffect(), ctx, EffectFixtures.use("E58", taken, source,
                TriggerContext.damagedBy(quorma.gameengine.model.Side.B, null, 601)), t);
        // 1001 * 500 / 1000 = 500.5 -> 500; 601 * 500 / 1000 = 300.5 -> 300
        assertEquals(List.of("damage 500 from A to B", "damage 300 from A to B"), ctx.calls);
    }

    // ---- E10, E13

    @Test
    void e10AsksTheTurnToDestroy() {
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        assertEquals(Outcome.NONE, apply(new DestroyEffect(), ctx, EffectFixtures.use("E10", Map.of(), t), t));
        assertEquals(List.of("destroy B"), ctx.calls);
    }

    @Test
    void e13AsksTheTurnToDiscard() {
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        assertEquals(Outcome.NONE, apply(new DiscardEffect(), ctx, EffectFixtures.use("E13", Map.of(), t), t));
        assertEquals(List.of("discard B"), ctx.calls);
    }

    // ---- E14, E15, E16, E18

    @Test
    void e14GrantsAKeywordAndE15TakesItAway() {
        Creature t = EffectFixtures.creature(1000, 3000);
        Map<String, Value> keyword = Map.of("keyword", new Value.StrV("K_PRIMO_COLPO"));
        assertEquals(Outcome.NONE, apply(new KeywordGrantEffect(), new FakeContext(),
                EffectFixtures.use("E14", keyword, t), t));
        assertTrue(t.has("K_PRIMO_COLPO"));
        assertEquals(Outcome.NONE, apply(new RemoveKeywordEffect(), new FakeContext(),
                EffectFixtures.use("E15", keyword, t), t));
        assertFalse(t.has("K_PRIMO_COLPO"));
    }

    @Test
    void e16GrantsNothingWhenTheCreatureAlreadyHasTheEvolution() {
        Creature t = EffectFixtures.creature(1000, 3000);
        FakeContext ctx = new FakeContext();
        ctx.hasAbility = true;
        Map<String, Value> evolution = Map.of("evolution_id", new Value.StrV("NOT_BOUND_AT_ALL"));
        assertEquals(Outcome.NONE, apply(new EvolutionGrantEffect(), ctx, EffectFixtures.use("E16", evolution, t), t));
        assertEquals(0, t.abilities().size());
    }

    @Test
    void e18GrantsAStaticCopyOfItself() {
        Creature t = EffectFixtures.creature(1000, 3000);
        Effect.Use use = EffectFixtures.use("E18", Map.of(), t);
        assertEquals(Outcome.NONE, apply(new SelfGrantEffect(), new FakeContext(), use, t));
        assertTrue(t.has(use.expansion().abilityId()));
    }

    // ---- E59

    @Test
    void e59CancelsTheAtkBonusAndReportsBothStats() {
        Creature t = EffectFixtures.creature(1000, 3000);
        t.addAtk(500);
        t.addDef(400); // a plain DEF addition is not a bonus Creature.cancelBonuses removes: DEF is reported unchanged
        Outcome o = apply(new CancelBonusesEffect(), new FakeContext(), EffectFixtures.use("E59", Map.of(), t), t);
        assertEquals(new Outcome(List.of(new StatChange(StatChange.Stat.ATK, 1500, 1000),
                new StatChange(StatChange.Stat.DEF, 3400, 3400)), OptionalInt.empty()), o);
    }
}
