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

    public MatchController(MatchService matches) {
        this.matches = matches;
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

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
