package quorma.backend.telemetry;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record UserHandStatsId(UUID userId, String handType) {
}
