package quorma.backend.telemetry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/stats")
@Tag(name = "Stats", description = "A player's statistics, summed from the match logs in the background. Players only.")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping
    @Operation(operationId = "getMyStats", summary = "My statistics",
            description = "Series (best of three) and single games played and won, damage, spill damage and kills, the poker hands the player had and the record with each faction. The figures are added shortly after a series ends (in the background), so the series just played may appear a moment later. A player with no series gets zeros.")
    public StatsResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return stats.forUser(UUID.fromString(jwt.getSubject()));
    }
}
