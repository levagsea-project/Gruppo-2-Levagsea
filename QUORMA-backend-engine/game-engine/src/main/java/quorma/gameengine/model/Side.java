package quorma.gameengine.model;

/** The two seats. A is the player who started the series and is shuffled first (ruleset 12.2). */
public enum Side {
    A,
    B;

    public Side other() {
        return this == A ? B : A;
    }
}
