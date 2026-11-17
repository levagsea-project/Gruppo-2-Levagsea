package quorma.gameengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * A whole match with spells that heal (written on the card as expansions with no trigger): the input is accepted, the
 * log passes its schema and repeats exactly, and a spell drawn in Refill shows up as {@code card_effect_resolved}.
 */
class SpellsInMatchLogTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Schema LOG_SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of("classpath:schemas/match-log.schema.json"));
    private static final Schema INPUT_SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of("classpath:schemas/match-input.schema.json"));

    private static final String HEAL = """
            {"expansions": [{"effect_id": "E6", "effect_params": {"mode": "flat", "amount": 1400},
              "target_id": "S9", "duration": "D_Istantanea", "class": "I"}]}""";

    private static String deck(String prefix, String faction, String subfaction, int creatures) {
        List<String> cards = new ArrayList<>();
        String[] suits = {"VINCOLO", "FRATTURA", "SPIRA", "VESTIGIO"};
        for (int i = 0; i < creatures; i++) {
            int rank = i % 13 + 1;
            String suit = suits[i / 13];
            cards.add("""
                    {"id": "%s-%s-%d", "kind": "CREATURE", "rank": %d, "suit": "%s", "subfaction": "%s",
                     "atk": %d, "def": %d, "abilities": []}""".formatted(prefix, suit, rank, rank, suit, subfaction,
                    2500 + rank * 150, 4000 + rank * 250));
        }
        for (int i = 0; cards.size() < 32; i++) {
            cards.add("{\"id\": \"%s-S%d\", \"kind\": \"SPELL\", \"faction\": \"%s\", \"abilities\": [%s]}"
                    .formatted(prefix, i, faction, HEAL));
        }
        return String.join(",\n", cards);
    }

    private static String input(long seed) {
        return """
                {"schema_version": 1, "seed": %d, "ruleset_version": "1.0-alpha", "players": {
                  "A": {"faction": "RESISTENZA_UMANA", "pool_snapshot_id": "pool-a", "deck": [%s]},
                  "B": {"faction": "POTENZE_ARTIFICIALI", "deck": [%s]}}}"""
                .formatted(seed, deck("RU", "RESISTENZA_UMANA", "PORTANOME", 21),
                        deck("PA", "POTENZE_ARTIFICIALI", "MEROZYN", 25));
    }

    @Test
    void healingSpellsPlayAndAreLogged() {
        GameEngine engine = new GameEngine();
        boolean seen = false;
        for (long seed = 1; seed <= 10; seed++) {
            String in = input(seed);
            assertEquals(List.of(), INPUT_SCHEMA.validate(MAPPER.readTree(in)).stream().map(Error::toString).toList());
            String log = engine.matchLog(in);
            assertEquals(List.of(), LOG_SCHEMA.validate(MAPPER.readTree(log)).stream().map(Error::toString).toList(),
                    "seed " + seed);
            assertEquals(log, engine.matchLog(in), "same input, same bytes (seed " + seed + ")");
            seen |= log.contains("\"card_effect_resolved\"");
        }
        assertTrue(seen, "a spell was drawn in Refill in at least one of the matches");
    }
}
