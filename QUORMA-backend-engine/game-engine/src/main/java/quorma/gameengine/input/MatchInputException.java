package quorma.gameengine.input;

/**
 * The match input cannot be played: bad JSON, a field that breaks the schema, a card that does not belong to the
 * player, or an ability the engine cannot play. The message starts with the JSON path of the problem, for example
 * {@code players.A.deck[3].abilities[0]}. A caller error, never an engine bug.
 */
public final class MatchInputException extends RuntimeException {

    public MatchInputException(String message) {
        super(message);
    }

    public MatchInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
