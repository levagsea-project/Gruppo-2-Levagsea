package quorma.backend.deck;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import quorma.backend.catalogue.CardDefinition;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.collection.CardInstance;
import quorma.backend.collection.CardInstanceRepository;
import quorma.backend.common.ErrorCodes;

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
                .map(d -> new DeckSummary(d.getId(), d.getName(), Faction.fromDataName(d.getFaction()).name(),
                        d.getSnapshotId(), d.isDefense(),
                        counts.getOrDefault(d.getId(), 0L).intValue(), d.getUpdatedAt()))
                .toList();
    }

    /** One deck of the owner with its cards; 404 if it does not exist or belongs to someone else. */
    @Transactional(readOnly = true)
    public DeckResponse get(UUID ownerId, UUID deckId) {
        Deck deck = find(ownerId, deckId);
        List<UUID> instanceIds = deckCards.findByDeckId(deckId).stream()
                .map(dc -> dc.getId().cardInstanceId())
                .toList();
        List<DeckCardResponse> views = load(ownerId, deck.getFaction(), deck.getSnapshotId(), instanceIds,
                new ArrayList<>());
        return toResponse(deck, views);
    }

    /** Creates a deck; 409 if the owner already has a deck with that name. */
    @Transactional
    public DeckResponse create(UUID ownerId, DeckRequest req) {
        String name = req.name().strip();
        if (decks.existsByOwnerIdAndNameAndIdNot(ownerId, name, new UUID(0, 0))) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_NAME_TAKEN);
        }
        String faction = Faction.valueOf(req.faction()).dataName();
        List<DeckCardResponse> views = validate(ownerId, faction, req.snapshotId(), req.cardInstanceIds());

        Deck deck = new Deck(ownerId, name, faction, req.snapshotId(), Instant.now());
        save(deck);
        deckCards.saveAll(toRows(deck, views));
        return toResponse(deck, views);
    }

    /** Replaces name, snapshot and cards of a deck (the faction never changes); same checks as {@link #create}. */
    @Transactional
    public DeckResponse update(UUID ownerId, UUID deckId, DeckUpdateRequest req) {
        Deck deck = find(ownerId, deckId);
        String name = req.name().strip();
        if (decks.existsByOwnerIdAndNameAndIdNot(ownerId, name, deckId)) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_NAME_TAKEN);
        }
        List<DeckCardResponse> views = validate(ownerId, deck.getFaction(), req.snapshotId(), req.cardInstanceIds());

        deckCards.deleteByDeckId(deckId);
        // The bulk delete clears the persistence context, so reload the deck before updating it.
        deck = find(ownerId, deckId);
        deck.setName(name);
        deck.setSnapshotId(req.snapshotId());
        deck.setUpdatedAt(Instant.now());
        save(deck);
        deckCards.saveAll(toRows(deck, views));
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

    /** Runs {@link #load} then {@link DeckValidator}; throws 400 listing every broken rule. */
    private List<DeckCardResponse> validate(UUID ownerId, String faction, UUID snapshotId, List<UUID> instanceIds) {
        List<DeckError> errors = new ArrayList<>();
        List<DeckCardResponse> views = load(ownerId, faction, snapshotId, instanceIds, errors);
        errors.addAll(validator.validate(Faction.fromDataName(faction), views));
        if (!errors.isEmpty()) {
            throw new DeckException(HttpStatus.BAD_REQUEST, ErrorCodes.DECK_INVALID, errors);
        }
        return views;
    }

    /**
     * Joins instances, cards and snapshot definitions into views, adding a message to {@code errors} per problem:
     * repeated instance id, unknown snapshot or snapshot of another faction, instance missing or not owned (one
     * message for both, so other players' ids cannot be probed), card of another faction, card absent from the
     * snapshot.
     */
    private List<DeckCardResponse> load(UUID ownerId, String faction, UUID snapshotId, List<UUID> instanceIds,
                                        List<DeckError> errors) {
        // repeated instance ids (order of the request is kept)
        Set<UUID> unique = new LinkedHashSet<>();
        Set<UUID> reported = new HashSet<>();
        for (UUID id : instanceIds) {
            if (!unique.add(id) && reported.add(id)) {
                errors.add(DeckError.of(ErrorCodes.DECK_INSTANCE_DUPLICATED, "id", id));
            }
        }

        // snapshot
        boolean snapshotOk = snapshots.findById(snapshotId)
                .filter(s -> Objects.equals(s.getFaction(), faction))
                .isPresent();
        if (!snapshotOk) {
            errors.add(DeckError.of(ErrorCodes.DECK_SNAPSHOT_INVALID));
        }

        // instances: missing or not owned give the same message
        Map<UUID, CardInstance> instMap = instances.findAllById(unique).stream()
                .filter(i -> Objects.equals(i.getOwnerId(), ownerId))
                .collect(Collectors.toMap(CardInstance::getInstanceId, i -> i));
        for (UUID id : unique) {
            if (!instMap.containsKey(id)) {
                errors.add(DeckError.of(ErrorCodes.DECK_INSTANCE_NOT_FOUND, "id", id));
            }
        }

        // cards
        Set<UUID> cardIds = instMap.values().stream().map(CardInstance::getCardId).collect(Collectors.toSet());
        var cardMap = cards.findAllById(cardIds).stream().collect(Collectors.toMap(c -> c.getId(), c -> c));

        // definitions in the snapshot
        Map<UUID, CardDefinition> defMap = new HashMap<>();
        if (snapshotOk && !cardIds.isEmpty()) {
            definitions.findBySnapshotIdAndCardIdIn(snapshotId, cardIds)
                    .forEach(d -> defMap.put(d.getCardId(), d));
        }

        List<DeckCardResponse> result = new ArrayList<>();
        for (UUID id : unique) {
            CardInstance inst = instMap.get(id);
            if (inst == null) {
                continue;
            }
            var card = cardMap.get(inst.getCardId());
            if (card == null) {
                continue;
            }
            if (!Objects.equals(card.getFaction(), faction)) {
                errors.add(DeckError.of(ErrorCodes.DECK_CARD_WRONG_FACTION, "card", card.getCardCode()));
                continue;
            }
            CardDefinition def = defMap.get(card.getId());
            if (def == null) {
                if (snapshotOk) {
                    errors.add(DeckError.of(ErrorCodes.DECK_CARD_NO_DEFINITION, "card", card.getCardCode()));
                }
                continue;
            }
            result.add(new DeckCardResponse(inst.getInstanceId(), card.getId(), card.getCardCode(), card.getKind().name(),
                    def.getName(), card.getSubfaction(), card.getSuit(), card.getRank(), def.getAtk(), def.getDef(),
                    def.getAbilities(), card.getDeckSlot()));
        }
        return result;
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

    /** Rows are built from the views, never by hand: the database checks that they are consistent with the deck. */
    private List<DeckCard> toRows(Deck deck, List<DeckCardResponse> views) {
        return views.stream()
                .map(v -> new DeckCard(new DeckCardId(deck.getId(), v.cardInstanceId()), deck.getOwnerId(), v.cardId(),
                        deck.getFaction(), deck.getSnapshotId(), v.deckSlot()))
                .toList();
    }

    private DeckResponse toResponse(Deck d, List<DeckCardResponse> cards) {
        return new DeckResponse(d.getId(), d.getName(), Faction.fromDataName(d.getFaction()).name(), d.getSnapshotId(),
                d.isDefense(), d.getVersion(),
                d.getCreatedAt(), d.getUpdatedAt(), cards);
    }
}
