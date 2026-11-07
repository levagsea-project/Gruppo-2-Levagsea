package quorma.backend.telemetry;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record CardStatsId(UUID snapshotId, UUID cardId) {
}
