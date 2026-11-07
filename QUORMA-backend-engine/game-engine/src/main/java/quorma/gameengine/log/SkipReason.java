package quorma.gameengine.log;

/** Why a declared attack did not happen. */
public enum SkipReason {
    /** The opponent had no creature to target when the attack was declared. */
    NO_TARGET,
    /** The target was already destroyed when the attacker was called. */
    TARGET_DESTROYED,
    /** The attacker was destroyed before it was called to attack. */
    ATTACKER_DESTROYED
}
