package quorma.backend.match;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardDefinition;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardKind;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.Deck;
import quorma.backend.deck.DeckCard;
import quorma.backend.deck.DeckCardRepository;
import quorma.backend.deck.DeckEligibility;
import quorma.backend.deck.DeckException;
import quorma.backend.deck.DeckRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Turns decks into the snapshots a series keeps: for each deck the engine's {@code player.deck} array with the
 * full card definitions of the deck's pool snapshot. The snapshot is what makes a replay possible later, so it is
 * built once, here, and stored as JSON.
 */
@Service
public class DeckSnapshotService {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final DeckRepository decks;
    private final DeckCardRepository deckCards;
    private final CardRepository cards;
    private final CardDefinitionRepository definitions;
    private final DeckEligibility eligibility;

    public DeckSnapshotService(DeckRepository decks, DeckCardRepository deckCards, CardRepository cards,
                               CardDefinitionRepository definitions, DeckEligibility eligibility) {
        this.decks = decks;
        this.deckCards = deckCards;
        this.cards = cards;
        this.definitions = definitions;
        this.eligibility = eligibility;
    }

    /**
     * The decks a player puts up: exactly one per faction, each of their own, not outdated.
     * 400 {@code challenge.decks_invalid} for a wrong list, 404 {@code deck.not_found} for a deck that is not theirs.
     */
    @Transactional(readOnly = true)
    public Map<Faction, LockedDeck> lockChoices(UUID ownerId, List<DeckChoice> choices) {
        if (choices == null || choices.size() != Faction.values().length) {
            throw new ChallengeException(HttpStatus.BAD_REQUEST, ErrorCodes.CHALLENGE_DECKS_INVALID);
        }
        Set<Faction> seen = new HashSet<>();
        Map<Faction, LockedDeck> locked = new EnumMap<>(Faction.class);
        for (DeckChoice choice : choices) {
            Faction faction = Faction.valueOf(choice.faction());
            if (!seen.add(faction)) {
                throw new ChallengeException(HttpStatus.BAD_REQUEST, ErrorCodes.CHALLENGE_DECKS_INVALID);
            }
            Deck deck = decks.findByIdAndOwnerId(choice.deckId(), ownerId)
                    .orElseThrow(() -> new DeckException(HttpStatus.NOT_FOUND, ErrorCodes.DECK_NOT_FOUND));
            if (!deck.getFaction().equals(faction.name())) {
                throw new ChallengeException(HttpStatus.BAD_REQUEST, ErrorCodes.CHALLENGE_DECKS_INVALID);
            }
            eligibility.requireSelectable(deck);
            locked.put(faction, lock(deck));
        }
        return locked;
    }

    /** The defense decks of a player, one per faction (matchmaking: the defender does not choose). */
    @Transactional(readOnly = true)
    public Map<Faction, LockedDeck> lockDefense(UUID defenderId) {
        Map<Faction, LockedDeck> locked = new EnumMap<>(Faction.class);
        for (Deck deck : decks.findByOwnerIdAndDefenseTrue(defenderId)) {
            eligibility.requireSelectable(deck);
            locked.put(Faction.valueOf(deck.getFaction()), lock(deck));
        }
        if (locked.size() != Faction.values().length) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_DEFENSE_REQUIRED);
        }
        return locked;
    }

    /** Builds the snapshot of one deck from its cards and the definitions of the deck's pool snapshot. */
    @Transactional(readOnly = true)
    public LockedDeck lock(Deck deck) {
        List<DeckCard> rows = deckCards.findByDeckId(deck.getId());
        List<UUID> cardIds = rows.stream().map(DeckCard::getCardId).distinct().toList();
        Map<UUID, Card> byId = cards.findAllById(cardIds).stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));
        Map<UUID, CardDefinition> defs = definitions.findBySnapshotIdAndCardIdIn(deck.getSnapshotId(), cardIds).stream()
                .collect(Collectors.toMap(CardDefinition::getCardId, Function.identity()));

        // Sorted by card code: the order of the deck before the shuffle is part of the recipe, so it must be stable.
        List<Card> ordered = cardIds.stream().map(byId::get).sorted(Comparator.comparing(Card::getCardCode)).toList();
        ArrayNode array = MAPPER.createArrayNode();
        for (Card card : ordered) {
            CardDefinition def = defs.get(card.getId());
            if (def == null) {
                throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_CARD_NO_DEFINITION);
            }
            array.add(engineCard(card, def));
        }
        return new LockedDeck(Faction.valueOf(deck.getFaction()), deck.getSnapshotId(), MAPPER.writeValueAsString(array));
    }

    /** One card in the engine's input format (match-input.schema.json). */
    static ObjectNode engineCard(Card card, CardDefinition def) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", card.getCardCode());
        node.put("kind", card.getKind().name());
        if (card.getKind() == CardKind.CREATURE) {
            node.put("rank", card.getRank().intValue());
            node.put("suit", card.getSuit());
            node.put("subfaction", card.getSubfaction().name());
            node.put("atk", def.getAtk());
            node.put("def", def.getDef());
        } else {
            node.put("faction", card.getFaction());
        }
        JsonNode abilities = MAPPER.readTree(def.getAbilities());
        node.set("abilities", abilities);
        return node;
    }
}
