package quorma.backend.deck;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for returning a summary of a deck. Used for listing decks without their cards.
 */
public record DeckSummary(UUID id, String name, String faction, UUID snapshotId, boolean defense, int cardCount,
                          Instant updatedAt) {
}
