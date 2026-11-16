package quorma.backend.engine;

/** The engine service timed out, could not be reached, or returned an unsuccessful response. */
public class EngineUnavailableException extends RuntimeException {
    public EngineUnavailableException(String message) { super(message); }
    public EngineUnavailableException(String message, Throwable cause) { super(message, cause); }
}
