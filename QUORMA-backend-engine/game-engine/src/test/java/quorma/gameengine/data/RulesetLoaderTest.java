package quorma.gameengine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RulesetLoaderTest {

    private static final Ruleset RULES = RulesetLoader.fromClasspath();

    @Test
    void scudoIsMappedToRecords() {
        KeywordDef scudo = RULES.keywords().get("K_SCUDO");
        assertEquals("Scudo", scudo.name());
        assertEquals(ResClass.C, scudo.cls());
        assertTrue(scudo.params().get("x").required());
        assertEquals(new Value.StrV("pct"), scudo.params().get("kind").defaultValue());

        assertEquals(2, scudo.expandsTo().size());
        ExpansionDef pct = scudo.expandsTo().get(0);
        assertEquals("E28", pct.effectId());
        assertEquals("D_Permanente", pct.duration());
        assertEquals(new Value.Ref(Value.RefKind.PARAM, "x"), pct.effectParams().get("pct"));
        assertEquals("K_PARAM", pct.conditions().get(0).id());
    }

    @Test
    void commonParamsAreKeptOnTheExpansion() {
        ExpansionDef insaziabile = RULES.keywords().get("K_INSAZIABILE").expandsTo().get(0);
        assertEquals(java.util.List.of("max_stacks", "stack_key"), java.util.List.copyOf(insaziabile.common().keySet()));
        assertEquals(new Value.Ref(Value.RefKind.TUNE, "K_INSAZIABILE.max_stacks"), insaziabile.common().get("max_stacks"));
        assertEquals(new Value.StrV("K_INSAZIABILE"), insaziabile.common().get("stack_key"));
        assertEquals(Map.of(), RULES.keywords().get("K_SCUDO").expandsTo().get(0).common());
    }

    @Test
    void everyKeywordExpansionPointsAtKnownVocab() {
        Vocab v = RULES.vocab();
        for (KeywordDef k : RULES.keywords().values()) {
            for (ExpansionDef e : k.expandsTo()) {
                assertTrue(v.triggers().containsKey(e.triggerId()), k.id() + " trigger " + e.triggerId());
                assertTrue(v.effects().containsKey(e.effectId()), k.id() + " effect " + e.effectId());
                assertTrue(v.targets().containsKey(e.targetId()), k.id() + " target " + e.targetId());
            }
        }
    }

    @Test
    void tuneReadsBalanceJson() {
        // balance.json keywords.K_SCUDO.params.x is a number: only the lookup order matters here.
        Optional<Value> fromBalance = RULES.tune("K_SCUDO", "x", Map.of());
        assertTrue(fromBalance.isPresent());
        assertEquals(Value.IntV.class, fromBalance.get().getClass());
        // the card instance params win over balance.json (lookup order, not the value)
        Value.IntV instanceParam = new Value.IntV(15);
        Optional<Value> tuned = RULES.tune("K_SCUDO", "x", Map.of("x", instanceParam));
        assertTrue(tuned.isPresent());
        assertEquals(Value.IntV.class, tuned.get().getClass());
        assertEquals(instanceParam, tuned.get());
        assertEquals(Optional.empty(), RULES.tune("K_SCUDO", "nope", Map.of()));
    }

    @Test
    void loadingTwiceGivesEqualRecords() {
        assertEquals(RULES.keywords(), RulesetLoader.fromClasspath().keywords());
    }

    @Test
    void directoryLoadPicksUpEditedFiles(@TempDir Path dir) throws Exception {
        write(dir, "vocab.json", "{\"effects\":{},\"triggers\":{},\"targets\":{},\"conditions\":{},\"durations\":{}}");
        write(dir, "keyword.json", "{}");
        write(dir, "subfactions.json", "{\"subfactions\":{}}");
        write(dir, "balance.json", "{\"globals\":{\"mitigation_floor_permille\":650}}");
        assertEquals(Optional.of(new Value.IntV(650)), RulesetLoader.fromDirectory(dir).global("mitigation_floor_permille"));

        write(dir, "balance.json", "{\"globals\":{\"mitigation_floor_permille\":800}}");
        assertEquals(Optional.of(new Value.IntV(800)), RulesetLoader.fromDirectory(dir).global("mitigation_floor_permille"));
    }

    @Test
    void duplicateKeyFails(@TempDir Path dir) throws Exception {
        write(dir, "vocab.json", "{\"effects\":{},\"triggers\":{},\"targets\":{},\"conditions\":{},\"durations\":{}}");
        write(dir, "keyword.json", "{}");
        write(dir, "subfactions.json", "{\"subfactions\":{}}");
        write(dir, "balance.json", "{\"a\":1,\"a\":2}");
        assertThrows(RulesetException.class, () -> RulesetLoader.fromDirectory(dir));
    }

    @Test
    void missingRequiredFieldReportsPath(@TempDir Path dir) throws Exception {
        write(dir, "vocab.json", "{\"effects\":{},\"triggers\":{},\"targets\":{},\"conditions\":{},\"durations\":{}}");
        write(dir, "keyword.json", "{\"K_X\":{\"name\":\"X\",\"class\":\"C\"}}");
        write(dir, "subfactions.json", "{\"subfactions\":{}}");
        write(dir, "balance.json", "{}");
        RulesetException e = assertThrows(RulesetException.class, () -> RulesetLoader.fromDirectory(dir));
        assertTrue(e.getMessage().contains("keyword.json#K_X"), e.getMessage());
        assertTrue(e.getMessage().contains("expands_to"), e.getMessage());
    }

    @Test
    void fractionsAreKeptAsTextNotDouble() {
        Value v = JsonReader.parse("{\"a\":0.12}".getBytes(StandardCharsets.UTF_8), "t.json");
        assertEquals(new Value.DecV("0.12"), ((Value.MapV) v).get("a"));
    }

    @Test
    void synergyTuneReadsBalanceJson() {
        // balance.json synergies.VINCOLO_3.atk is number.
        Optional<Value> fromBalance = RULES.synergyTune("VINCOLO_3", "atk");
        assertTrue(fromBalance.isPresent());
        assertEquals(Value.IntV.class, fromBalance.get().getClass());
        assertEquals(Optional.empty(), RULES.synergyTune("VINCOLO_3", "nope"));
        Optional<Value> fromBalance2 = RULES.synergyTune("NOPE", "atk");
        assertEquals(Optional.empty(), fromBalance2);
    }

    private static void write(Path dir, String name, String content) throws Exception {
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }
}
