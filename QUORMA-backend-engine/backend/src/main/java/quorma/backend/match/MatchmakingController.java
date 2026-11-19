package quorma.backend.match;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matchmaking")
@Tag(name = "Matchmaking", description = "Play against a random opponent who may be offline. Players only: admins do not play. "
        + "Errors carry a `code` (a key of the label files), never text.")
public class MatchmakingController {

    private final MatchmakingService matchmaking;

    public MatchmakingController(MatchmakingService matchmaking) {
        this.matchmaking = matchmaking;
    }

    @PostMapping
    @Operation(summary = "Play against a random opponent", description = "The server picks a player who logged in within the last "
            + "7 days and has a legal defense deck for both factions, and plays the whole series (best of three, factions swapped) "
            + "at once against their defense decks: in each game they use the defense deck of the faction opposite to mine. "
            + "I choose my decks (one per faction). The response has the series with the log of every game; the opponent "
            + "reads the result at their next login.")
    @ApiResponse(responseCode = "200", description = "The series that was played")
    @ApiResponse(responseCode = "400", description = "challenge.decks_invalid or engine.input_invalid")
    @ApiResponse(responseCode = "404", description = "matchmaking.no_opponent (nobody can be picked now), or deck.not_found", content = @Content)
    @ApiResponse(responseCode = "409", description = "deck.outdated or deck.defense_required")
    @ApiResponse(responseCode = "503", description = "The engine is down or too slow. Body `code`: engine.unavailable")
    public SeriesResponse play(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MatchmakingRequest request) {
        return matchmaking.play(UUID.fromString(jwt.getSubject()), request);
    }
}
