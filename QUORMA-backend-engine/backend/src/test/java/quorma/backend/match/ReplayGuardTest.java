package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import quorma.backend.common.ErrorCodes;
import quorma.backend.engine.EngineInfo;
import quorma.backend.engine.EngineUnavailableException;

/** A match is only replayed on the engine build and rule pack that played it. */
class ReplayGuardTest {

    private static final String HASH = "ab".repeat(32);
    private static final String OTHER_HASH = "cd".repeat(32);

    private static ReplayGuard guardFor(String engineVersion, String rulesetVersion, String rulesetHash) {
        return new ReplayGuard(() -> new EngineInfo(engineVersion, rulesetVersion, rulesetHash));
    }

    private static ReplayUnavailableException refused(ReplayGuard guard, String engineVersion, String rulesetVersion, String hash) {
        return assertThrows(ReplayUnavailableException.class, () -> guard.requireSameEngine(engineVersion, rulesetVersion, hash));
    }

    @Test
    void theSameEngineAndRulePackIsAllowed() {
        assertDoesNotThrow(() -> guardFor("1.0.0", "1.0-alpha", HASH).requireSameEngine("1.0.0", "1.0-alpha", HASH));
    }

    @Test
    void anotherEngineVersionIsRefusedAndBothSidesAreNamed() {
        ReplayUnavailableException e = refused(guardFor("1.1.0", "1.0-alpha", HASH), "1.0.0", "1.0-alpha", HASH);
        assertEquals(ErrorCodes.REPLAY_ENGINE_VERSION_MISMATCH, e.code());
        assertEquals("1.0.0", e.stored().get("engineVersion"));
        assertEquals("1.1.0", e.running().get("engineVersion"));
        assertEquals(HASH, e.stored().get("rulesetHash"));
    }

    @Test
    void anotherRulePackIsRefusedEvenOnTheSameEngineVersion() {
        assertEquals(ErrorCodes.REPLAY_RULESET_MISMATCH,
                refused(guardFor("1.0.0", "1.0-alpha", OTHER_HASH), "1.0.0", "1.0-alpha", HASH).code(),
                "the rule pack changed (a rebalance) under the same engine build");
        assertEquals(ErrorCodes.REPLAY_RULESET_MISMATCH,
                refused(guardFor("1.0.0", "1.1", HASH), "1.0.0", "1.0-alpha", HASH).code());
    }

    @Test
    void theEngineVersionIsReportedFirstWhenBothDiffer() {
        assertEquals(ErrorCodes.REPLAY_ENGINE_VERSION_MISMATCH,
                refused(guardFor("2.0.0", "2.0", OTHER_HASH), "1.0.0", "1.0-alpha", HASH).code());
    }

    @Test
    void anEngineThatCannotSayWhatItIsIsNotTrusted() {
        ReplayGuard guard = new ReplayGuard(() -> {
            throw new EngineUnavailableException("the engine service could not be reached");
        });
        assertThrows(EngineUnavailableException.class, () -> guard.requireSameEngine("1.0.0", "1.0-alpha", HASH));
    }

    // ---- what the client of a replay endpoint receives ----

    @RestController
    static class ReplayProbe {
        @GetMapping("/probe")
        String probe() {
            throw new ReplayUnavailableException(ErrorCodes.REPLAY_ENGINE_VERSION_MISMATCH,
                    java.util.Map.of("engineVersion", "1.0.0"), java.util.Map.of("engineVersion", "1.1.0"));
        }
    }

    @Test
    void aRefusedReplayIs409WithTheCodeAndBothSides() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReplayProbe()).setControllerAdvice(new ReplayExceptionHandler()).build();
        mvc.perform(get("/probe"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("replay.engine_version_mismatch"))
                .andExpect(jsonPath("$.detail").value("replay.engine_version_mismatch"))
                .andExpect(jsonPath("$.stored.engineVersion").value("1.0.0"))
                .andExpect(jsonPath("$.running.engineVersion").value("1.1.0"));
    }
}
