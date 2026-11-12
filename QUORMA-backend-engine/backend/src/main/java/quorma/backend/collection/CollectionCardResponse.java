package quorma.backend.collection;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * An owned card instance joined with its card and its definition in a pool snapshot of the card's faction.
 */
public record CollectionCardResponse(UUID cardInstanceId, UUID cardId, String cardCode, String faction, String kind,
                                     String name, String subfaction, String suit, Short rank, Integer atk, Integer def,
                                     @Schema(description = "JSON array of abilities, opaque to the backend", type = "array")
                                     @JsonRawValue String abilities,
                                     String originType, Instant acquiredAt) {
}
