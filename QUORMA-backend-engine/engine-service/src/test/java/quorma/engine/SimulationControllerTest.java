package quorma.engine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import quorma.gameengine.GameEngine;

/** Runs the real engine behind the real controller; the golden pair is a copy of game-engine's all-four-keywords. */
class SimulationControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SimulationService service = new SimulationService(new GameEngine());
        mvc = MockMvcBuilders.standaloneSetup(new SimulationController(service))
                .setControllerAdvice(new EngineErrorHandler())
                .build();
    }

    private static String resource(String name) throws IOException {
        return new ClassPathResource("golden/" + name).getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsTheCanonicalMatchLogVerbatim() throws Exception {
        mvc.perform(post("/v1/simulate").contentType(MediaType.APPLICATION_JSON).content(resource("input.json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(resource("expected-log.json")));
    }

    @Test
    void invalidMatchInputIs400WithDetail() throws Exception {
        mvc.perform(post("/v1/simulate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/v1/simulate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonJsonContentTypeIsRejected() throws Exception {
        mvc.perform(post("/v1/simulate").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
