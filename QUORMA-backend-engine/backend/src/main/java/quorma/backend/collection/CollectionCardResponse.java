package quorma.backend.collection;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import quorma.backend.catalogue.CardPresenter;

/**
 * An owned card instance joined with its card and its definition in the active snapshot of the card's faction.
 * {@code name}, {@code flavour} and {@code sets} are card text and commercial data, in the language asked for with
 * {@code Accept-Language} (Italian as the fallback). Each ability in {@code abilities} carries its own params, which
 * fill the placeholders of its text.
 */
public record CollectionCardResponse(UUID cardInstanceId, UUID cardId, String cardCode, String faction, String kind,
                                     String name, @Schema(description = "The card's flavour text in `textLocale`; null if it has none") String flavour,
                                     @Schema(description = "The language `name` and `flavour` are in: the one asked for with Accept-Language if the card has it, else `it`") String textLocale,
                                     @Schema(description = "The sets the card belongs to: `code` is the identifier, `name` is in the best language the set has") List<CardPresenter.SetRef> sets,
                                     String subfaction, String suit, Short rank, Integer atk, Integer def,
                                     @Schema(description = "JSON array of abilities, opaque to the backend", type = "array")
                                     @JsonRawValue String abilities,
                                     String originType, Instant acquiredAt) {
}
