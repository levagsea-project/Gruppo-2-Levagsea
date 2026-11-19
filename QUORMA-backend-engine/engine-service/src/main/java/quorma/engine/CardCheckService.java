package quorma.engine;

import java.util.Objects;
import org.springframework.stereotype.Service;
import quorma.gameengine.GameEngine;

/** Thin HTTP adapter over the engine's card pool check: the rules are in game-engine. */
@Service
public class CardCheckService {

    private final GameEngine gameEngine;

    public CardCheckService(GameEngine gameEngine) {
        this.gameEngine = Objects.requireNonNull(gameEngine, "gameEngine");
    }

    /** Checks the candidate card pool of the request and returns the canonical report JSON. */
    public String check(String requestJson) {
        return gameEngine.cardCheck(requestJson);
    }
}
