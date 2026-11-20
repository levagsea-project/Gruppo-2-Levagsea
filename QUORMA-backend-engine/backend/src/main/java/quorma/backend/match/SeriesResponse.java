package quorma.backend.match;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A series of games between two players, as one of them sees it. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeriesResponse(
        UUID id,
        @Schema(description = "CHALLENGE (a room or a challenge by nickname) or MATCHMAKING") SeriesKind kind,
        @Schema(description = "Code of the challenge or room that started it: ch-XXXX-XXXX or rnd-XXXX-XXXX. Absent in matchmaking")
        String code,
        UserRef playerA,
        UserRef playerB,
        @Schema(description = "A if I started the series, B if I was the other player") String mySeat,
        SeriesStatus status,
        @Schema(description = "Result of the series seen from player A: A_WIN, B_WIN or DRAW") MatchOutcome outcome,
        Instant finishedAt,
        @Schema(description = "True while I have not read the result: it is delivered at the next login") boolean unread,
        List<GameResponse> games) {
}
