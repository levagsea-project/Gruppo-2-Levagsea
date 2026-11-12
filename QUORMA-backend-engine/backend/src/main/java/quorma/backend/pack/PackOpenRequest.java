package quorma.backend.pack;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import quorma.backend.catalogue.Faction;

/**
 * {@code POST /packs/open}. Pools are per faction, so a pack belongs to one faction.
*/
public record PackOpenRequest(
        @Schema(description = "Code of the pack type, from GET /packs")
        @NotBlank(message = PackErrorCodes.REQUIRED) String packType,
        @Schema(description = "Faction whose pool the pack is drawn from",
                allowableValues = {"POTENZE_ARTIFICIALI", "RESISTENZA_UMANA"})
        @NotBlank(message = PackErrorCodes.REQUIRED)
        @Pattern(regexp = Faction.PATTERN, message = PackErrorCodes.FACTION_UNKNOWN)
        String faction) {
}
