package quorma.backend.telemetry;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** A player's statistics. A new player, or one whose series are not aggregated yet, has zeros and empty lists. */
public record StatsResponse(
        @Schema(description = "Series played (a series is the best of three)") long series,
        @Schema(description = "Series won") long wins,
        @Schema(description = "Series lost") long losses,
        @Schema(description = "Series drawn") long draws,
        @Schema(description = "Single games played") long gamesPlayed,
        @Schema(description = "Single games won") long gamesWon,
        @Schema(description = "Damage dealt by the player's creatures, in all games") long damageDealt,
        @Schema(description = "Overflow damage to the opponent after a kill, in all games") long spillDamage,
        @Schema(description = "Creatures the player destroyed, in all games") long kills,
        @Schema(description = "Turns the player had each poker hand, most frequent first; only hands that happened are listed")
        List<HandCount> hands,
        @Schema(description = "Games played and won with each faction; only factions already played are listed")
        List<FactionRecord> factions) {

    public record HandCount(
            @Schema(description = "Hand type constant, e.g. `FLUSH` (the frontend shows the label `game.hand.<name>`)") String hand,
            @Schema(description = "Turns in which the player had it") long turns) {
    }

    public record FactionRecord(
            @Schema(description = "Faction constant: `RESISTENZA_UMANA` or `POTENZE_ARTIFICIALI`") String faction,
            @Schema(description = "Games played with it") long gamesPlayed,
            @Schema(description = "Games won with it") long gamesWon) {
    }
}
