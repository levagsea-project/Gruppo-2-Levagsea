package quorma.backend.deck;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import quorma.backend.catalogue.PreferredLocales;

@RestController
@RequestMapping("/decks")
@Tag(name = "Decks", description = "The player's decks. Players only: admins do not play. Errors carry a `code`; a 400 also lists every broken rule in `errors` as `{code, params}`. The faction is `RESISTENZA_UMANA` or `POTENZE_ARTIFICIALI`.")
public class DeckController {

    private final DeckService decks;
    private final DeckEligibility eligibility;

    public DeckController(DeckService decks, DeckEligibility eligibility) {
        this.decks = decks;
        this.eligibility = eligibility;
    }

    @GetMapping
    @Operation(summary = "List my decks")
    public List<DeckSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return decks.list(userId(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my decks with its cards")
    @ApiResponse(responseCode = "200", description = "The deck with its cards")
    @ApiResponse(responseCode = "404", description = "Not found, or not mine", content = @Content)
    public DeckResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                            @Parameter(description = "Language of the card text (name, flavour, set names): the first language in the list that a card has, Italian as the fallback. The response says which language each text is in (`textLocale`).") @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return decks.get(userId(jwt), id, PreferredLocales.parse(acceptLanguage));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a deck", description = "Validates structure (32 cards, faction limits, uniqueness).")
    @ApiResponse(responseCode = "201", description = "Deck created")
    @ApiResponse(responseCode = "400", description = "Invalid deck (structure, ownership or snapshot)")
    @ApiResponse(responseCode = "409", description = "Deck name already used")
    public DeckResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeckRequest request,
                               @Parameter(description = "Language of the card text (name, flavour, set names): the first language in the list that a card has, Italian as the fallback. The response says which language each text is in (`textLocale`).") @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return decks.create(userId(jwt), request, PreferredLocales.parse(acceptLanguage));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace name, snapshot and cards of a deck", description = "Same validation as creation. The faction cannot change.")
    @ApiResponse(responseCode = "200", description = "Deck updated")
    @ApiResponse(responseCode = "400", description = "Invalid deck (structure, ownership or snapshot)")
    @ApiResponse(responseCode = "404", description = "Not found, or not mine", content = @Content)
    @ApiResponse(responseCode = "409", description = "Deck name already used")
    public DeckResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody DeckUpdateRequest request,
                               @Parameter(description = "Language of the card text (name, flavour, set names): the first language in the list that a card has, Italian as the fallback. The response says which language each text is in (`textLocale`).") @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return decks.update(userId(jwt), id, request, PreferredLocales.parse(acceptLanguage));
    }

    @PostMapping("/{id}/defense")
    @Operation(summary = "Make a deck the defense deck of its faction", description = "Replaces the current defense deck of that faction. An outdated deck cannot be chosen.")
    @ApiResponse(responseCode = "200", description = "The deck, now the defense deck")
    @ApiResponse(responseCode = "404", description = "Not found, or not mine", content = @Content)
    @ApiResponse(responseCode = "409", description = "The deck is outdated. Body `code`: deck.outdated")
    public DeckResponse setDefense(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Parameter(description = "Language of the card text (name, flavour, set names): the first language in the list that a card has, Italian as the fallback. The response says which language each text is in (`textLocale`).") @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return decks.setDefense(userId(jwt), id, PreferredLocales.parse(acceptLanguage));
    }

    @GetMapping("/play-status")
    @Operation(summary = "Can I play?", description = "A player needs a legal (not outdated) defense deck for both factions to start or accept a challenge or matchmaking. Lists the factions whose defense deck is outdated or missing, and every outdated deck. Ask for it when the home page opens: the state can change during a session.")
    public DeckEligibility.PlayStatus playStatus(@AuthenticationPrincipal Jwt jwt) {
        return eligibility.status(userId(jwt));
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
