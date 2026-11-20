package quorma.backend.match;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matches")
@Tag(name = "Matches", description = "The history of my series (a series is one start: a room, a challenge or matchmaking, "
        + "with up to three games) and the results I have not read yet. Players only. Only the two players of a series "
        + "can see it: for anyone else it is a 404. The log of a game is not stored: the turn-by-turn result of a game is "
        + "rebuilt by `GET /matches/{seriesId}/games/{n}/replay`, which also verifies it against the stored hash. Errors carry a `code`.")
public class MatchController {

    private final MatchService matches;
    private final ReplayService replays;

    public MatchController(MatchService matches, ReplayService replays) {
        this.matches = matches;
        this.replays = replays;
    }

    @GetMapping
    @Operation(operationId = "listMatchSeries", summary = "My history", description = "My finished series, newest first, each with its games (outcome, factions, "
            + "SHA-256 of the log). With `unread=true` only the series whose result I have not read: this is how an offline "
            + "player receives results at the next login. A series ends `A_WIN`, `B_WIN` or `DRAW` seen from player A "
            + "(the one who started it); `mySeat` says which one I am.")
    public List<SeriesResponse> list(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(name = "unread", defaultValue = "false") boolean unread,
                                     @RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "20") int size) {
        return matches.list(userId(jwt), unread, page, size);
    }

    @GetMapping("/{seriesId}")
    @Operation(operationId = "getMatchSeries", summary = "One of my series")
    @ApiResponse(responseCode = "200", description = "The series with its games")
    @ApiResponse(responseCode = "404", description = "Not found, or I did not play it. Body `code`: match.not_found", content = @Content)
    public SeriesResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("seriesId") UUID seriesId) {
        return matches.get(userId(jwt), seriesId);
    }

    @PostMapping("/{seriesId}/seen")
    @Operation(operationId = "markMatchSeriesSeen", summary = "Mark a result as read", description = "Marks every game of the series as read for me; it stops "
            + "appearing under `unread=true`. Safe to repeat.")
    @ApiResponse(responseCode = "200", description = "The series, now read")
    @ApiResponse(responseCode = "404", description = "Not found, or I did not play it. Body `code`: match.not_found", content = @Content)
    public SeriesResponse seen(@AuthenticationPrincipal Jwt jwt, @PathVariable("seriesId") UUID seriesId) {
        return matches.markSeen(userId(jwt), seriesId);
    }

    @GetMapping("/{seriesId}/games/{n}/replay")
    @Operation(operationId = "replayMatchGame", summary = "Replay one game", description = "Plays game `n` (1 to 3) of one of my "
            + "series again from its stored recipe and returns the match log, turn by turn. The log is never stored: the engine "
            + "rebuilds it, and the response is sent only if its SHA-256 equals the one stored when the game was played "
            + "(`verified` is always true). The replay runs only on the engine build and the rule pack that played the game.")
    @ApiResponse(responseCode = "200", description = "The log of the game, verified against the stored hash")
    @ApiResponse(responseCode = "404", description = "No such game, or I did not play it. Body `code`: match.not_found", content = @Content)
    @ApiResponse(responseCode = "409", description = "The running engine is not the one that played the game. Body `code`: "
            + "replay.engine_version_mismatch or replay.ruleset_mismatch, with `stored` and `running` (engineVersion, rulesetVersion, rulesetHash)", content = @Content)
    @ApiResponse(responseCode = "500", description = "The replay gave another result than the stored one. Body `code`: replay.result_mismatch", content = @Content)
    @ApiResponse(responseCode = "503", description = "The engine is not available. Body `code`: engine.unavailable", content = @Content)
    public ReplayResponse replay(@AuthenticationPrincipal Jwt jwt, @PathVariable("seriesId") UUID seriesId,
                                 @PathVariable("n") int n) {
        return replays.replay(userId(jwt), seriesId, n);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
