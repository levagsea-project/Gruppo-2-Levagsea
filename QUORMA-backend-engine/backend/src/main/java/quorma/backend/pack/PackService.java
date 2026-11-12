package quorma.backend.pack;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.collection.CardInstanceRepository;
import quorma.backend.collection.CardOwnershipHistoryRepository;
import quorma.backend.collection.CollectionService;
import quorma.backend.common.ErrorCodes;
import quorma.backend.user.CreditTransactionRepository;
import quorma.backend.user.UserRepository;

/**
 * Opening a pack: charge credits and create the card instances in one transaction. If anything fails, nothing is kept.
 */
@Service
public class PackService {

    private final UserRepository users;
    private final CreditTransactionRepository ledger;
    private final PoolSnapshotRepository snapshots;
    private final CardDefinitionRepository definitions;
    private final CardRepository cards;
    private final CardInstanceRepository instances;
    private final CardOwnershipHistoryRepository history;
    private final PackOpeningRepository openings;
    private final CollectionService collection;
    private final PackDrawer drawer;
    private final PackProperties props;

    public PackService(UserRepository users, CreditTransactionRepository ledger, PoolSnapshotRepository snapshots,
                       CardDefinitionRepository definitions, CardRepository cards, CardInstanceRepository instances,
                       CardOwnershipHistoryRepository history, PackOpeningRepository openings,
                       CollectionService collection, PackDrawer drawer, PackProperties props) {
        this.users = users;
        this.ledger = ledger;
        this.snapshots = snapshots;
        this.definitions = definitions;
        this.cards = cards;
        this.instances = instances;
        this.history = history;
        this.openings = openings;
        this.collection = collection;
        this.drawer = drawer;
        this.props = props;
    }

    /** The pack types on sale. */
    public List<PackTypeResponse> list() {
        return props.types().entrySet().stream()
                .map(e -> new PackTypeResponse(e.getKey(), e.getValue().price(), e.getValue().size(),
                        e.getValue().slots(), e.getValue().supportSlots(), e.getValue().featured()))
                .toList();
    }

    /**
     * Buys and opens one pack of the given type and faction.
     */
    @Transactional
    public PackOpenResponse open(UUID userId, UUID idempotencyKey, PackOpenRequest req) {
        // TODO(pack-open)
        throw notImplemented();
    }

    private static PackException notImplemented() {
        return new PackException(HttpStatus.NOT_IMPLEMENTED, ErrorCodes.NOT_IMPLEMENTED);
    }
}
