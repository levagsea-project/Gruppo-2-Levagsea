package quorma.engine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import quorma.gameengine.GameEngine;

/** The real engine behind the real controller: the rules themselves are tested in game-engine. */
class CardCheckControllerTest {

    private static final String CREATURE = "{\"id\":\"%s\",\"kind\":\"CREATURE\",\"rank\":%d,\"suit\":\"VINCOLO\","
            + "\"subfaction\":\"ADATTATI\",\"atk\":3000,\"def\":5000,\"abilities\":[%s]}";

    private final GameEngine engine = new GameEngine();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CardCheckController(new CardCheckService(engine)))
                .setControllerAdvice(new EngineErrorHandler())
                .build();
    }

    private static String request(String... cards) {
        return "{\"schema_version\":1,\"faction\":\"RESISTENZA_UMANA\",\"cards\":[" + String.join(",", cards) + "]}";
    }

    @Test
    void aGoodPoolIsOkAndNamesTheRulePack() throws Exception {
        mvc.perform(post("/v1/cards/check").contentType(MediaType.APPLICATION_JSON)
                        .content(request(CREATURE.formatted("A", 5, ""))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.ruleset_hash").value(engine.rulesetHash()))
                .andExpect(jsonPath("$.card_errors").isEmpty())
                .andExpect(jsonPath("$.pool_errors").isEmpty());
    }

    @Test
    void aBadCardIs200WithItsErrorsInTheReport() throws Exception {
        mvc.perform(post("/v1/cards/check").contentType(MediaType.APPLICATION_JSON)
                        .content(request(CREATURE.formatted("A", 5, "{\"id\":\"K_LETALE\"}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.card_errors[0].card_id").value("A"))
                .andExpect(jsonPath("$.card_errors[0].code").value("card.rank_not_allowed"));
    }

    @Test
    void aBrokenRequestIs400WithDetail() throws Exception {
        mvc.perform(post("/v1/cards/check").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void malformedJsonIs400AndOtherContentTypesAreRefused() throws Exception {
        mvc.perform(post("/v1/cards/check").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/cards/check").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
