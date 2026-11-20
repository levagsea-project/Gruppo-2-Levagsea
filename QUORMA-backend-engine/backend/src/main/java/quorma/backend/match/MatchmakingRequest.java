package quorma.backend.match;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;

/** {@code POST /matchmaking}: play against a random opponent, with my decks. */
public record MatchmakingRequest(
        @Schema(description = "The faction I play in game 1; the opponent plays the other one", allowableValues = {"POTENZE_ARTIFICIALI", "RESISTENZA_UMANA"})
        @NotBlank(message = ErrorCodes.REQUIRED)
        @Pattern(regexp = Faction.PATTERN, message = ErrorCodes.FACTION_UNKNOWN) String startingFaction,
        @Schema(description = "Exactly one deck per faction")
        @NotNull(message = ErrorCodes.REQUIRED)
        @Size(min = 2, max = 2, message = ErrorCodes.CHALLENGE_DECKS_INVALID) List<@Valid DeckChoice> decks) {
}
