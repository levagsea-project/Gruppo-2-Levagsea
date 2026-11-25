package quorma.backend.telemetry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.PreferredLocales;

@RestController
@RequestMapping("/admin/telemetry")
@Tag(name = "Admin telemetry", description = "Global statistics for balance tuning. Admins only (covered by `/admin/**`).")
public class AdminTelemetryController {

    private final StatsService stats;

    public AdminTelemetryController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/cards")
    @Operation(operationId = "getCardTelemetry", summary = "Global figures for each card of a snapshot",
            description = "All players' games, wins, damage, spill damage and kills for each card of one pool snapshot, most played first: the active snapshot of `faction`, or `snapshotId` if given. Win rate is `matchesWon / matchesPlayed`. Cards never played are not listed.")
    public List<CardFiguresResponse> cards(
            @Parameter(description = "The faction whose active snapshot is read") @RequestParam Faction faction,
            @Parameter(description = "A specific pool snapshot (of that faction) instead of the active one") @RequestParam(required = false) UUID snapshotId,
            @Parameter(description = "Language of the card name: the first language in the list that a card has, Italian as the fallback.") @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return stats.cardsOfSnapshot(faction, snapshotId, PreferredLocales.parse(acceptLanguage));
    }
}
