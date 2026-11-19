package quorma.backend.match;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;

/** The deck a player puts up for one faction. A series needs one for each of the two factions. */
public record DeckChoice(
        @Schema(allowableValues = {"POTENZE_ARTIFICIALI", "RESISTENZA_UMANA"})
        @NotBlank(message = ErrorCodes.REQUIRED)
        @Pattern(regexp = Faction.PATTERN, message = ErrorCodes.FACTION_UNKNOWN) String faction,
        @Schema(description = "One of my decks of that faction. An outdated deck cannot be chosen")
        @NotNull(message = ErrorCodes.REQUIRED) UUID deckId) {
}
