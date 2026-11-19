package quorma.backend.match;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/challenges")
@Tag(name = "Challenges", description = "Challenge a player by nickname. Players only: admins do not play. "
        + "Every challenge is a best of three with factions swapped after every game, and each player uses their own decks, "
        + "one per faction. The opponent may be offline: they answer when they log in, and accepting plays the whole series "
        + "at once, with no decision from either player. Both players see the result; the one who was not there reads it at "
        + "the next login (`GET /matches?unread=true`). Errors carry a `code` (a key of the label files), never text.")
public class ChallengeController {

    private final ChallengeService challenges;

    public ChallengeController(ChallengeService challenges) {
        this.challenges = challenges;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Challenge a player", description = "Locks a snapshot of my decks (one per faction) and waits for the "
            + "opponent, for the time set in `matches.challenge-ttl` (48 h, at most 7 days). One pending challenge at a time "
            + "from me to the same player. I need a legal defense deck for both factions.")
    @ApiResponse(responseCode = "201", description = "Challenge created, status PENDING")
    @ApiResponse(responseCode = "400", description = "Body `code`: challenge.self, challenge.decks_invalid, or validation.failed with `fieldErrors`")
    @ApiResponse(responseCode = "404", description = "challenge.opponent_not_found (no such player), or deck.not_found (a deck that is not mine)")
    @ApiResponse(responseCode = "409", description = "challenge.already_pending, deck.outdated or deck.defense_required")
    public ChallengeResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateChallengeRequest request) {
        return challenges.create(userId(jwt), request);
    }

    @GetMapping
    @Operation(summary = "My challenges", description = "The ones I sent and the ones I received, newest first. "
            + "A pending challenge past its expiry is listed as EXPIRED.")
    public List<ChallengeResponse> list(@AuthenticationPrincipal Jwt jwt,
                                        @RequestParam(required = false) ChallengeStatus status) {
        return challenges.list(userId(jwt), Optional.ofNullable(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One of my challenges")
    @ApiResponse(responseCode = "404", description = "Not found, or not mine. Body `code`: challenge.not_found", content = @Content)
    public ChallengeResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return challenges.get(userId(jwt), id);
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a challenge and play it", description = "Only the challenged player. I choose my decks (one per "
            + "faction); the server draws a seed for every game, calls the engine, stores the recipe of each game (decks, seed, "
            + "versions, SHA-256 of the log) and the challenge becomes ACCEPTED. The response has the whole series with the "
            + "log of every game. If the engine fails nothing is saved and the challenge stays PENDING: retry later. "
            + "Accepting twice is a 409.")
    @ApiResponse(responseCode = "200", description = "The series that was played")
    @ApiResponse(responseCode = "400", description = "challenge.decks_invalid or engine.input_invalid")
    @ApiResponse(responseCode = "404", description = "Not found, or I am not the challenged player. Body `code`: challenge.not_found", content = @Content)
    @ApiResponse(responseCode = "409", description = "challenge.not_pending, challenge.expired, deck.outdated or deck.defense_required")
    @ApiResponse(responseCode = "503", description = "The engine is down or too slow. Body `code`: engine.unavailable")
    public SeriesResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody DecksRequest request) {
        return challenges.accept(userId(jwt), id, request.decks());
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline a challenge", description = "Only the challenged player, while it is pending.")
    @ApiResponse(responseCode = "404", description = "Not found, or I am not the challenged player", content = @Content)
    @ApiResponse(responseCode = "409", description = "challenge.not_pending or challenge.expired")
    public ChallengeResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return challenges.decline(userId(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Withdraw a challenge", description = "Only the challenger, while it is pending.")
    @ApiResponse(responseCode = "404", description = "Not found, or I am not the challenger", content = @Content)
    @ApiResponse(responseCode = "409", description = "challenge.not_pending or challenge.expired")
    public ChallengeResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return challenges.cancel(userId(jwt), id);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
