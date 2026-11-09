package quorma.backend.deck;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** {@code POST /decks}. The deck rules are checked by {@link DeckService}. */
public record DeckRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Pattern(regexp = "Resistenza Umana|Potenze Artificiali", message = "unknown faction") String faction,
        @Schema(description = "Pool snapshot (of the same faction) the deck is pinned to") @NotNull UUID snapshotId,
        @Schema(description = "Owned card instances, exactly 32") @NotNull List<@NotNull UUID> cardInstanceIds) {
}
