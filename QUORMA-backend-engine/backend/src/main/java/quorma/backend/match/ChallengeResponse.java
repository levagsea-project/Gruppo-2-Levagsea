package quorma.backend.match;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** A challenge. {@code seriesId} is set once it was accepted: it is already played. */
public record ChallengeResponse(
        UUID id,
        String code,
        @Schema(description = "PENDING, ACCEPTED (accepted and already played), DECLINED, CANCELLED or EXPIRED. "
                + "A pending challenge past its expiry is shown as EXPIRED") ChallengeStatus status,
        UserRef challenger,
        UserRef opponent,
        @Schema(description = "The faction the challenger plays in game 1; the opponent has the other one") String challengerFaction,
        Instant createdAt,
        Instant expiresAt,
        Instant resolvedAt,
        UUID seriesId) {
}
