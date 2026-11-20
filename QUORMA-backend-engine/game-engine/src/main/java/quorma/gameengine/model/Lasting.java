package quorma.gameengine.model;

import java.util.Objects;

/**
 * How long something an effect left behind lasts: a vocabulary duration ({@code D_...}), the turn it started in and,
 * for {@code D_FincheSorgenteInGioco}, the creature it depends on.
 *
 * <ul>
 *   <li>{@code D_Permanente}: until the creature leaves play.</li>
 *   <li>{@code D_FinoAllaFineDelTurno}: until the End of turn of the turn it started in.</li>
 *   <li>{@code D_FinoAllaFineDelCombattimento}: until the end of the Combat of the turn it started in.</li>
 *   <li>{@code D_FinoAlProssimoRifornimento}: until the start of the next Refill, so one that starts during a Refill
 *       lasts until the Refill of the next turn.</li>
 *   <li>{@code D_FinoAUccisione}: until the creature that has it destroys a creature.</li>
 *   <li>{@code D_FincheSorgenteInGioco}: while {@code source} is in play and not destroyed; with no source, until the
 *       creature leaves play.</li>
 * </ul>
 */
public record Lasting(String duration, int turn, Creature source) {

    public static final String PERMANENT = "D_Permanente";
    public static final String END_OF_TURN = "D_FinoAllaFineDelTurno";
    public static final String END_OF_COMBAT = "D_FinoAllaFineDelCombattimento";
    public static final String NEXT_REFILL = "D_FinoAlProssimoRifornimento";
    public static final String UNTIL_KILL = "D_FinoAUccisione";
    public static final String WHILE_SOURCE = "D_FincheSorgenteInGioco";

    public Lasting {
        Objects.requireNonNull(duration, "duration");
    }

    public static Lasting permanent() {
        return new Lasting(PERMANENT, 0, null);
    }

    /** False once its source has been destroyed or has left play ({@code D_FincheSorgenteInGioco} only). */
    public boolean active() {
        return !WHILE_SOURCE.equals(duration) || source == null || (!source.destroyed() && !source.leftPlay());
    }

    /**
     * True if this ends at the moment named by {@code duration} in turn {@code now}: End of turn and end of Combat
     * end what started this turn or before, the start of Refill ends what started before this turn.
     */
    public boolean endsAt(String moment, int now) {
        if (!duration.equals(moment)) {
            return false;
        }
        return NEXT_REFILL.equals(moment) ? turn < now : turn <= now;
    }
}
