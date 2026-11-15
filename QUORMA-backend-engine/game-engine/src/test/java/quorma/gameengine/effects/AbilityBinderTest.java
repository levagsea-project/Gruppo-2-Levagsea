package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.KeywordDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Subfaction;

/**
 * The expansion parser on the live data: each of the four keywords binds to the expansion of {@code keyword.json}
 * with its references resolved, and every problem with a card's abilities is refused with a message that names
 * the card and the ability.
 */
class AbilityBinderTest {

    private static final Ruleset RULES = RulesetLoader.fromClasspath();
    private static final AbilityBinder BINDER = new AbilityBinder(RULES);

    private static CreatureCard card(Ability... abilities) {
        return new CreatureCard("RU-VIN-A", 1, Suit.VINCOLO, Subfaction.ADATTATI, 4000, 7000, List.of(abilities));
    }

    private static AbilityRef ref(String id, Object... nameValue) {
        Map<String, Value> params = new LinkedHashMap<>();
        for (int i = 0; i < nameValue.length; i += 2) {
            Object v = nameValue[i + 1];
            params.put((String) nameValue[i], v instanceof Integer n ? new Value.IntV(n) : new Value.StrV((String) v));
        }
        return new AbilityRef(id, params);
    }

    private static BoundExpansion only(CreatureCard card) {
        List<BoundExpansion> bound = BINDER.bind(card);
        assertEquals(1, bound.size(), "one expansion expected, got " + bound);
        return bound.get(0);
    }

    private static String refused(CreatureCard card) {
        return assertThrows(AbilityException.class, () -> BINDER.bind(card)).getMessage();
    }

    // ---- the four live keywords ----

    @Test
    void scudoDefaultsToThePercentageVariant() {
        BoundExpansion b = only(card(ref("K_SCUDO", "x", 25)));
        assertEquals("K_SCUDO", b.abilityId());
        assertEquals("T0", b.triggerId());
        assertEquals("E28", b.effectId());
        assertEquals("S1", b.targetId());
        assertEquals(ResClass.C, b.cls());
        assertEquals(Optional.of("reduction_pct"), b.stringParam("mode"));
        assertEquals(25, b.intParam("pct"), "$param.x comes from the card");
        assertEquals(OptionalInt.empty(), b.maxStacks());
        assertEquals(Optional.empty(), b.stackKey());
    }

    @Test
    void scudoFlatPicksTheFlatVariant() {
        BoundExpansion b = only(card(ref("K_SCUDO", "x", 800, "kind", "flat")));
        assertEquals(Optional.of("flat"), b.stringParam("mode"));
        assertEquals(800, b.intParam("amount"));
        assertEquals(1, b.intParam("min_damage"));
    }

    @Test
    void insaziabileReadsBalanceAndStacks() {
        BoundExpansion b = only(card(ref("K_INSAZIABILE")));
        assertEquals("T19", b.triggerId());
        assertEquals("E1", b.effectId());
        assertEquals(ResClass.T, b.cls());
        assertEquals(Optional.of("pct_base_atk"), b.stringParam("mode"));
        assertEquals(150, b.intParam("permille"), "$tune.K_INSAZIABILE.permille from balance.json");
        assertEquals(OptionalInt.of(3), b.maxStacks(), "$tune resolved in common params too");
        assertEquals(Optional.of("K_INSAZIABILE"), b.stackKey());
    }

    @Test
    void aTierScaledValueOnTheCardWinsOverBalance() {
        // balance.json: K_INSAZIABILE.tier_scaled = [permille]; pool creation writes the scaled value on the card.
        BoundExpansion b = only(card(ref("K_INSAZIABILE", "permille", 180)));
        assertEquals(180, b.intParam("permille"));
    }

    @Test
    void traboccanteIsAReplacementOnSpill() {
        BoundExpansion b = only(card(ref("K_TRABOCCANTE")));
        assertEquals("T34", b.triggerId());
        assertEquals("E21", b.effectId());
        assertEquals(ResClass.R, b.cls());
        assertEquals("D_Istantanea", b.duration());
        assertEquals(1200, b.intParam("factor"));
    }

    @Test
    void perforanteReadsTheCard() {
        BoundExpansion b = only(card(ref("K_PERFORANTE", "x", 30)));
        assertEquals("E56", b.effectId());
        assertEquals(30, b.intParam("pct"));
    }

    @Test
    void abilitiesKeepCardOrder() {
        List<BoundExpansion> bound = BINDER.bind(card(
                ref("K_TRABOCCANTE"), ref("K_SCUDO", "x", 10), ref("K_INSAZIABILE")));
        assertEquals(List.of("K_TRABOCCANTE", "K_SCUDO", "K_INSAZIABILE"),
                bound.stream().map(BoundExpansion::abilityId).toList());
    }

    @Test
    void aCardWithNoAbilitiesBindsToNothing() {
        assertEquals(List.of(), BINDER.bind(card()));
    }

    // ---- refusals ----

    @Test
    void parkedOrUnknownAbilitiesAreRefused() {
        String msg = refused(card(ref("K_LETALE")));
        assertTrue(msg.contains("card RU-VIN-A K_LETALE: unknown ability"), msg);
        assertTrue(refused(card(ref("EVO_CRESCITA"))).contains("unknown ability"), "evolutions are not loaded");
    }

    @Test
    void cardParamsAreChecked() {
        assertTrue(refused(card(ref("K_SCUDO"))).contains("missing required param 'x'"));
        assertTrue(refused(card(ref("K_SCUDO", "x", 25, "y", 1))).contains("unknown param 'y'"));
        assertTrue(refused(card(ref("K_SCUDO", "x", "25"))).contains("param 'x' must be an integer"));
        assertTrue(refused(card(ref("K_SCUDO", "x", 25, "kind", "half"))).contains("must be one of [pct, flat]"));
        assertTrue(refused(card(ref("K_TRABOCCANTE", "factor", 1500))).contains("unknown param 'factor'"),
                "only declared or tier-scaled params may be written on the card");
        assertTrue(refused(card(ref("K_INSAZIABILE", "permille", "lots"))).contains("must be an integer"));
    }

    @Test
    void keywordValuesOutOfRangeAreRefused() {
        assertTrue(refused(card(ref("K_SCUDO", "x", 150))).contains("param 'pct' must be between 0 and 100, got 150"));
        assertTrue(refused(card(ref("K_PERFORANTE", "x", -5))).contains("param 'pct' must be between 0 and 100"));
        assertTrue(refused(card(ref("K_INSAZIABILE", "permille", -1))).contains("param 'permille' must be 0 or more"));
    }

    // ---- abilities written on the card (trigger, effect, target, no keyword) ----

    @Test
    void anInlineAbilityBindsLikeAKeywordAndIsNamedByItsPosition() {
        ExpansionDef shield = expansion("T0", "E28", ResClass.C, "D_Permanente", "mode", "reduction_pct", "pct", 40);
        List<BoundExpansion> bound = BINDER.bind(card(ref("K_PERFORANTE", "x", 30), new InlineEffect(List.of(shield))));
        assertEquals(List.of("K_PERFORANTE", "INLINE_2"), bound.stream().map(BoundExpansion::abilityId).toList());
        BoundExpansion b = bound.get(1);
        assertEquals("E28", b.effectId());
        assertEquals(40, b.intParam("pct"));
        assertEquals(Optional.empty(), b.stackKey());
    }

    @Test
    void anInlineAbilityCanHaveSeveralExpansions() {
        ExpansionDef pierce = expansion("T0", "E56", ResClass.C, "D_Permanente", "pct", 20);
        ExpansionDef spill = expansion("T34", "E21", ResClass.R, "D_Istantanea", "factor", 1500);
        List<BoundExpansion> bound = BINDER.bind(card(new InlineEffect(List.of(pierce, spill))));
        assertEquals(List.of("E56", "E21"), bound.stream().map(BoundExpansion::effectId).toList());
        assertEquals(List.of("INLINE_1", "INLINE_1"), bound.stream().map(BoundExpansion::abilityId).toList());
    }

    @Test
    void flatShieldGetsTheVocabDefaultMinimumDamage() {
        ExpansionDef flat = expansion("T0", "E28", ResClass.C, "D_Permanente", "mode", "flat", "amount", 500);
        assertEquals(1, only(card(new InlineEffect(List.of(flat)))).intParam("min_damage"));
    }

    @Test
    void inlineStacksDefaultToTheAbilityIdAndTuneStillResolves() {
        ExpansionDef grow = new ExpansionDef("T19", Map.of(), "E1",
                Map.of("mode", new Value.StrV("pct_base_atk"), "permille", new Value.Ref(Value.RefKind.TUNE, "K_INSAZIABILE.permille")),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.T, Map.of("max_stacks", new Value.IntV(2)));
        BoundExpansion b = only(card(new InlineEffect(List.of(grow))));
        assertEquals(150, b.intParam("permille"));
        assertEquals(OptionalInt.of(2), b.maxStacks());
        assertEquals(Optional.of("INLINE_1"), b.stackKey(), "vocab.common_params.stack_key defaults to the source id");
    }

    @Test
    void inlineAbilitiesAreCheckedLikeKeywords() {
        assertTrue(refusedInline(expansion("T0", "E26", ResClass.C, "D_Permanente"))
                .contains("card RU-VIN-A INLINE_1: effect E26 is not implemented"));
        assertTrue(refusedInline(expansion("T0", "E1", ResClass.C, "D_Permanente", "mode", "pct_base_atk", "permille", 100))
                .contains("effect E1 with trigger T0, class C and duration D_Permanente is not implemented by the engine yet "
                        + "(supported: T19/T/D_Permanente)"));
        assertTrue(refusedInline(expansion("T0", "E28", ResClass.T, "D_Permanente", "mode", "reduction_pct", "pct", 10))
                .contains("class T"), "the class must match the shape");
        assertTrue(refusedInline(expansion("T0", "E28", ResClass.C, "D_Permanente", "mode", "reduction_pct"))
                .contains("effect E28 mode reduction_pct needs param 'pct'"));
        assertTrue(refusedInline(expansion("T0", "E28", ResClass.C, "D_Permanente", "mode", "reduction_pct", "pct", 10, "amount", 5))
                .contains("does not take param 'amount'"));
        assertTrue(refusedInline(expansion("T0", "E56", ResClass.C, "D_Permanente", "pct", "lots"))
                .contains("param 'pct' must be an integer"));
        assertTrue(refusedInline(expansion("T34", "E21", ResClass.R, "D_Istantanea", "factor", -1))
                .contains("param 'factor' must be 0 or more"));
    }

    @Test
    void inlineAbilitiesHaveNoCardParamsAndNoConditions() {
        ExpansionDef param = new ExpansionDef("T0", Map.of(), "E56",
                Map.of("pct", new Value.Ref(Value.RefKind.PARAM, "x")),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.C);
        assertTrue(refusedInline(param).contains("cannot resolve $param.x"));
        ExpansionDef conditioned = new ExpansionDef("T0", Map.of(), "E56", Map.of("pct", new Value.IntV(10)),
                "S1", Map.of(), "D_Permanente",
                List.of(new ConditionDef("K_PARAM", Map.of("name", new Value.StrV("kind"), "equals", new Value.StrV("pct")))),
                ResClass.C);
        assertTrue(refusedInline(conditioned).contains("conditions on an ability written on the card are not implemented"));
    }

    private static ExpansionDef expansion(String trigger, String effect, ResClass cls, String duration, Object... nameValue) {
        Map<String, Value> params = new LinkedHashMap<>();
        for (int i = 0; i < nameValue.length; i += 2) {
            Object v = nameValue[i + 1];
            params.put((String) nameValue[i], v instanceof Integer n ? new Value.IntV(n) : new Value.StrV((String) v));
        }
        return new ExpansionDef(trigger, Map.of(), effect, params, "S1", Map.of(), duration, List.of(), cls);
    }

    private static String refusedInline(ExpansionDef expansion) {
        return refused(card(new InlineEffect(List.of(expansion))));
    }

    @Test
    void vocabularyTheEngineDoesNotImplementIsRefused() {
        ExpansionDef destroy = new ExpansionDef(
                "T0", Map.of(), "E26", Map.of(), "S1", Map.of(), "D_Permanente", List.of(), ResClass.C);
        String msg = refusedKeyword(destroy);
        assertTrue(msg.contains("card RU-VIN-A K_TEST: effect E26 is not implemented"), msg);

        ExpansionDef otherTrigger = new ExpansionDef("T2", Map.of(), "E1",
                Map.of("mode", new Value.StrV("pct_base_atk"), "permille", new Value.IntV(100)),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.T);
        assertTrue(refusedKeyword(otherTrigger).contains("trigger T2 is not implemented"));

        ExpansionDef otherMode = new ExpansionDef("T0", Map.of(), "E28",
                Map.of("mode", new Value.StrV("permille"), "permille", new Value.IntV(800)),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.C);
        assertTrue(refusedKeyword(otherMode).contains("effect E28 with mode permille is not implemented"));

        ExpansionDef onceATurn = new ExpansionDef("T19", Map.of(), "E1",
                Map.of("mode", new Value.StrV("pct_base_atk"), "permille", new Value.IntV(100)),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.T, Map.of("once_per_turn", new Value.BoolV(true)));
        assertTrue(refusedKeyword(onceATurn).contains("once_per_turn is not implemented"));
    }

    @Test
    void anUnresolvableReferenceIsRefused() {
        ExpansionDef e = new ExpansionDef("T0", Map.of(), "E56",
                Map.of("pct", new Value.Ref(Value.RefKind.TUNE, "K_TEST.nope")),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.C);
        assertTrue(refusedKeyword(e).contains("cannot resolve $tune.K_TEST.nope"));
    }

    /** Binds a card with keyword {@code K_TEST}, made of one expansion over the real ruleset; returns the refusal. */
    private static String refusedKeyword(ExpansionDef expansion) {
        KeywordDef k = new KeywordDef("K_TEST", "Test", expansion.cls(), "K_TEST", Map.of(), "", List.of(expansion));
        Ruleset rules = new Ruleset(RULES.vocab(), Map.of("K_TEST", k), RULES.subfactions(), RULES.balance());
        CreatureCard card = card(new AbilityRef("K_TEST"));
        return assertThrows(AbilityException.class, () -> new AbilityBinder(rules).bind(card)).getMessage();
    }
}
