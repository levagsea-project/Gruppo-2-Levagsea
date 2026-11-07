package quorma.backend.deck;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record DeckCardId(UUID deckId, UUID cardInstanceId) {
}
