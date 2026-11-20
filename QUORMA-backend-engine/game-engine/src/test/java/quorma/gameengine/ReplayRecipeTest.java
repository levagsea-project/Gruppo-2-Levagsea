package quorma.gameengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import quorma.gameengine.input.MatchInputException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * A replay is the recipe of a match played again: the same input on the engine that played it. The match log names that
 * engine and rule pack in its header, and an input that sends them gets the engine's own refusal if it is not that
 * engine, so a replay cannot quietly produce another match.
 */
class ReplayRecipeTest {

    private static final GameEngine ENGINE = new GameEngine();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static ObjectNode golden() throws IOException {
        return (ObjectNode) MAPPER.readTree(Files.readString(
                Path.of("src", "test", "resources", "golden", "all-four-keywords", "input.json"), StandardCharsets.UTF_8));
    }

    @Test
    void anInputBuiltFromTheHeaderOfALogPlaysToTheSameBytes() throws IOException {
        ObjectNode input = golden();
        String log = ENGINE.matchLog(MAPPER.writeValueAsString(input));
        JsonNode header = MAPPER.readTree(log).get("header");

        ObjectNode replay = input.deepCopy();
        replay.put("engine_version", header.get("engine_version").asString());
        replay.put("ruleset_version", header.get("ruleset_version").asString());
        replay.put("ruleset_hash", header.get("ruleset_hash").asString());

        assertEquals(log, ENGINE.matchLog(MAPPER.writeValueAsString(replay)));
        assertEquals(EngineVersion.VERSION, header.get("engine_version").asString());
        assertEquals(ENGINE.rulesetHash(), header.get("ruleset_hash").asString());
    }

    @Test
    void anotherEngineVersionIsRefusedClearly() throws IOException {
        ObjectNode input = golden();
        input.put("engine_version", "0.0.1");
        MatchInputException e = assertThrows(MatchInputException.class, () -> ENGINE.matchLog(MAPPER.writeValueAsString(input)));
        assertTrue(e.getMessage().startsWith("input.engine_version:"), e.getMessage());
        assertTrue(e.getMessage().contains("this engine is " + EngineVersion.VERSION), e.getMessage());
        assertTrue(e.getMessage().contains("played by 0.0.1"), e.getMessage());
    }

    @Test
    void anotherRulePackIsRefusedClearly() throws IOException {
        ObjectNode input = golden();
        input.put("ruleset_hash", "0".repeat(64));
        MatchInputException e = assertThrows(MatchInputException.class, () -> ENGINE.matchLog(MAPPER.writeValueAsString(input)));
        assertTrue(e.getMessage().startsWith("input.ruleset_hash:"), e.getMessage());
    }

    @Test
    void theEngineVersionIsOptionalForANewMatch() throws IOException {
        ObjectNode input = golden();
        assertTrue(!input.has("engine_version"));
        ENGINE.matchLog(MAPPER.writeValueAsString(input));
    }
}
