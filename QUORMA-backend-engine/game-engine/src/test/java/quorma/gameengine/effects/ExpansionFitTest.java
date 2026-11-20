package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.data.Value;
import quorma.gameengine.data.VocabEntry;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;

/**
 * An effect, a target, a trigger, a duration and a class must fit together, as {@code vocab.json} says: the binder
 * refuses at bind time what would otherwise crash in a match or be ignored in silence.
 */
class ExpansionFitTest {

    private static final AbilityBinder BINDER = EffectFixtures.BINDER;
    private static final Value.IntV N = new Value.IntV(100);
    private static final Map<String, Value> FLAT = Map.of("mode", new Value.StrV("flat"), "amount", N);
    private static final Map<String, Value> AMOUNT = Map.of("amount", N);

    private static ExpansionDef def(String trigger, ResClass cls, String effect, Map<String, Value> params,
            String target, String duration) {
        return new ExpansionDef(trigger, Map.of(), effect, params, target, Map.of(), duration, List.of(), cls);
    }

    private static void onCreature(ExpansionDef d) {
        BINDER.bind(new CreatureCard("P", 5, Suit.VINCOLO, Subfaction.ADATTATI, 1000, 3000,
                List.of(new InlineEffect(List.of(d)))));
    }

    private static void onSpell(ExpansionDef d) {
        BINDER.bindLasting(new SpellCard("SP", Faction.RESISTENZA_UMANA, List.of(new InlineEffect(List.of(d)))));
    }

    private static void onEquipment(ExpansionDef d) {
        BINDER.bindLasting(new EquipmentCard("EQ", Faction.RESISTENZA_UMANA, List.of(new InlineEffect(List.of(d)))));
    }

    private static String refusal(Runnable bind) {
        return assertThrows(AbilityException.class, bind::run).getMessage();
    }

    // ---- the kind of thing the target is

    @Test
    void anEffectOnAPlayerCannotAimAtCreaturesAndTheOtherWayRound() {
        String lifeOnCreatures = refusal(() -> onCreature(def("T19", ResClass.T, "E8", AMOUNT, "S9", "D_Istantanea")));
        assertTrue(lifeOnCreatures.contains("target S9 aims at [creature], but effect E8 needs [player]"), lifeOnCreatures);
        String damageOnAPlayer = refusal(() -> onCreature(def("T19", ResClass.T, "E7", AMOUNT, "S30", "D_Istantanea")));
        assertTrue(damageOnAPlayer.contains("target S30 aims at [player], but effect E7 needs [creature]"), damageOnAPlayer);
        assertDoesNotThrow(() -> onCreature(def("T19", ResClass.T, "E8", AMOUNT, "S30", "D_Istantanea")));
        assertDoesNotThrow(() -> onCreature(def("T19", ResClass.T, "E7", AMOUNT, "S1", "D_Istantanea")));
    }

    // ---- the duration

    @Test
    void anInstantEffectNeedsDIstantaneaAndALastingOneCannotHaveIt() {
        String instant = refusal(() -> onCreature(def("T19", ResClass.T, "E7", AMOUNT, "S1", "D_Permanente")));
        assertTrue(instant.contains("effect E7 happens at once: its duration must be D_Istantanea, got D_Permanente"), instant);
        String lasting = refusal(() -> onCreature(def("T19", ResClass.T, "E1", FLAT, "S1", "D_Istantanea")));
        assertTrue(lasting.contains("effect E1 lasts: its duration cannot be D_Istantanea"), lasting);
    }

    @Test
    void theDurationOfAnImmediateEffectIsNotCheckedBecauseTheEngineIgnoresIt() {
        // the base set writes D_Istantanea on an equipment's E2 and E3; they apply for good
        assertDoesNotThrow(() -> onEquipment(def(null, ResClass.I, "E2", FLAT, "S27", "D_Istantanea")));
        assertDoesNotThrow(() -> onEquipment(def(null, ResClass.I, "E3", AMOUNT, "S27", "D_Istantanea")));
    }

    // ---- the context a trigger provides

    @Test
    void aTriggeredEffectCanOnlyUseTheContextItsTriggerProvides() {
        String refused = refusal(() -> onCreature(def("T19", ResClass.T, "E1", FLAT, "S43", "D_Permanente")));
        assertTrue(refused.contains("target S43 needs the context 'declared_target', which trigger T19 does not provide"), refused);
        assertDoesNotThrow(() -> onCreature(def("T6", ResClass.T, "E1", FLAT, "S43", "D_Permanente")));
    }

    // ---- where a creature target is found

    @Test
    void aTriggeredEffectCannotAimAtATargetOnlyAContinuousEffectReaches() {
        String refused = refusal(() -> onCreature(def("T19", ResClass.T, "E1", FLAT, "S17", "D_Permanente")));
        assertTrue(refused.contains("target S17 is not found by a class T effect (it works in [C])"), refused);
    }

    @Test
    void aContinuousEffectCannotAimAtATargetOnlyATriggeredEffectChooses() {
        String refused = refusal(() -> onCreature(def("T0", ResClass.C, "E1", FLAT, "S7", "D_Permanente")));
        assertTrue(refused.contains("target S7 is not found by a class C effect (it works in [T])"), refused);
        String selection = refusal(() -> onCreature(def("T0", ResClass.C, "E1", FLAT, "S33", "D_Permanente")));
        assertTrue(selection.contains("target S33 is not found by a class C effect"), selection);
    }

    @Test
    void aSpellMayLeaveAContinuousEffectOnACreatureItChoosesButACreatureMayNot() {
        assertDoesNotThrow(() -> onSpell(def("T0", ResClass.C, "E1", FLAT, "S33", "D_FinoAllaFineDelTurno")));
        assertDoesNotThrow(() -> onSpell(def("T0", ResClass.C, "E1", FLAT, "S9", "D_FinoAllaFineDelTurno")));
        String refused = refusal(() -> onSpell(def("T0", ResClass.C, "E1", FLAT, "S7", "D_FinoAllaFineDelTurno")));
        assertTrue(refused.contains("target S7 is not found by a class C effect"), refused);
    }

    @Test
    void theNormalCasesStillBind() {
        assertDoesNotThrow(() -> onCreature(def("T19", ResClass.T, "E1", FLAT, "S1", "D_Permanente")));
        assertDoesNotThrow(() -> onCreature(def("T0", ResClass.C, "E1", FLAT, "S9", "D_Permanente")));
        assertDoesNotThrow(() -> onCreature(def("T19", ResClass.T, "E1", FLAT, "S9", "D_Permanente")));
    }

    // ---- a continuous E2

    @Test
    void aContinuousE2IsValidOnlyOnAnEquipmentAimedAtItsReceiver() {
        Map<String, Value> def = Map.of("mode", new Value.StrV("pct_base_def"), "permille", new Value.IntV(300));
        assertDoesNotThrow(() -> onEquipment(def("T0", ResClass.C, "E2", def, "S27", "D_FincheSorgenteInGioco")));
        for (String target : List.of("S1", "S27")) {
            String onCreature = refusal(() -> onCreature(def("T0", ResClass.C, "E2", def, target, "D_Permanente")));
            assertTrue(onCreature.contains("effect E2 is continuous (class C) only on an equipment, aimed at S27, got creature aimed at "
                    + target), onCreature);
        }
        String onSpell = refusal(() -> onSpell(def("T0", ResClass.C, "E2", def, "S9", "D_FinoAllaFineDelTurno")));
        assertTrue(onSpell.contains("got spell aimed at S9"), onSpell);
        String equipmentElsewhere = refusal(() -> onEquipment(def("T0", ResClass.C, "E2", def, "S1", "D_Permanente")));
        assertTrue(equipmentElsewhere.contains("got equipment aimed at S1"), equipmentElsewhere);
    }

    // ---- vocab.json and the Target classes say the same

    @Test
    void reachClassesInTheVocabularyAgreeWithWhatEachTargetClassImplements() throws Exception {
        var vocab = RulesetLoader.fromClasspath().vocab();
        for (Target t : Targets.all()) {
            VocabEntry entry = vocab.targets().get(t.id());
            Set<String> declared = new HashSet<>();
            if (entry.raw().get("reach_classes") instanceof Value.ListV l) {
                l.items().forEach(v -> declared.add(((Value.StrV) v).v()));
            }
            boolean reaches = t.getClass().getMethod("reaches", TargetView.class, Side.class, Creature.class, Side.class,
                    Creature.class).getDeclaringClass() != Target.class;
            boolean selects = t.getClass().getMethod("select", TargetView.class, Side.class, Creature.class,
                    TriggerContext.class, BoundExpansion.class).getDeclaringClass() != Target.class;
            assertEquals(reaches, declared.contains("C"), t.id() + ": reach_classes C and reaches()");
            assertEquals(selects, declared.contains("T"), t.id() + ": reach_classes T and select()");
        }
    }
}
