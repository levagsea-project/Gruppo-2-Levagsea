package quorma.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal liveness/info endpoint. Match endpoints will delegate to game-engine. */
@RestController
public class EngineInfoController {

    private final String version;

    public EngineInfoController(@Value("${engine.version}") String version) {
        this.version = version;
    }

    @GetMapping("/engine/info")
    public Map<String, String> info() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("engineVersion", version);
        return body;
    }
}
