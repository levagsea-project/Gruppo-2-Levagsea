package quorma.backend.match;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** An open room: someone waiting online for an opponent. The decks of the creator stay hidden until the match. */
public record RoomResponse(
        UUID id,
        @Schema(description = "rnd-XXXX-XXXX") String code,
        UserRef creator,
        @Schema(description = "The faction the creator plays in game 1; whoever joins plays the other one in game 1") String creatorFaction,
        RoomStatus status,
        Instant createdAt,
        Instant expiresAt) {
}
