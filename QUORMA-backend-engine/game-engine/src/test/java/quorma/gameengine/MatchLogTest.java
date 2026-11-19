package quorma.gameengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.JsonReader;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.data.Value;
import quorma.gameengine.input.MatchInputException;
import quorma.gameengine.json.CanonicalJson;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The MatchLog JSON: the hand-made example is reproduced byte for byte, every log passes the JSON Schema, both
 * hashes can be recomputed, and the input's {@code ruleset_hash} is checked.
 */
class MatchLogTest {

    private static final GameEngine ENGINE = new GameEngine();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Schema LOG_SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of("classpath:schemas/match-log.schema.json"));
    private static final Schema INPUT_SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of("classpath:schemas/match-input.schema.json"));

    private static String resource(String path) {
        try (InputStream in = MatchLogTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> schemaErrors(Schema schema, String json) {
        return schema.validate(MAPPER.readTree(json)).stream().map(Error::toString).toList();
    }

    /** The input of {@code match-log.example.json}, rebuilt from its header. */
    private static String exampleInput() {
        JsonNode header = MAPPER.readTree(resource("/schemas/match-log.example.json")).get("header");
        ObjectNode input = MAPPER.createObjectNode();
        input.put("schema_version", 1);
        input.put("seed", header.get("seed").asLong());
        input.put("ruleset_version", header.get("ruleset_version").asString());
        ObjectNode players = input.putObject("players");
        for (String side : List.of("A", "B")) {
            ObjectNode p = players.putObject(side);
            p.set("faction", header.get("players").get(side).get("faction"));
            p.set("deck", header.get("players").get(side).get("deck"));
        }
        return MAPPER.writeValueAsString(input);
    }

    @Test
    void theExampleLogIsReproducedByteForByte() {
        assertEquals(resource("/schemas/match-log.example.json"), ENGINE.matchLog(exampleInput()),
                "the example was computed by hand (step 1), its hashes by Python: same bytes, same hashes");
    }

    @Test
    void theRulesetHashCoversTheSixRuleFilesAndIsTheSameFromTheJarAndFromData() {
        assertEquals(RulesetLoader.rulesetHashFromDirectory(Path.of("..", "data")), ENGINE.rulesetHash());
        assertTrue(ENGINE.rulesetHash().matches("[0-9a-f]{64}"));
    }

    @Test
    void theResultHashIsTheHashOfTheLogWithoutIt() {
        String log = ENGINE.matchLog(exampleInput());
        Value.MapV parsed = (Value.MapV) JsonReader.parse(log.getBytes(StandardCharsets.UTF_8), "log");
        @SuppressWarnings("unchecked")
        Map<String, Object> plain = new LinkedHashMap<>((Map<String, Object>) Value.toJson(parsed));
        String hash = (String) plain.remove("result_hash");
        assertEquals(CanonicalJson.sha256(plain), hash);
        assertEquals(log, CanonicalJson.write(Value.toJson(parsed)), "parsing and writing again changes nothing");
    }

    @Test
    void theExampleFilesPassTheirSchemas() {
        assertEquals(List.of(), schemaErrors(LOG_SCHEMA, resource("/schemas/match-log.example.json")));
        assertEquals(List.of(), schemaErrors(INPUT_SCHEMA, exampleInput()));
    }

    /**
     * A 32-card deck: creatures of every rank of two suits with the four keywords and an inline ability spread
     * over them, plus blank spells and equipment.
     */
    private static String deck(String prefix, String faction, String subfaction, String suit1, String suit2, int creatures) {
        List<String> abilities = List.of(
                "",
                "{\"id\": \"K_SCUDO\", \"params\": {\"x\": 20}}",
                "{\"id\": \"K_PERFORANTE\", \"params\": {\"x\": 30}}",
                "{\"id\": \"K_INSAZIABILE\"}",
                "{\"id\": \"K_TRABOCCANTE\"}",
                "{\"id\": \"K_SCUDO\", \"params\": {\"x\": 600, \"kind\": \"flat\"}}",
                "{\"expansions\": [{\"trigger_id\": \"T0\", \"effect_id\": \"E28\", \"effect_params\": "
                        + "{\"mode\": \"reduction_pct\", \"pct\": 10}, \"target_id\": \"S1\", \"duration\": \"D_Permanente\", \"class\": \"C\"}]}");
        List<String> cards = new ArrayList<>();
        for (int i = 0; i < creatures; i++) {
            int rank = i % 13 + 1;
            String suit = i < 13 ? suit1 : suit2;
            cards.add("""
                    {"id": "%s-%s-%d", "kind": "CREATURE", "rank": %d, "suit": "%s", "subfaction": "%s",
                     "atk": %d, "def": %d, "abilities": [%s]}""".formatted(prefix, suit, rank, rank, suit, subfaction,
                    2500 + rank * 150, 4000 + rank * 250, abilities.get(i % abilities.size())));
        }
        for (int i = 0; cards.size() < 32; i++) {
            cards.add("{\"id\": \"%s-X%d\", \"kind\": \"%s\", \"faction\": \"%s\", \"abilities\": []}"
                    .formatted(prefix, i, i % 2 == 0 ? "SPELL" : "EQUIPMENT", faction));
        }
        return String.join(",\n", cards);
    }

    private static String fullInput(long seed) {
        return """
                {"schema_version": 1, "seed": %d, "ruleset_version": "1.0-alpha", "players": {
                  "A": {"faction": "RESISTENZA_UMANA", "pool_snapshot_id": "pool-a", "deck": [%s]},
                  "B": {"faction": "POTENZE_ARTIFICIALI", "deck": [%s]}}}"""
                .formatted(seed, deck("RU", "RESISTENZA_UMANA", "PORTANOME", "VINCOLO", "FRATTURA", 21),
                        deck("PA", "POTENZE_ARTIFICIALI", "MEROZYN", "SPIRA", "VESTIGIO", 25));
    }

    @Test
    void fullMatchesPassTheSchemaAndRepeatExactly() {
        for (long seed : new long[] {1, 2, 3, 42, -9_000_000_000L}) {
            String input = fullInput(seed);
            assertEquals(List.of(), schemaErrors(INPUT_SCHEMA, input), "input, seed " + seed);
            String log = ENGINE.matchLog(input);
            assertEquals(List.of(), schemaErrors(LOG_SCHEMA, log), "log, seed " + seed);
            assertEquals(log, ENGINE.matchLog(input), "same input, same bytes (seed " + seed + ")");
            assertTrue(log.endsWith("}\n") && !log.contains("\r"), "LF and a trailing newline");
        }
    }

    @Test
    void totalsAreTheSumsOfTheEvents() {
        JsonNode log = MAPPER.readTree(ENGINE.matchLog(fullInput(7)));
        for (String side : List.of("A", "B")) {
            long damage = 0;
            long spill = 0;
            long kills = 0;
            long lost = 0;
            for (JsonNode turn : log.get("turns")) {
                for (String phase : List.of("cycle_1", "cycle_2")) {
                    for (JsonNode e : turn.get(phase)) {
                        String type = e.get("type").asString();
                        if (type.equals("attack_resolved") && e.get("attacker").get("side").asString().equals(side)) {
                            damage += e.get("damage").asLong();
                            spill += e.get("spill").asLong();
                            kills += e.get("killed").asBoolean() ? 1 : 0;
                        }
                        if (type.equals("creature_destroyed") && e.get("creature").get("side").asString().equals(side)) {
                            lost++;
                        }
                    }
                }
            }
            JsonNode totals = log.get("totals").get("players").get(side);
            assertEquals(damage, totals.get("damage_dealt").asLong(), side);
            assertEquals(spill, totals.get("spill_damage").asLong(), side);
            assertEquals(kills, totals.get("kills").asLong(), side);
            assertEquals(lost, totals.get("creatures_lost").asLong(), side);
            long cardDamage = 0;
            for (JsonNode c : log.get("totals").get("cards")) {
                if (c.get("side").asString().equals(side)) {
                    cardDamage += c.get("damage_dealt").asLong();
                }
            }
            assertEquals(damage, cardDamage, "the cards' damage adds up to the player's, " + side);
        }
        assertEquals(64, log.get("totals").get("cards").size(), "one entry per card of both decks");
        assertEquals(log.get("result").get("life"), MAPPER.readTree(
                "{\"A\": " + lastBoard(log, "A") + ", \"B\": " + lastBoard(log, "B") + "}"));
    }

    private static long lastBoard(JsonNode log, String side) {
        JsonNode turns = log.get("turns");
        return turns.get(turns.size() - 1).get("board").get(side).get("life").asLong();
    }

    @Test
    void theSchemaRejectsABrokenLog() {
        String log = ENGINE.matchLog(exampleInput());
        assertFalse(schemaErrors(LOG_SCHEMA, log.replace("\"prng\": \"SplitMix64\"", "\"prng\": \"Random\"")).isEmpty());
        assertFalse(schemaErrors(LOG_SCHEMA, log.replace("\"damage\": 3300", "\"damage\": 3300.5")).isEmpty());
        String extraKey = log.replace("\"stack_size\": 1", "\"stack_size\": 1, \"extra\": 0");
        assertFalse(extraKey.equals(log), "the replacement must hit");
        assertFalse(schemaErrors(LOG_SCHEMA, extraKey).isEmpty());
    }

    @Test
    void theInputRulesetHashMustMatch() {
        String input = exampleInput();
        String withHash = input.replace("\"ruleset_version\"", "\"ruleset_hash\":\"" + ENGINE.rulesetHash() + "\",\"ruleset_version\"");
        assertEquals(ENGINE.matchLog(input), ENGINE.matchLog(withHash), "the same rules: accepted, same log");
        String other = input.replace("\"ruleset_version\"", "\"ruleset_hash\":\"" + "0".repeat(64) + "\",\"ruleset_version\"");
        String msg = assertThrows(MatchInputException.class, () -> ENGINE.simulate(other)).getMessage();
        assertTrue(msg.startsWith("input.ruleset_hash: this engine plays the rule pack " + ENGINE.rulesetHash()), msg);
    }

    @Test
    void theEngineKnowsItsVersion() {
        assertTrue(EngineVersion.VERSION.matches("\\d+\\.\\d+\\.\\d+.*"), EngineVersion.VERSION);
    }
}
