package quorma.gameengine.model;

import java.util.Objects;

/** How a match ended. */
public record MatchResult(Outcome outcome, EndReason reason) {

    public MatchResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(reason, "reason");
    }
}
