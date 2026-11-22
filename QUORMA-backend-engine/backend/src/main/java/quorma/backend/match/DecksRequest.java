package quorma.backend.match;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import quorma.backend.common.ErrorCodes;

/** The decks a player puts up when accepting a challenge or joining a room: one per faction. */
public record DecksRequest(
        @Schema(description = "Exactly one deck per faction")
        @NotNull(message = ErrorCodes.REQUIRED)
        @Size(min = 2, max = 2, message = ErrorCodes.CHALLENGE_DECKS_INVALID) List<@Valid DeckChoice> decks) {
}
