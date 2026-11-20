package quorma.backend.match;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** One game played again from its stored recipe. */
public record ReplayResponse(
        UUID seriesId,
        int gameNumber,
        UUID matchId,
        @Schema(description = "Faction player A played in this game") String factionA,
        @Schema(description = "Faction player B played in this game") String factionB,
        @Schema(description = "A_WIN, B_WIN or DRAW") MatchOutcome outcome,
        @Schema(description = "SHA-256 of the log, the same as the one stored when the game was played") String resultHash,
        @Schema(description = "Always true: a replay whose hash differs from the stored one is an error, never a response")
        boolean verified,
        @Schema(description = "The match log, turn by turn (engine schema match-log)") JsonNode log) {
}
