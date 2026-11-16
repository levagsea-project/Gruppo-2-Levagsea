package quorma.backend.engine;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Backend entry point that delegates match simulation to the engine-service. */
@RestController
@RequestMapping("/v1")
public class SimulationController {

    private final HttpGameEngine gameEngine;

    public SimulationController(HttpGameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    @PostMapping("/simulate")
    public MatchLog simulate(@RequestBody JsonNode input) {
        return gameEngine.simulate(input);
    }
}
