package quorma.gameengine.log;

/** Why an effect on the stack did nothing. */
public enum FizzleReason {
    /** Its target was destroyed by the time it resolved ("le creature distrutte non sono influenzate"). */
    TARGET_DESTROYED
}
