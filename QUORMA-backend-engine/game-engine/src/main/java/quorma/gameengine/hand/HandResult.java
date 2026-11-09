package quorma.gameengine.hand;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of evaluating a board.
 *
 * @param type          the best-ranked hand the creatures form
 * @param attackerLanes lanes of the creatures that attack, ascending
 * @param tiebreak      values compared when two hands have the same type, most significant first. Ace is
 *                      <b>14</b> here, so comparison is plain lexicographic. The low straight A-2-3-4-5 is
 *                      {@code [5]}. Suits never appear.
 */
public record HandResult(HandType type, List<Integer> attackerLanes, List<Integer> tiebreak) {

    /** Value of the Ace in {@link #tiebreak()}. */
    public static final int ACE_HIGH = 14;

    public HandResult {
        Objects.requireNonNull(type, "type");
        attackerLanes = List.copyOf(attackerLanes);
        tiebreak = List.copyOf(tiebreak);
    }

    /** The result for a board with no creatures: no hand, no attackers. */
    public static HandResult none() {
        return new HandResult(HandType.NONE, List.of(), List.of());
    }
}
