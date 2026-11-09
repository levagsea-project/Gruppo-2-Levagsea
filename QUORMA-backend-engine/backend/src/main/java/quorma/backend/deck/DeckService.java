package quorma.backend.deck;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.collection.CardInstanceRepository;

/** Deck CRUD. Every save is checked (ownership, snapshot, structure) before anything is written; a bad deck is a 400. */
@Service
public class DeckService {

    private final DeckRepository decks;
    private final DeckCardRepository deckCards;
    private final CardInstanceRepository instances;
    private final CardRepository cards;
    private final CardDefinitionRepository definitions;
    private final PoolSnapshotRepository snapshots;
    private final DeckValidator validator;

    public DeckService(DeckRepository decks, DeckCardRepository deckCards, CardInstanceRepository instances,
                       CardRepository cards, CardDefinitionRepository definitions, PoolSnapshotRepository snapshots,
                       DeckValidator validator) {
        this.decks = decks;
        this.deckCards = deckCards;
        this.instances = instances;
        this.cards = cards;
        this.definitions = definitions;
        this.snapshots = snapshots;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<DeckSummary> list(UUID ownerId) {
        List<Deck> mine = decks.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        Map<UUID, Long> counts = mine.isEmpty() ? Map.of() : deckCards.findByDeckIds(mine.stream().map(Deck::getId).toList())
                .stream().collect(Collectors.groupingBy(c -> c.getId().deckId(), Collectors.counting()));
        return mine.stream()
                .map(d -> new DeckSummary(d.getId(), d.getName(), d.getFaction(), d.getSnapshotId(), d.isDefense(),
                        counts.getOrDefault(d.getId(), 0L).intValue(), d.getUpdatedAt()))
                .toList();
    }

    /** One deck of the owner with its cards; 404 if it does not exist or belongs to someone else. */
    @Transactional(readOnly = true)
    public DeckResponse get(UUID ownerId, UUID deckId) {
        // TODO(deck-get)
        throw notImplemented();
    }

    /** Creates a deck; 409 if the owner already has a deck with that name. */
    @Transactional
    public DeckResponse create(UUID ownerId, DeckRequest req) {
        // TODO(deck-create)
        throw notImplemented();
    }

    /** Replaces name, snapshot and cards of a deck (the faction never changes); same checks as {@link #create}. */
    @Transactional
    public DeckResponse update(UUID ownerId, UUID deckId, DeckUpdateRequest req) {
        // TODO(deck-update)
        throw notImplemented();
    }

    /** Deletes a deck of the owner; 409 for the defense deck. */
    @Transactional
    public void delete(UUID ownerId, UUID deckId) {
        // TODO(deck-delete)
        throw notImplemented();
    }

    /** Runs {@link #load} then {@link DeckValidator}; throws 400 listing every broken rule. */
    private List<DeckCardResponse> validate(UUID ownerId, String faction, UUID snapshotId, List<UUID> instanceIds) {
        // TODO(deck-service-validate)
        throw notImplemented();
    }

    /**
     * Joins instances, cards and snapshot definitions into views, adding a message to {@code errors} per problem:
     * repeated instance id, unknown snapshot or snapshot of another faction, instance missing or not owned (one
     * message for both, so other players' ids cannot be probed), card of another faction, card absent from the
     * snapshot.
     */
    private List<DeckCardResponse> load(UUID ownerId, String faction, UUID snapshotId, List<UUID> instanceIds,
                                    List<String> errors) {
        // TODO(deck-service-load)
        throw notImplemented();
    }

    private Deck find(UUID ownerId, UUID deckId) {
        return decks.findByIdAndOwnerId(deckId, ownerId)
                .orElseThrow(() -> new DeckException(HttpStatus.NOT_FOUND, "Deck not found"));
    }

    private void save(Deck deck) {
        try {
            decks.saveAndFlush(deck);
        } catch (DataIntegrityViolationException e) {
            throw new DeckException(HttpStatus.CONFLICT, "Deck name already in use or snapshot does not match the faction");
        }
    }

    /** Rows are built from the views, never by hand: the database checks that they are consistent with the deck. */
    private List<DeckCard> toRows(Deck deck, List<DeckCardResponse> views) {
        return views.stream()
                .map(v -> new DeckCard(new DeckCardId(deck.getId(), v.cardInstanceId()), deck.getOwnerId(), v.cardId(),
                        deck.getFaction(), deck.getSnapshotId(), v.deckSlot()))
                .toList();
    }

    private DeckResponse toResponse(Deck d, List<DeckCardResponse> cards) {
        return new DeckResponse(d.getId(), d.getName(), d.getFaction(), d.getSnapshotId(), d.isDefense(), d.getVersion(),
                d.getCreatedAt(), d.getUpdatedAt(), cards);
    }

    private static DeckException notImplemented() {
        return new DeckException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
