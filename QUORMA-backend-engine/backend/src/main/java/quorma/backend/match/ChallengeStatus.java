package quorma.backend.match;

/** Lifecycle of a challenge. */
public enum ChallengeStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED,
    EXPIRED
}
