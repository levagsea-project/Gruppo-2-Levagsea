package quorma.backend.pack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardKind;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.PoolSnapshot;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.collection.CardInstance;
import quorma.backend.collection.CardInstanceRepository;
import quorma.backend.collection.CardOrigin;
import quorma.backend.collection.CardOwnershipHistory;
import quorma.backend.collection.CardOwnershipHistoryRepository;
import quorma.backend.collection.CollectionService;
import quorma.backend.collection.OwnershipReason;
import quorma.backend.common.ErrorCodes;
import quorma.backend.pack.PackDrawer.PoolCard;
import quorma.backend.user.CreditTransaction;
import quorma.backend.user.CreditTransactionRepository;
import quorma.backend.user.CreditTransactionType;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/**
 * Opening a pack: charge credits and create the card instances in one transaction. If anything fails, nothing is kept.
 */
@Service
public class PackService {

    /** {@code credit_transactions.reference_type} of a pack purchase; its reference_id is the pack id. */
    private static final String LEDGER_REFERENCE_TYPE = "PACK";

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
    private final StoredPackTypeRepository storedTypes;

    public PackService(UserRepository users, CreditTransactionRepository ledger, PoolSnapshotRepository snapshots,
                       CardDefinitionRepository definitions, CardRepository cards, CardInstanceRepository instances,
                       CardOwnershipHistoryRepository history, PackOpeningRepository openings,
                       CollectionService collection, PackDrawer drawer, PackProperties props,
                       StoredPackTypeRepository storedTypes) {
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
        this.storedTypes = storedTypes;
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
        // 1. Pack type.
        PackType type = props.types().get(req.packType());
        if (type == null) {
            throw new PackException(HttpStatus.BAD_REQUEST, ErrorCodes.PACK_UNKNOWN_TYPE);
        }
        // Interim (schema step): the set comes from the stored pack type of the same code. The shop will read
        // price and slots from pack_types too (packs and starter decks step); until then a type with no row is unknown.
        StoredPackType stored = storedTypes.findById(req.packType())
                .orElseThrow(() -> new PackException(HttpStatus.BAD_REQUEST, ErrorCodes.PACK_UNKNOWN_TYPE));
        String faction = req.faction();

        // 2. Lock the user row before reading the balance: a concurrent purchase of the same user waits here until
        // this transaction ends, then sees the balance it left.
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("user " + userId + " from a valid JWT not found"));

        // 3. Repeated request: before the balance check, a retry gets its pack even if the balance is now too low.
        Optional<PackOpening> previous = openings.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            PackOpening opening = previous.get();
            if (!opening.getPackType().equals(req.packType()) || !opening.getFaction().equals(faction)) {
                throw new PackException(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.PACK_IDEMPOTENCY_KEY_REUSED);
            }
            // Whoever owns them now: the cards of the pack are returned even if they were traded since.
            return response(opening, instances.findByOriginRef(opening.getId()));
        }

        // 4. Balance.
        if (user.getCredits() < type.price()) {
            throw new PackException(HttpStatus.PAYMENT_REQUIRED, ErrorCodes.PACK_INSUFFICIENT_CREDITS);
        }

        // 5. Pool: the latest snapshot of the faction.
        PoolSnapshot snapshot = snapshots.findFirstByFactionOrderByCreatedAtDesc(faction)
                .orElseThrow(() -> new PackException(HttpStatus.NOT_FOUND, ErrorCodes.PACK_NO_POOL));
        List<UUID> poolCardIds = definitions.findBySnapshotId(snapshot.getId()).stream()
                .map(c -> c.getCardId())
                .toList();
        List<PoolCard> pool = cards.findAllById(poolCardIds).stream()
                .map(card -> new PoolCard(card.getId(), card.getKind(),
                        card.getKind() == CardKind.CREATURE ? Tier.ofRank(card.getRank()) : null))
                .toList();

        // 6. Draw. Nothing has been written yet: a failure here leaves no trace, not even the key.
        List<UUID> drawn = drawer.draw(pool, type);

        // 7. Credits. The opening's id, generated by the server, is the pack id.
        long creditsAfter = user.getCredits() - type.price();
        PackOpening opening = openings.save(new PackOpening(userId, idempotencyKey, req.packType(), stored.getSetId(),
                faction, snapshot.getId(), type.price(), creditsAfter));
        user.setCredits(creditsAfter);
        ledger.save(new CreditTransaction(userId, -type.price(), creditsAfter, CreditTransactionType.PACK_PURCHASE,
                LEDGER_REFERENCE_TYPE, opening.getId()));

        // 8. Cards: one instance per drawn card, even for duplicates. Instances first, the history needs their ids.
        Instant now = Instant.now();
        List<CardInstance> created = new ArrayList<>(drawn.size());
        for (UUID cardId : drawn) {
            created.add(new CardInstance(userId, cardId, CardOrigin.PACK, opening.getId(), now));
        }
        instances.saveAll(created);
        List<CardOwnershipHistory> entries = new ArrayList<>(created.size());
        for (CardInstance instance : created) {
            entries.add(new CardOwnershipHistory(instance.getId(), null, userId, OwnershipReason.PACK, null));
        }
        history.saveAll(entries);

        // 9. Response. Flush so a constraint violation surfaces here, inside the transaction.
        instances.flush();
        return response(opening, created);
    }

    private PackOpenResponse response(PackOpening opening, List<CardInstance> packCards) {
        return new PackOpenResponse(opening.getId(), opening.getPackType(), opening.getSnapshotId(),
                opening.getPrice(), opening.getCreditsAfter(), collection.toViews(packCards));
    }
}
