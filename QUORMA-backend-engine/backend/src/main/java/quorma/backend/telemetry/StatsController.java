package quorma.backend.telemetry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import quorma.backend.catalogue.PreferredLocales;

@RestController
@Tag(name = "Stats", description = "A player's statistics, summed from the match logs in the background. Players only.")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/me/stats")
    @Operation(operationId = "getMyStats", summary = "My statistics",
            description = "Series (best of three) and single games played and won, damage, spill damage and kills, the poker hands the player had and the record with each faction. The figures are added shortly after a series ends (in the background), so the series just played may appear a moment later. A player with no series gets zeros.")
    public StatsResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return stats.forUser(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/users/{username}/stats")
    @Operation(operationId = "getUserStats", summary = "Another player's statistics",
            description = "The same figures as `GET /me/stats` for the player with that username (any player can read them: they are public, like the profile). 404 `user.not_found` if there is no such player; admins do not play and are not found.")
    public StatsResponse ofUser(@PathVariable String username) {
        return stats.forUsername(username);
    }

    @GetMapping("/me/stats/cards")
    @Operation(operationId = "getMyCardStats", summary = "My statistics for each card",
            description = "For each card the player has played: games, wins, times drawn, damage, spill damage and kills; most played first. Cards never played are not listed. Meant as a deckbuilding help.")
    public List<CardFiguresResponse> myCards(@AuthenticationPrincipal Jwt jwt,
                                             @Parameter(description = "Language of the card name: the first language in the list that a card has, Italian as the fallback (`textLocale` says which).") @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return stats.cardsOfUser(UUID.fromString(jwt.getSubject()), PreferredLocales.parse(acceptLanguage));
    }
}
