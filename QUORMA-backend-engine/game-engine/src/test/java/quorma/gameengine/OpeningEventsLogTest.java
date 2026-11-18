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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The effects of Evaluate and Declare (an Evolution fired by being targeted, T9) are in the MatchLog, in the turn's
 * {@code opening} array, and the log still passes its schema.
 */
class OpeningEventsLogTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Schema LOG_SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of("classpath:schemas/match-log.schema.json"));

    private static String deck(String prefix, String faction, String subfaction, int creatures, String ability) {
        List<String> cards = new ArrayList<>();
        String[] suits = {"VINCOLO", "FRATTURA", "SPIRA", "VESTIGIO"};
        for (int i = 0; i < creatures; i++) {
            int rank = i % 13 + 1;
            String suit = suits[i / 13];
            cards.add("""
                    {"id": "%s-%s-%d", "kind": "CREATURE", "rank": %d, "suit": "%s", "subfaction": "%s",
                     "atk": %d, "def": %d, "abilities": [%s]}""".formatted(prefix, suit, rank, rank, suit, subfaction,
                    2500 + rank * 150, 4000 + rank * 250, ability));
        }
        for (int i = 0; cards.size() < 32; i++) {
            cards.add("{\"id\": \"%s-X%d\", \"kind\": \"%s\", \"faction\": \"%s\", \"abilities\": []}"
                    .formatted(prefix, i, i % 2 == 0 ? "SPELL" : "EQUIPMENT", faction));
        }
        return String.join(",\n", cards);
    }

    @Test
    void anEvolutionFiredByBeingTargetedIsLoggedInTheOpening() {
        String input = """
                {"schema_version": 1, "seed": 7, "ruleset_version": "1.0-alpha", "players": {
                  "A": {"faction": "RESISTENZA_UMANA", "pool_snapshot_id": "pool-a", "deck": [%s]},
                  "B": {"faction": "POTENZE_ARTIFICIALI", "deck": [%s]}}}"""
                .formatted(deck("RU", "RESISTENZA_UMANA", "PORTANOME", 21, "{\"id\": \"EVO_TRINCEA\"}"),
                        deck("PA", "POTENZE_ARTIFICIALI", "MEROZYN", 25, ""));

        String log = new GameEngine().matchLog(input);

        assertEquals(List.of(), LOG_SCHEMA.validate(MAPPER.readTree(log)).stream().map(Error::toString).toList());
        JsonNode firstTurn = MAPPER.readTree(log).get("turns").get(0);
        JsonNode opening = firstTurn.get("opening");
        assertTrue(opening != null && opening.size() >= 2, "an effect_triggered and an effect_resolved at least");
        assertEquals("effect_triggered", opening.get(0).get("type").asString());
        assertEquals("T9", opening.get(0).get("trigger_id").asString());
        assertEquals("EVO_TRINCEA", opening.get(0).get("ability_id").asString());
        // the triggers of every targeted creature go on the stack first, then they resolve last in, first out
        boolean resolved = false;
        for (JsonNode event : opening) {
            resolved |= event.get("type").asString().equals("effect_resolved");
        }
        assertTrue(resolved);
    }
}
