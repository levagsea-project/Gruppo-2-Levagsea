package quorma.backend.deck;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** DTO for updating a deck. {@code PUT /decks/{id}}: replaces name, snapshot and cards. The faction of a deck never changes. */
public record DeckUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        @NotNull UUID snapshotId,
        @Schema(description = "Owned card instances, exactly 32") @NotNull List<@NotNull UUID> cardInstanceIds) {
}
