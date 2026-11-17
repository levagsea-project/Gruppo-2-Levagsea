package quorma.backend.deck;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * A card in a deck: the owned instance joined with its card and its definition in the deck's snapshot.
 */
public record DeckCardResponse(UUID cardInstanceId, UUID cardId, String cardCode, String kind, String name,
                               String subfaction, String suit, Short rank, Integer atk, Integer def,
                               @Schema(description = "JSON array of abilities, opaque to the backend", type = "array")
                               @JsonRawValue String abilities,
                               String deckSlot) {
}
