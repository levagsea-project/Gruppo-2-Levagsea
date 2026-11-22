package quorma.backend.pack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardKind;
import quorma.backend.catalogue.CardPresenter;
import quorma.backend.catalogue.CardSet;
import quorma.backend.catalogue.CardSetCard;
import quorma.backend.catalogue.CardSetCardRepository;
import quorma.backend.catalogue.CardSetRepository;
import quorma.backend.catalogue.CardSetStatus;
import quorma.backend.catalogue.PreferredLocales;
import quorma.backend.catalogue.CardRepository;
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

    private static final Logger LOG = LoggerFactory.getLogger(PackService.class);

    /** {@code credit_transactions.reference_type} of a pack purchase; its reference_id is the pack id. */
    private static final String LEDGER_REFERENCE_TYPE = "PACK";

    private final UserRepository users;
    private final CreditTransactionRepository ledger;
    private final ActiveSnapshotService activeSnapshots;
    private final CardDefinitionRepository definitions;
    private final CardRepository cards;
    private final CardInstanceRepository instances;
    private final CardOwnershipHistoryRepository history;
    private final PackOpeningRepository openings;
    private final CollectionService collection;
    private final PackDrawer drawer;
    private final StoredPackTypeRepository storedTypes;
    private final CardSetRepository sets;
    private final CardSetCardRepository links;
    private final CardPresenter presenter;

    public PackService(UserRepository users, CreditTransactionRepository ledger, ActiveSnapshotService activeSnapshots,
                       CardDefinitionRepository definitions, CardRepository cards, CardInstanceRepository instances,
                       CardOwnershipHistoryRepository history, PackOpeningRepository openings,
                       CollectionService collection, PackDrawer drawer, StoredPackTypeRepository storedTypes,
                       CardSetRepository sets, CardSetCardRepository links, CardPresenter presenter) {
        this.users = users;
        this.ledger = ledger;
        this.activeSnapshots = activeSnapshots;
        this.definitions = definitions;
        this.cards = cards;
        this.instances = instances;
        this.history = history;
        this.openings = openings;
        this.collection = collection;
        this.drawer = drawer;
        this.storedTypes = storedTypes;
        this.sets = sets;
        this.links = links;
        this.presenter = presenter;
    }

    /** The pack types on sale: the ones whose set is {@code IN_VENDITA}, cheapest first, each with the set it draws from. */
    @Transactional(readOnly = true)
    public List<PackTypeResponse> list(PreferredLocales locales) {
        List<StoredPackType> types = storedTypes.findAll();
        Map<UUID, CardSet> setById = new HashMap<>();
        sets.findAllById(types.stream().map(StoredPackType::getSetId).collect(Collectors.toSet()))
                .forEach(s -> setById.put(s.getId(), s));
        Map<UUID, CardPresenter.SetRef> refs = presenter.setRefs(setById.keySet(), locales);
        return types.stream()
                .filter(t -> setById.get(t.getSetId()).getStatus() == CardSetStatus.IN_VENDITA)
                .sorted(Comparator.comparingLong(StoredPackType::getPrice).thenComparing(StoredPackType::getCode))
                .map(t -> {
                    PackType type = t.toPackType();
                    return new PackTypeResponse(t.getCode(), type.price(), type.size(), type.slots(), type.supportSlots(),
                            type.featured(), refs.get(t.getSetId()));
                })
                .toList();
    }

    /**
     * Buys and opens one pack of the given type and faction.
     */
    @Transactional
    public PackOpenResponse open(UUID userId, UUID idempotencyKey, PackOpenRequest req, PreferredLocales locales) {
        // 1. Pack type: a row of pack_types, which has exactly one set.
        StoredPackType stored = storedTypes.findById(req.packType())
                .orElseThrow(() -> new PackException(HttpStatus.BAD_REQUEST, ErrorCodes.PACK_UNKNOWN_TYPE));
        PackType type = stored.toPackType();
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
            return response(opening, instances.findByOriginRef(opening.getId()), locales);
        }

        // 3b. Only a set that is on sale sells packs. After the replay above, so a retry of a purchase made before the set
        // closed still gets its pack.
        CardSet set = sets.findById(stored.getSetId()).orElseThrow();
        if (set.getStatus() != CardSetStatus.IN_VENDITA) {
            throw new PackException(HttpStatus.CONFLICT, ErrorCodes.PACK_NOT_ON_SALE);
        }

        // 4. Balance.
        if (user.getCredits() < type.price()) {
            throw new PackException(HttpStatus.PAYMENT_REQUIRED, ErrorCodes.PACK_INSUFFICIENT_CREDITS);
        }

        // 5. Pool: the cards of the pack's set that are in the active snapshot of the faction (the set's link to a card is
        // what puts it in a pack; a card in no set is in the game but in no pack).
        UUID snapshotId = activeSnapshots.activeId(faction)
                .orElseThrow(() -> new PackException(HttpStatus.NOT_FOUND, ErrorCodes.PACK_NO_POOL));
        Set<UUID> inSet = links.findBySetId(set.getId()).stream().map(CardSetCard::getCardId).collect(Collectors.toSet());
        List<UUID> poolCardIds = definitions.findBySnapshotId(snapshotId).stream()
                .map(c -> c.getCardId())
                .filter(inSet::contains)
                .toList();
        List<PoolCard> pool = cards.findAllById(poolCardIds).stream()
                .map(card -> new PoolCard(card.getId(), card.getKind(),
                        card.getKind() == CardKind.CREATURE ? Tier.ofRank(card.getRank()) : null))
                .toList();

        // 6. Draw. Nothing has been written yet: a failure here leaves no trace, not even the key.
        List<UUID> drawn;
        try {
            drawn = drawer.draw(pool, type);
        } catch (IllegalStateException e) {
            // The pool of the set cannot fill a slot of the pack (a card was taken out of the set since it was published).
            // The engine checks this at every publish; here it is a clean refusal, not a failure.
            LOG.warn("pack {} of set {} cannot be filled for {}: {}", req.packType(), set.getCode(), faction, e.getMessage());
            throw new PackException(HttpStatus.NOT_FOUND, ErrorCodes.PACK_NO_POOL);
        }

        // 7. Credits. The opening's id, generated by the server, is the pack id.
        long creditsAfter = user.getCredits() - type.price();
        PackOpening opening = openings.save(new PackOpening(userId, idempotencyKey, req.packType(), stored.getSetId(),
                faction, snapshotId, type.price(), creditsAfter));
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
        return response(opening, created, locales);
    }

    private PackOpenResponse response(PackOpening opening, List<CardInstance> packCards, PreferredLocales locales) {
        return new PackOpenResponse(opening.getId(), opening.getPackType(), opening.getSnapshotId(),
                opening.getPrice(), opening.getCreditsAfter(), collection.toViews(packCards, locales));
    }
}
