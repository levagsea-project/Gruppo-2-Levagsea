package quorma.backend.telemetry;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record UserFactionStatsId(UUID userId, String faction) {
}
