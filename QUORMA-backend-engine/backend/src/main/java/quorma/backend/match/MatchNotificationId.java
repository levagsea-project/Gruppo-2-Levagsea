package quorma.backend.match;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record MatchNotificationId(UUID matchId, UUID userId) {
}
