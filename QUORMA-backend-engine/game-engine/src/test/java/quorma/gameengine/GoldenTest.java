package quorma.gameengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Golden files ({@code src/test/resources/golden/<scenario>/}): {@code input.json} must give exactly the bytes of
 * {@code expected-log.json}. The expected logs are computed by hand, never from the engine's output (see
 * {@code game-engine/AGENTS.md}); a golden file changes only when a rule changes on purpose.
 */
class GoldenTest {

    private static final Path GOLDEN = Path.of("src", "test", "resources", "golden");
    private static final GameEngine ENGINE = new GameEngine();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final SchemaRegistry SCHEMAS = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private static final Schema INPUT = SCHEMAS.getSchema(SchemaLocation.of("classpath:schemas/match-input.schema.json"));
    private static final Schema LOG = SCHEMAS.getSchema(SchemaLocation.of("classpath:schemas/match-log.schema.json"));

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> errors(Schema schema, String json) {
        return schema.validate(MAPPER.readTree(json)).stream().map(Error::toString).toList();
    }

    @TestFactory
    Stream<DynamicTest> everyScenarioGivesItsExpectedLog() throws IOException {
        List<Path> scenarios;
        try (Stream<Path> dirs = Files.list(GOLDEN)) {
            scenarios = dirs.filter(Files::isDirectory).sorted().toList();
        }
        assertTrue(scenarios.size() >= 3, "golden scenarios found: " + scenarios);
        return scenarios.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> {
            String input = read(dir.resolve("input.json"));
            String expected = read(dir.resolve("expected-log.json"));
            assertEquals(List.of(), errors(INPUT, input), "input.json against its schema");
            assertEquals(List.of(), errors(LOG, expected), "expected-log.json against its schema");
            assertEquals(expected, ENGINE.matchLog(input));
        }));
    }

    @TestFactory
    Stream<DynamicTest> theFirstScenarioIsTheSchemaExample() {
        return Stream.of(DynamicTest.dynamicTest("all-four-keywords", () -> assertEquals(
                read(Path.of("src", "main", "resources", "schemas", "match-log.example.json")),
                read(GOLDEN.resolve("all-four-keywords").resolve("expected-log.json")),
                "the published example and its golden file must not drift apart")));
    }
}
