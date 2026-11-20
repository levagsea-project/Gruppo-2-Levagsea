package quorma.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import quorma.gameengine.EngineVersion;
import quorma.gameengine.GameEngine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The identity the backend checks a replay against: it is the engine's own, not a copy that could drift, and it is what
 * the header of every MatchLog says.
 */
class EngineInfoControllerTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final GameEngine engine = new GameEngine();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new EngineInfoController(engine),
                new SimulationController(new SimulationService(engine))).setControllerAdvice(new EngineErrorHandler()).build();
    }

    @Test
    void reportsTheEngineVersionTheRulesetAndTheRulePack() throws Exception {
        mvc.perform(get("/engine/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.engineVersion").value(EngineVersion.VERSION))
                .andExpect(jsonPath("$.rulesetVersion").value(engine.rules().rulesetVersion()))
                .andExpect(jsonPath("$.rulesetHash").value(engine.rulesetHash()));
    }

    @Test
    void isWhatTheHeaderOfAMatchLogSays() throws Exception {
        String info = mvc.perform(get("/engine/info")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String input = new ClassPathResource("golden/input.json").getContentAsString(StandardCharsets.UTF_8);
        String log = mvc.perform(post("/v1/simulate").contentType(MediaType.APPLICATION_JSON).content(input))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode i = MAPPER.readTree(info);
        JsonNode header = MAPPER.readTree(log).get("header");
        assertEquals(header.get("engine_version").asString(), i.get("engineVersion").asString());
        assertEquals(header.get("ruleset_version").asString(), i.get("rulesetVersion").asString());
        assertEquals(header.get("ruleset_hash").asString(), i.get("rulesetHash").asString());
    }
}
