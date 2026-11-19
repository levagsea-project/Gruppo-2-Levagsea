package quorma.backend.deck;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.PreferredLocales;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;

/** Deck CRUD. Every save is checked (ownership, snapshot, structure) before anything is written; a bad deck is a 400. */
@Service
public class DeckService {

    private final DeckRepository decks;
    private final DeckCardRepository deckCards;
    private final DeckLoader loader;
    private final ActiveSnapshotService activeSnapshots;
    private final DeckValidator validator;
    private final DeckEligibility eligibility;

    public DeckService(DeckRepository decks, DeckCardRepository deckCards, DeckLoader loader,
                       ActiveSnapshotService activeSnapshots, DeckValidator validator, DeckEligibility eligibility) {
        this.decks = decks;
        this.deckCards = deckCards;
        this.loader = loader;
        this.activeSnapshots = activeSnapshots;
        this.validator = validator;
        this.eligibility = eligibility;
    }

    @Transactional(readOnly = true)
    public List<DeckSummary> list(UUID ownerId) {
        List<Deck> mine = decks.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        Map<UUID, Long> counts = mine.isEmpty() ? Map.of() : deckCards.findByDeckIds(mine.stream().map(d -> d.getId()).toList())
                .stream().collect(Collectors.groupingBy(c -> c.getId().deckId(), Collectors.counting()));
        return mine.stream()
                .map(d -> new DeckSummary(d.getId(), d.getName(), d.getFaction(),
                        d.getSnapshotId(), d.isDefense(), d.isOutdated(),
                        counts.getOrDefault(d.getId(), 0L).intValue(), d.getUpdatedAt()))
                .toList();
    }

    /** One deck of the owner with its cards; 404 if it does not exist or belongs to someone else. */
    @Transactional(readOnly = true)
    public DeckResponse get(UUID ownerId, UUID deckId) {
        return get(ownerId, deckId, PreferredLocales.DEFAULT);
    }

    /** Same, with the card text in the languages asked for (Italian as the fallback). */
    @Transactional(readOnly = true)
    public DeckResponse get(UUID ownerId, UUID deckId, PreferredLocales locales) {
        Deck deck = find(ownerId, deckId);
        List<UUID> instanceIds = deckCards.findByDeckId(deckId).stream()
                .map(dc -> dc.getId().cardInstanceId())
                .toList();
        List<DeckCardResponse> views = loader.load(ownerId, deck.getFaction(), deck.getSnapshotId(), instanceIds,
                new ArrayList<>(), locales);
        return toResponse(deck, views);
    }

    /** Creates a deck; 409 if the owner already has a deck with that name. */
    @Transactional
    public DeckResponse create(UUID ownerId, DeckRequest req) {
        return create(ownerId, req, PreferredLocales.DEFAULT);
    }

    /** Same, with the card text in the languages asked for. */
    @Transactional
    public DeckResponse create(UUID ownerId, DeckRequest req, PreferredLocales locales) {
        String name = req.name().strip();
        if (decks.existsByOwnerIdAndNameAndIdNot(ownerId, name, new UUID(0, 0))) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_NAME_TAKEN);
        }
        String faction = req.faction();
        List<DeckCardResponse> views = validate(ownerId, faction, req.snapshotId(), req.cardInstanceIds(), locales);

        Deck deck = new Deck(ownerId, name, faction, req.snapshotId(), Instant.now());
        deck.setOutdated(!isLatest(faction, req.snapshotId()));
        save(deck);
        deckCards.saveAll(loader.toRows(deck, views));
        return toResponse(deck, views);
    }

    /** Replaces name, snapshot and cards of a deck (the faction never changes); same checks as {@link #create}. */
    @Transactional
    public DeckResponse update(UUID ownerId, UUID deckId, DeckUpdateRequest req) {
        return update(ownerId, deckId, req, PreferredLocales.DEFAULT);
    }

    /** Same, with the card text in the languages asked for. */
    @Transactional
    public DeckResponse update(UUID ownerId, UUID deckId, DeckUpdateRequest req, PreferredLocales locales) {
        Deck deck = find(ownerId, deckId);
        String name = req.name().strip();
        if (decks.existsByOwnerIdAndNameAndIdNot(ownerId, name, deckId)) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_NAME_TAKEN);
        }
        List<DeckCardResponse> views = validate(ownerId, deck.getFaction(), req.snapshotId(), req.cardInstanceIds(), locales);

        deckCards.deleteByDeckId(deckId);
        // The bulk delete clears the persistence context, so reload the deck before updating it.
        deck = find(ownerId, deckId);
        deck.setName(name);
        deck.setSnapshotId(req.snapshotId());
        // A legal deck on the latest snapshot is no longer outdated; on an older one it stays so.
        deck.setOutdated(!isLatest(deck.getFaction(), req.snapshotId()));
        deck.setUpdatedAt(Instant.now());
        save(deck);
        deckCards.saveAll(loader.toRows(deck, views));
        return toResponse(deck, views);
    }

    /** Deletes a deck of the owner; 409 for the defense deck. */
    @Transactional
    public void delete(UUID ownerId, UUID deckId) {
        Deck deck = find(ownerId, deckId);
        if (deck.isDefense()) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_DEFENSE_UNDELETABLE);
        }
        decks.delete(deck);
    }

    /**
     * Makes a deck the defense deck of its faction, replacing the current one. 409 {@code deck.outdated} for an
     * outdated deck. This is how a blocked player gets playing again.
     */
    @Transactional
    public DeckResponse setDefense(UUID ownerId, UUID deckId) {
        return setDefense(ownerId, deckId, PreferredLocales.DEFAULT);
    }

    /** Same, with the card text in the languages asked for. */
    @Transactional
    public DeckResponse setDefense(UUID ownerId, UUID deckId, PreferredLocales locales) {
        Deck deck = find(ownerId, deckId);
        eligibility.requireSelectable(deck);
        if (!deck.isDefense()) {
            for (Deck current : decks.findByOwnerIdAndDefenseTrue(ownerId)) {
                if (current.getFaction().equals(deck.getFaction())) {
                    current.setDefense(false);
                    decks.saveAndFlush(current); // the unique index allows one defense deck per owner and faction
                }
            }
            deck.setDefense(true);
            save(deck);
        }
        return get(ownerId, deckId, locales);
    }

    /** Outdated means illegal on the active snapshot; a deck saved on another snapshot is flagged until it is moved. */
    private boolean isLatest(String faction, UUID snapshotId) {
        return activeSnapshots.isActive(faction, snapshotId);
    }

    /** Runs {@link #load} then {@link DeckValidator}; throws 400 listing every broken rule. */
    private List<DeckCardResponse> validate(UUID ownerId, String faction, UUID snapshotId, List<UUID> instanceIds,
                                            PreferredLocales locales) {
        List<DeckError> errors = new ArrayList<>();
        List<DeckCardResponse> views = loader.load(ownerId, faction, snapshotId, instanceIds, errors, locales);
        errors.addAll(validator.validate(Faction.valueOf(faction), views));
        if (!errors.isEmpty()) {
            throw new DeckException(HttpStatus.BAD_REQUEST, ErrorCodes.DECK_INVALID, errors);
        }
        return views;
    }

    private Deck find(UUID ownerId, UUID deckId) {
        return decks.findByIdAndOwnerId(deckId, ownerId)
                .orElseThrow(() -> new DeckException(HttpStatus.NOT_FOUND, ErrorCodes.DECK_NOT_FOUND));
    }

    private void save(Deck deck) {
        try {
            decks.saveAndFlush(deck);
        } catch (DataIntegrityViolationException e) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_CONFLICT);
        }
    }

    private DeckResponse toResponse(Deck d, List<DeckCardResponse> cards) {
        return new DeckResponse(d.getId(), d.getName(), d.getFaction(), d.getSnapshotId(),
                d.isDefense(), d.isOutdated(), d.getVersion(),
                d.getCreatedAt(), d.getUpdatedAt(), cards);
    }
}
