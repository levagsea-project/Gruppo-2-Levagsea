package quorma.backend.engine;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(summary = "Simulate a match",
            description = "Admins only: a development and diagnostic entry point, players start matches through challenges. "
                    + "Forwards the match input to the engine-service and returns the resulting match log. "
                    + "The backend does not interpret or re-derive the result.")
    @ApiResponse(responseCode = "200", description = "Match log")
    @ApiResponse(responseCode = "400", description = "The engine rejected the input. Body `code`: engine.input_invalid")
    @ApiResponse(responseCode = "503", description = "The engine service is down or too slow. Body `code`: engine.unavailable")
    public MatchLog simulate(@RequestBody JsonNode input) {
        return gameEngine.simulate(input);
    }
}
