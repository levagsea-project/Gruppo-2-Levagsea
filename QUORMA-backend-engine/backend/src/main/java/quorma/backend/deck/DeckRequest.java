package quorma.backend.deck;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;

/** {@code POST /decks}. The deck rules are checked by {@link DeckService}. */
public record DeckRequest(
        @NotBlank(message = ErrorCodes.REQUIRED) @Size(max = 64, message = ErrorCodes.DECK_NAME_MAX_LENGTH) String name,
        @Schema(description = "Faction of the deck", allowableValues = {"POTENZE_ARTIFICIALI", "RESISTENZA_UMANA"})
        @NotBlank(message = ErrorCodes.REQUIRED)
        @Pattern(regexp = Faction.PATTERN, message = ErrorCodes.FACTION_UNKNOWN) String faction,
        @Schema(description = "Pool snapshot (of the same faction) the deck is pinned to")
        @NotNull(message = ErrorCodes.REQUIRED) UUID snapshotId,
        @Schema(description = "Owned card instances, exactly 32")
        @NotNull(message = ErrorCodes.REQUIRED) List<@NotNull(message = ErrorCodes.REQUIRED) UUID> cardInstanceIds) {
}
