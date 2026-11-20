package quorma.backend.match;

import java.util.Map;

/**
 * A match cannot be replayed on the engine that is running: it was played by another engine build or another rule pack,
 * and the same input could play differently there. The message is an {@code ErrorCodes} code; {@code stored} is what the
 * match recorded and {@code running} what the engine says it is, for the person who has to start the right engine.
 */
public class ReplayUnavailableException extends RuntimeException {

    private final Map<String, String> stored;
    private final Map<String, String> running;

    public ReplayUnavailableException(String code, Map<String, String> stored, Map<String, String> running) {
        super(code);
        this.stored = Map.copyOf(stored);
        this.running = Map.copyOf(running);
    }

    public String code() {
        return getMessage();
    }

    public Map<String, String> stored() {
        return stored;
    }

    public Map<String, String> running() {
        return running;
    }
}
