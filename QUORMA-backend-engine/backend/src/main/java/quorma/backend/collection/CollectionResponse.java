package quorma.backend.collection;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/** The player's collection: one entry per card instance (two copies of a card are two entries). */
public record CollectionResponse(
        @Schema(description = "Snapshots used to resolve card data: the latest one of each faction") List<UUID> snapshotIds,
        List<CollectionCardResponse> cards) {
}
