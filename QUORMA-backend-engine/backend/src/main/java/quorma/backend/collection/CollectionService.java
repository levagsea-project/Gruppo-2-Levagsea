package quorma.backend.collection;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.PoolSnapshotRepository;

/** Read side of the collection. */
@Service
public class CollectionService {

    private final CardInstanceRepository instances;
    private final CardRepository cards;
    private final CardDefinitionRepository definitions;
    private final PoolSnapshotRepository snapshots;

    public CollectionService(CardInstanceRepository instances, CardRepository cards,
                             CardDefinitionRepository definitions, PoolSnapshotRepository snapshots) {
        this.instances = instances;
        this.cards = cards;
        this.definitions = definitions;
        this.snapshots = snapshots;
    }

    /** Every instance owned by the user, newest first, with card data from the latest snapshot of each faction. */
    @Transactional(readOnly = true)
    public CollectionResponse get(UUID ownerId) {
        // TODO(collection-get)
        throw new UnsupportedOperationException("CollectionService.get");
    }

    /**
     * Turns instances into views. Loads their cards in one query, then the definitions of those cards in the latest
     * snapshot of each faction in one query per faction (no query per instance). Also used by
     * {@code PackService} for the cards of a new pack. A card missing from the latest snapshot (possible when a
     * newer pool drops it) still appears, with null name, stats and abilities.
     */
    @Transactional(readOnly = true)
    public List<CollectionCardResponse> toViews(List<CardInstance> owned) {
        // TODO(collection-views)
        throw new UnsupportedOperationException("CollectionService.toViews");
    }
}
