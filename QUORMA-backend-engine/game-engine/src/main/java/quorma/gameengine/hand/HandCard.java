package quorma.gameengine.hand;

import java.util.Objects;

/**
 * What the hand evaluator sees of a creature in play: its lane, and its <em>effective</em> rank and suit.
 * The caller applies any effect that bends rank or suit before building this. The evaluator never reads
 * ATK, DEF or keywords.
 *
 * @param lane lane 1..5
 * @param rank 1..13: Ace = 1 (counts high or low), 2..10, Jack = 11, Queen = 12, King = 13
 * @param suit effective suit
 */
public record HandCard(int lane, int rank, Suit suit) {

    public static final int MIN_LANE = 1;
    public static final int MAX_LANE = 5;
    public static final int ACE = 1;
    public static final int MIN_RANK = 1;
    public static final int MAX_RANK = 13;

    public HandCard {
        if (lane < MIN_LANE || lane > MAX_LANE) {
            throw new IllegalArgumentException("lane must be 1..5, got " + lane);
        }
        if (rank < MIN_RANK || rank > MAX_RANK) {
            throw new IllegalArgumentException("rank must be 1..13, got " + rank);
        }
        Objects.requireNonNull(suit, "suit");
    }
}
