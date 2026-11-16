package quorma.backend.engine;

/** The engine service rejected the request (4xx): the backend sent invalid input. */
public class EngineRejectedException extends RuntimeException {
    public EngineRejectedException(String message, Throwable cause) { super(message, cause); }
}