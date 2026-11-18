package quorma.backend.pack;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/packs")
@Tag(name = "Packs", description = "Buying card packs with credits. Players only: admins do not play. "
        + "Errors carry a `code` (a key of the label files), never text for the player.")
public class PackController {

    private final PackService packs;

    public PackController(PackService packs) {
        this.packs = packs;
    }

    @GetMapping
    @Operation(summary = "List the pack types on sale", description = "Price and cards per Tier of each type."
        + "The frontend shows names from its labels: `pack.type.<code>` for a type, `game.tier.<tier>` for a rarity.")
    public List<PackTypeResponse> list() {
        return packs.list();
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Buy and open a pack",
            description = "One atomic transaction: charges the price and adds the new cards to the collection. "
                    + "Sending the same Idempotency-Key again returns the same pack and charges nothing.")
    @ApiResponse(responseCode = "201", description = "Pack opened")
    @ApiResponse(responseCode = "400", description = "Body `code`: pack.unknown_type, pack.idempotency_key_invalid (missing or not a UUID), or validation.failed with `fieldErrors`")
    @ApiResponse(responseCode = "402", description = "Not enough credits. Body `code`: pack.insufficient_credits")
    @ApiResponse(responseCode = "404", description = "No card pool for that faction yet. Body `code`: pack.no_pool")
    @ApiResponse(responseCode = "422", description = "Idempotency-Key already used for a different pack type or faction. Body `code`: pack.idempotency_key_reused")
    public PackOpenResponse open(@AuthenticationPrincipal Jwt jwt,
                                 @Parameter(description = "A UUID made by the client, new for every purchase and "
                                         + "repeated on every retry of that purchase")
                                 @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                 @Valid @RequestBody PackOpenRequest request) {
        return packs.open(UUID.fromString(jwt.getSubject()), idempotencyKey, request);
    }
}
