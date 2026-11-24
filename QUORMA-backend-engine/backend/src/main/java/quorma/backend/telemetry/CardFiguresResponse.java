package quorma.backend.telemetry;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** The figures of one card: a player's own, or all players' inside one pool snapshot. */
public record CardFiguresResponse(
        UUID cardId,
        String cardCode,
        @Schema(description = "The card's name in `textLocale`") String name,
        @Schema(description = "The language `name` is in: the one asked for with Accept-Language if the card has it, else `it`") String textLocale,
        @Schema(description = "Games the card was in a deck") long matchesPlayed,
        @Schema(description = "Of those, the games its player won (a draw is not a win)") long matchesWon,
        @Schema(description = "Times the card was drawn") long timesDrawn,
        @Schema(description = "Damage dealt by the card") long damageDealt,
        @Schema(description = "Overflow damage to the opponent after the card's kills") long spillDamage,
        @Schema(description = "Creatures the card destroyed") long kills) {
}
