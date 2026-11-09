package quorma.gameengine.data;

/** Thrown when a data file is missing, malformed or does not match the expected shape. The message carries a JSON path. */
public final class RulesetException extends RuntimeException {
    public RulesetException(String message) {
        super(message);
    }

    public RulesetException(String message, Throwable cause) {
        super(message, cause);
    }
}
