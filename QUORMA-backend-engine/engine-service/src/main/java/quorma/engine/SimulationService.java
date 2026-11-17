package quorma.engine;

import java.util.Objects;
import org.springframework.stereotype.Service;
import quorma.gameengine.GameEngine;

/** Thin HTTP adapter over the deterministic game engine. */
@Service
public class SimulationService {

    private final GameEngine gameEngine;

    public SimulationService(GameEngine gameEngine) {
        this.gameEngine = Objects.requireNonNull(gameEngine, "gameEngine");
    }

    /** Plays the supplied schema-defined input and returns the canonical MatchLog JSON. */
    public String simulate(String inputJson) {
        return gameEngine.matchLog(inputJson);
    }
}
