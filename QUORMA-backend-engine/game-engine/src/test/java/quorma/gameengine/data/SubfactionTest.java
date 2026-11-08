package quorma.gameengine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Subfaction matrix: {@code data/subfactions.json} and {@code balance.subfaction_advantage_permille}.
 * Expected values are copied by hand from ruleset 2.4 and 9.1, not read back from the engine.
 */
class SubfactionTest {

    private static final Ruleset RULES = RulesetLoader.fromClasspath();

    private static final List<String> RESISTENZA = List.of("Adattati", "Spezzasigilli", "Portanome", "Segnaterra");
    private static final List<String> POTENZE = List.of("SEKTAC", "MEROZYN", "EFFRIX", "YLMOR");

    /** Ruleset 2.4 table, column "Forte Contro". */
    private static final Map<String, String> STRONG = Map.of(
            "Adattati", "MEROZYN",
            "Spezzasigilli", "SEKTAC",
            "Portanome", "EFFRIX",
            "Segnaterra", "YLMOR",
            "SEKTAC", "Segnaterra",
            "MEROZYN", "Portanome",
            "EFFRIX", "Adattati",
            "YLMOR", "Spezzasigilli");

    /** Ruleset 2.4 table, column "Debole Contro". */
    private static final Map<String, String> WEAK = Map.of(
            "Adattati", "EFFRIX",
            "Spezzasigilli", "YLMOR",
            "Portanome", "MEROZYN",
            "Segnaterra", "SEKTAC",
            "SEKTAC", "Spezzasigilli",
            "MEROZYN", "Adattati",
            "EFFRIX", "Portanome",
            "YLMOR", "Segnaterra");

    private static List<String> all() {
        List<String> all = new java.util.ArrayList<>(RESISTENZA);
        all.addAll(POTENZE);
        return all;
    }

    @Test
    void theEightSubfactionsAreLoadedInFileOrder() {
        assertEquals(all().stream().sorted().toList(), RULES.subfactions().keySet().stream().sorted().toList());
        assertEquals(8, RULES.subfactions().size());
    }

    @Test
    void factionsAndRelationsMatchTheRulesetTable() {
        for (String name : all()) {
            SubfactionDef d = RULES.subfactions().get(name);
            assertEquals(name, d.name());
            assertEquals(RESISTENZA.contains(name) ? "Resistenza Umana" : "Potenze Artificiali", d.faction(), name);
            assertEquals(STRONG.get(name), d.strongAgainst(), name + " strong");
            assertEquals(WEAK.get(name), d.weakAgainst(), name + " weak");
        }
    }

    @Test
    void theTwoCyclesAreTaggedAsInTheLore() {
        for (String name : List.of("Spezzasigilli", "SEKTAC", "Segnaterra", "YLMOR")) {
            assertEquals("MONDO", RULES.subfactions().get(name).cycle(), name);
        }
        for (String name : List.of("EFFRIX", "Adattati", "MEROZYN", "Portanome")) {
            assertEquals("IDENTITA", RULES.subfactions().get(name).cycle(), name);
        }
    }

    @Test
    void strongAttackerGetsOnePointOneWeakAttackerNinePointZero() {
        assertEquals(1100, RULES.advantagePermille("Adattati", "MEROZYN"));
        assertEquals(900, RULES.advantagePermille("Adattati", "EFFRIX"));
        assertEquals(1100, RULES.advantagePermille("YLMOR", "Spezzasigilli"));
        assertEquals(900, RULES.advantagePermille("YLMOR", "Segnaterra"));
    }

    @Test
    void everyPairOfSubfactionsIsStrongWeakOrNeutral() {
        for (String a : all()) {
            for (String d : all()) {
                int expected = d.equals(STRONG.get(a)) ? 1100 : d.equals(WEAK.get(a)) ? 900 : 1000;
                assertEquals(expected, RULES.advantagePermille(a, d), a + " attacks " + d);
            }
        }
    }

    @Test
    void relationsAreReciprocal() {
        for (String a : all()) {
            // if A is strong against B, then B attacking A is weak, and the other way round
            assertEquals(900, RULES.advantagePermille(STRONG.get(a), a), a + " strong, so its victim is weak");
            assertEquals(1100, RULES.advantagePermille(WEAK.get(a), a), a + " weak, so its master is strong");
        }
    }

    @Test
    void sameFactionIsAlwaysNeutral() {
        for (List<String> faction : List.of(RESISTENZA, POTENZE)) {
            for (String a : faction) {
                for (String d : faction) {
                    assertEquals(Ruleset.NEUTRAL_PERMILLE, RULES.advantagePermille(a, d), a + " attacks " + d);
                }
            }
        }
    }

    @Test
    void unknownSubfactionFailsLoudly() {
        assertThrows(RulesetException.class, () -> RULES.advantagePermille("Nope", "Adattati"));
        assertThrows(RulesetException.class, () -> RULES.advantagePermille("Adattati", "Nope"));
    }
}
