package quorma.backend.deck;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import quorma.backend.common.ErrorCodes;

/** DTO for updating a deck. {@code PUT /decks/{id}}: replaces name, snapshot and cards. The faction of a deck never changes. */
public record DeckUpdateRequest(
        @NotBlank(message = ErrorCodes.REQUIRED) @Size(max = 64, message = ErrorCodes.DECK_NAME_MAX_LENGTH) String name,
        @NotNull(message = ErrorCodes.REQUIRED) UUID snapshotId,
        @Schema(description = "Owned card instances, exactly 32")
        @NotNull(message = ErrorCodes.REQUIRED) List<@NotNull(message = ErrorCodes.REQUIRED) UUID> cardInstanceIds) {
}
