package quorma.backend.match;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@Tag(name = "Rooms", description = "Play against someone who is online now. A player opens a room (`rnd-XXXX-XXXX`) and waits; "
        + "another player joins it from the list and the whole series (best of three, factions swapped, own decks) is played "
        + "at once. Players only: admins do not play. Errors carry a `code` (a key of the label files), never text.")
public class RoomController {

    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "openRoom", summary = "Open a room", description = "Locks a snapshot of my decks (one per faction) and waits for someone to "
            + "join, for the time set in `matches.room-ttl` (10 minutes). One open room at a time. I need a legal defense "
            + "deck for both factions.")
    @ApiResponse(responseCode = "201", description = "Room open")
    @ApiResponse(responseCode = "400", description = "challenge.decks_invalid, or validation.failed with `fieldErrors`")
    @ApiResponse(responseCode = "404", description = "deck.not_found (a deck that is not mine)")
    @ApiResponse(responseCode = "409", description = "room.already_open, deck.outdated or deck.defense_required")
    public RoomResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpenRoomRequest request) {
        return rooms.open(userId(jwt), request);
    }

    @GetMapping
    @Operation(operationId = "listRooms", summary = "Open rooms", description = "The rooms I could join: not mine, not expired, oldest first. "
            + "Each one shows the faction its creator plays in game 1.")
    public List<RoomResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return rooms.list(userId(jwt));
    }

    @GetMapping("/mine")
    @Operation(operationId = "getMyRoom", summary = "The last room I opened", description = "Still open, expired, closed, or MATCHED "
            + "with the `seriesId` of the series that was played while I was waiting. After a page reload this is how the "
            + "creator finds the code to close the room, or the result.")
    @ApiResponse(responseCode = "200", description = "My last room")
    @ApiResponse(responseCode = "404", description = "I never opened a room. Body `code`: room.not_found", content = @Content)
    public RoomResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return rooms.mine(userId(jwt));
    }

    @PostMapping("/{code}/join")
    @Operation(operationId = "joinRoom", summary = "Join a room and play", description = "I choose my decks (one per faction); the server draws a seed for "
            + "every game, calls the engine and stores the recipe of each game. The response has the whole series with the log of "
            + "every game. The creator reads the result at their next login. Two players joining together: the second one gets "
            + "room.not_open. If the engine fails nothing is saved and the room stays open.")
    @ApiResponse(responseCode = "200", description = "The series that was played")
    @ApiResponse(responseCode = "400", description = "room.own_room, challenge.decks_invalid or engine.input_invalid")
    @ApiResponse(responseCode = "404", description = "room.not_found, or deck.not_found (a deck that is not mine)", content = @Content)
    @ApiResponse(responseCode = "409", description = "room.not_open, room.expired, deck.outdated or deck.defense_required")
    @ApiResponse(responseCode = "503", description = "The engine is down or too slow. Body `code`: engine.unavailable")
    public SeriesResponse join(@AuthenticationPrincipal Jwt jwt, @PathVariable("code") String code,
                               @Valid @RequestBody DecksRequest request) {
        return rooms.join(userId(jwt), code, request.decks());
    }

    @PostMapping("/{code}/cancel")
    @Operation(operationId = "cancelRoom", summary = "Close my room", description = "Only the creator, while it is open.")
    @ApiResponse(responseCode = "200", description = "The room, now CANCELLED")
    @ApiResponse(responseCode = "404", description = "room.not_found, or it is not mine", content = @Content)
    @ApiResponse(responseCode = "409", description = "room.not_open or room.expired")
    public RoomResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable("code") String code) {
        return rooms.cancel(userId(jwt), code);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
