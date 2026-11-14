package quorma.gameengine.effects;

/**
 * A card's ability cannot be played: unknown or parked, wrong params, or written with vocabulary the engine
 * does not implement yet. Thrown when the card is bound, never silently ignored.
 */
public final class AbilityException extends RuntimeException {

    public AbilityException(String message) {
        super(message);
    }
}
