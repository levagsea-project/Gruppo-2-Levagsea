package quorma.gameengine.hand;

/**
 * The poker hands. Ordinal 0 is the best hand, so a lower ordinal beats a higher one.
 * {@link #NONE} is the board with no creatures: the worst hand, with no attackers.
 */
public enum HandType {
    FIVE_OF_A_KIND,
    ROYAL_FLUSH,
    STRAIGHT_FLUSH,
    FOUR_OF_A_KIND,
    FULL_HOUSE,
    FLUSH,
    STRAIGHT,
    THREE_OF_A_KIND,
    FOUR_OF_A_SUIT,
    TWO_PAIR,
    PAIR,
    HIGH_CARD,
    NONE
}
