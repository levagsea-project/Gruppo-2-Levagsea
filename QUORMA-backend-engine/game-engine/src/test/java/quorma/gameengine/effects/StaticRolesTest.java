package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * What an effect does when it is a static one that the turn reads: the ATK amount and its {@code scale_by}, the
 * damage reduction, the abilities a static effect grants or removes, and the effects that apply once on an equipment.
 */
class StaticRolesTest {

    /** A view that knows one side's creatures and which of them attack; the rest is not needed here. */
    private static final class View implements TargetView {
        final List<Creature> creatures;
        final Set<Creature> attackers = new HashSet<>();

        View(List<Creature> creatures) {
            this.creatures = creatures;
        }

        @Override public List<Creature> creatures(Side side) { return creatures; }
        @Override public boolean attacker(Side side, Creature c) { return attackers.contains(c); }
        @Override public Optional<Creature> creatureAt(Side side, int lane) { throw new UnsupportedOperationException(); }
        @Override public List<Creature> everyone() { throw new UnsupportedOperationException(); }
        @Override public boolean adjacent(Side side, Creature a, Creature b) { throw new UnsupportedOperationException(); }
        @Override public List<Creature> neighbours(Side side, Creature c) { throw new UnsupportedOperationException(); }
        @Override public boolean inCombination(Side side, Creature c) { throw new UnsupportedOperationException(); }
        @Override public boolean filtersHold(List<String> f, Side controller, Side side, Creature c) { throw new UnsupportedOperationException(); }
        @Override public int factionRank(Creature c) { throw new UnsupportedOperationException(); }
        @Override public Optional<Creature> select(String id, Map<String, Value> p, Side controller) { throw new UnsupportedOperationException(); }
    }

    /** A layer that records what the effects did to it. */
    private static final class Layer implements AbilityLayer {
        final List<BoundExpansion> added = new ArrayList<>();
        final List<String> removed = new ArrayList<>();
        final Set<String> has = new HashSet<>();

        @Override public AbilityBinder binder() { return EffectFixtures.BINDER; }
        @Override public boolean has(String abilityId) { return has.contains(abilityId); }
        @Override public void add(List<BoundExpansion> expansions) { added.addAll(expansions); }
        @Override public void remove(String keywordId) { removed.add(keywordId); }
    }

    private static Value.IntV n(int v) {
        return new Value.IntV(v);
    }

    private static Value.StrV s(String v) {
        return new Value.StrV(v);
    }

    // ---- E1: the amount and scale_by of a static copy

    @Test
    void e1AmountIsFlatOrAShareOfBaseAtkTimesTheScale() {
        BoundExpansion flat = EffectFixtures.expansion("E1", Map.of("mode", s("flat"), "amount", n(200)));
        BoundExpansion share = EffectFixtures.expansion("E1", Map.of("mode", s("pct_base_atk"), "permille", n(100)));
        assertEquals(600, AtkEffect.amount(flat, 5000, 3));
        assertEquals(200, AtkEffect.amount(share, 1001, 2), "1001 * 100 / 1000 = 100 (floored), times 2");
        assertEquals(-200, AtkEffect.amount(EffectFixtures.expansion("E1", Map.of("mode", s("flat"), "amount", n(-200))), 5000, 1));
    }

    @Test
    void e1ScaleByCountsWhatItNamesAndIsOneWithout() {
        Creature a = EffectFixtures.creature(1000, 3000);
        Creature b = EffectFixtures.creature(1000, 3000);
        Creature dead = EffectFixtures.creature(1000, 3000);
        dead.markDestroyed();
        View view = new View(List.of(a, b, dead));
        view.attackers.add(a);

        assertEquals(1, AtkEffect.scale(view, Side.A, scaled(null)));
        assertEquals(1, AtkEffect.scale(view, Side.A, scaled("non_attackers")), "only b does not attack; dead does not count");
        assertEquals(2, AtkEffect.scale(view, Side.A, scaled("creatures_in_play")));
        assertEquals(2, AtkEffect.scale(view, Side.A, scaled("own_empty_lanes")), "5 lanes, 3 creatures");
        assertEquals(0, AtkEffect.scale(view, Side.A, scaled("own_evolutions_triggered")));
    }

    private static BoundExpansion scaled(String by) {
        return by == null ? EffectFixtures.staticExpansion("E1", Map.of("mode", s("flat"), "amount", n(100)))
                : EffectFixtures.staticExpansion("E1", Map.of("mode", s("flat"), "amount", n(100), "scale_by", s(by)));
    }

    @Test
    void e1ScaleByIsRefusedOutsideAContinuousEffect() {
        Map<String, Value> params = Map.of("mode", s("flat"), "amount", n(100), "scale_by", s("non_attackers"));
        AbilityException triggered = assertThrows(AbilityException.class, () -> EffectFixtures.expansion("E1", params));
        assertTrue(triggered.getMessage().contains("effect E1 takes param 'scale_by' only in a continuous (class C) effect, got class T"),
                triggered.getMessage());
        assertEquals(1, EffectFixtures.staticExpansion("E1", params).effectParams().size() - 2, "class C binds it");
    }

    // ---- E28: the reduction

    @Test
    void e28ReadsAFlatOrAPercentageReduction() {
        BoundExpansion flat = EffectFixtures.expansion("E28", Map.of("mode", s("flat"), "amount", n(500), "min_damage", n(1)));
        BoundExpansion pct = EffectFixtures.expansion("E28", Map.of("mode", s("reduction_pct"), "pct", n(20)));
        assertEquals(new DamageReductionEffect.Reduction(true, 500, 1), DamageReductionEffect.reduction(flat));
        assertEquals(new DamageReductionEffect.Reduction(false, 20, 0), DamageReductionEffect.reduction(pct));
    }

    // ---- E14, E15, E16: the abilities a static effect changes

    @Test
    void e14AddsTheGrantedKeywordToTheLayer() {
        Layer layer = new Layer();
        BoundExpansion e = EffectFixtures.expansion("E14", Map.of("keyword", s("K_PRIMO_COLPO")));
        new KeywordGrantEffect().layer(layer, e);
        assertTrue(layer.added.stream().allMatch(x -> x.abilityId().equals("K_PRIMO_COLPO")));
        assertFalse(layer.added.isEmpty());
        assertEquals(List.of(), layer.removed);
    }

    @Test
    void e15TakesTheKeywordAwayFromTheLayer() {
        Layer layer = new Layer();
        new RemoveKeywordEffect().layer(layer, EffectFixtures.expansion("E15", Map.of("keyword", s("K_PRIMO_COLPO"))));
        assertEquals(List.of("K_PRIMO_COLPO"), layer.removed);
        assertEquals(List.of(), layer.added);
    }

    @Test
    void e16AddsTheEvolutionOnlyIfTheCreatureDoesNotHaveIt() {
        BoundExpansion e = EffectFixtures.expansion("E16", Map.of("evolution_id", s("EVO_CRESCITA")));
        Layer without = new Layer();
        new EvolutionGrantEffect().layer(without, e);
        assertFalse(without.added.isEmpty());
        assertTrue(without.added.stream().allMatch(x -> x.abilityId().equals("EVO_CRESCITA")));

        Layer with = new Layer();
        with.has.add("EVO_CRESCITA");
        new EvolutionGrantEffect().layer(with, e);
        assertEquals(List.of(), with.added);
    }

    @Test
    void theThreeAbilityEffectsAreFoundAsAbilityLayerEffects() {
        for (String id : List.of("E14", "E15", "E16")) {
            assertTrue(EffectDefinitions.find(id).orElseThrow() instanceof AbilityLayerEffect, id);
        }
        assertFalse(EffectDefinitions.find("E1").orElseThrow() instanceof AbilityLayerEffect);
    }

    // ---- E2: applied once on an equipment

    @Test
    void onlyE2IsAppliedOnceWhenItIsAStaticEffectOfAnEquipment() {
        for (Effect effect : Effects.all()) {
            assertEquals(effect.id().equals("E2"), effect.appliedOnceWhenStatic(), effect.id());
        }
    }
}
