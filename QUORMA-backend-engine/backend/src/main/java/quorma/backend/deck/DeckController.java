package quorma.backend.deck;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/decks")
@Tag(name = "Decks", description = "The player's decks. Players only: admins do not play. A 400 error lists every broken rule in `errors`.")
public class DeckController {

    private final DeckService decks;

    public DeckController(DeckService decks) {
        this.decks = decks;
    }

    @GetMapping
    @Operation(summary = "List my decks")
    public List<DeckSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return decks.list(userId(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my decks with its cards")
    @ApiResponse(responseCode = "404", description = "Not found, or not mine", content = @Content)
    public DeckResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return decks.get(userId(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a deck", description = "Validates structure (32 cards, faction limits, uniqueness).")
    @ApiResponse(responseCode = "201", description = "Deck created")
    @ApiResponse(responseCode = "400", description = "Invalid deck (structure, ownership or snapshot)")
    @ApiResponse(responseCode = "409", description = "Deck name already used")
    public DeckResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeckRequest request) {
        return decks.create(userId(jwt), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace name, snapshot and cards of a deck", description = "Same validation as creation. The faction cannot change.")
    @ApiResponse(responseCode = "400", description = "Invalid deck (structure, ownership or snapshot)")
    @ApiResponse(responseCode = "404", description = "Not found, or not mine", content = @Content)
    @ApiResponse(responseCode = "409", description = "Deck name already used")
    public DeckResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody DeckUpdateRequest request) {
        return decks.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a deck")
    @ApiResponse(responseCode = "404", description = "Not found, or not mine", content = @Content)
    @ApiResponse(responseCode = "409", description = "It is the defense deck")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        decks.delete(userId(jwt), id);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
