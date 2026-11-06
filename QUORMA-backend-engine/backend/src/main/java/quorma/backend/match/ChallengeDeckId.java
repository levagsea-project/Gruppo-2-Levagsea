package quorma.backend.match;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record ChallengeDeckId(UUID challengeId, String side, String faction) {
}
