package quorma.backend.deck;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for returning a deck with its cards.
 */
public record DeckResponse(UUID id, String name, String faction, UUID snapshotId, boolean defense, Long version,
                           Instant createdAt, Instant updatedAt, List<DeckCardResponse> cards) {
}
