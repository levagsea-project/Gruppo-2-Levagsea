package quorma.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import quorma.gameengine.EngineVersion;
import quorma.gameengine.GameEngine;

/**
 * Liveness and identity: which engine this is. {@code engineVersion}, {@code rulesetVersion} and {@code rulesetHash} are
 * read from the engine itself, the same values every MatchLog it writes names in its header, so the backend can check
 * that the engine that would replay a match is the one that played it. Never from configuration: a second source for
 * the same fact could drift.
 */
@RestController
public class EngineInfoController {

    private final GameEngine engine;

    public EngineInfoController(GameEngine engine) {
        this.engine = engine;
    }

    @GetMapping("/engine/info")
    public Map<String, String> info() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("engineVersion", EngineVersion.VERSION);
        body.put("rulesetVersion", engine.rules().rulesetVersion());
        body.put("rulesetHash", engine.rulesetHash());
        return body;
    }
}
