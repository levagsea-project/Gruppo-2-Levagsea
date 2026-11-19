package quorma.backend.match;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** One game of a series. Player A is the one who started the series, in every game. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameResponse(
        int gameNumber,
        UUID matchId,
        @Schema(description = "Faction player A played in this game") String factionA,
        @Schema(description = "Faction player B played in this game") String factionB,
        @Schema(description = "A_WIN, B_WIN or DRAW") MatchOutcome outcome,
        @Schema(description = "SHA-256 of the match log, as the engine wrote it") String resultHash,
        Instant playedAt,
        @Schema(description = "The match log, turn by turn (engine schema match-log). Only in the response that ran the "
                + "series; later it is rebuilt by the replay endpoint") JsonNode log) {
}
