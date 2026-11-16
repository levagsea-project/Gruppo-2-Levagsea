package quorma.backend.engine;

/** The engine service rejected the request (400/422): the simulation input is invalid. The message is the engine's validation detail. */
public class EngineRejectedException extends RuntimeException {
    public EngineRejectedException(String message, Throwable cause) { super(message, cause); }
}