package quorma.gameengine.model;

/** Why a match ended (ruleset 11). */
public enum EndReason {
    /** A player has no creatures in play at End of turn (11.1). */
    NO_CREATURES,
    /** A player is at 0 life or less at End of turn (11.2). */
    LIFE_ZERO,
    /** Both players lose at the same time: a draw (11.3). */
    BOTH_LOST,
    /** After turn 50, the player with more life wins (11.4). */
    TURN_50_LIFE,
    /** After turn 50 with equal life, the better hand of the last Evaluate wins (11.4). */
    TURN_50_HAND,
    /** After turn 50, equal life and equal hands: a draw (11.4). */
    TURN_50_DRAW
}
