package quorma.backend.pack;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import quorma.backend.collection.CollectionCardResponse;

/** Result of opening a pack: what was paid, the new balance and the new card instances. */
public record PackOpenResponse(
        @Schema(description = "Id of this opening, made by the server: the origin_ref of the new instances and the reference_id of the ledger row")
        UUID packId,
        String packType,
        UUID snapshotId,
        long price,
        long creditsAfter,
        List<CollectionCardResponse> cards) {
}
