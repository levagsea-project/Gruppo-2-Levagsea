package quorma.backend.deck;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardDefinition;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardKind;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.CardSet;
import quorma.backend.catalogue.CardSetCard;
import quorma.backend.catalogue.CardSetCardRepository;
import quorma.backend.catalogue.CardSetRepository;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.publish.CatalogueProperties;
import quorma.backend.collection.CardInstance;
import quorma.backend.collection.CardInstanceRepository;
import quorma.backend.collection.CardOrigin;
import quorma.backend.collection.CardOwnershipHistory;
import quorma.backend.collection.CardOwnershipHistoryRepository;
import quorma.backend.collection.OwnershipReason;

/**
 * Every player starts with two pre-made decks, one per faction, owned through {@code card_instances} (origin
 * {@code STARTER}) and set as the defense decks, so a new player can play at once (a player must have a legal defense
 * deck for both factions to play).
 *
 * <p>The cards come from the starter set ({@code catalogue.starter.set}, the base set) as it is in the active snapshot of
 * the faction, chosen by a fixed rule so every player gets the same deck: the lowest creatures first (rank 2 upwards, the
 * Ace last, suits in alphabetical order, one per suit and rank) up to the fewest creatures the faction's deck rules
 * allow, then spells and equipment alternately by code up to 32 cards. The deck is saved through {@link DeckService}, so
 * {@link DeckValidator} checks it like any deck: a starter deck that breaks a rule is refused, never saved.
 *
 * <p>Each faction is provisioned in its own transaction, separate from the caller's, and a failure is logged and never
 * propagates: a catalogue that is empty, or a starter set too small for a deck, must not stop a player from
 * registering or logging in. A faction the player already has a deck of is left alone, so this is safe to call again
 * (at login it also covers players who registered before the catalogue existed).
 */
@Service
public class StarterDeckService {

    private static final Logger LOG = LoggerFactory.getLogger(StarterDeckService.class);

    private static final Comparator<Card> LOWEST_CREATURE_FIRST = Comparator
            .<Card>comparingInt(c -> c.getRank() == 1 ? 14 : c.getRank())
            .thenComparing(Card::getSuit)
            .thenComparing(Card::getCardCode);

    private final CatalogueProperties props;
    private final CardSetRepository sets;
    private final CardSetCardRepository links;
    private final CardRepository cards;
    private final CardDefinitionRepository definitions;
    private final ActiveSnapshotService activeSnapshots;
    private final CardInstanceRepository instances;
    private final CardOwnershipHistoryRepository history;
    private final DeckRepository decks;
    private final DeckService deckService;
    private final TransactionTemplate tx;

    public StarterDeckService(CatalogueProperties props, CardSetRepository sets, CardSetCardRepository links,
                              CardRepository cards, CardDefinitionRepository definitions,
                              ActiveSnapshotService activeSnapshots, CardInstanceRepository instances,
                              CardOwnershipHistoryRepository history, DeckRepository decks, DeckService deckService,
                              PlatformTransactionManager txManager) {
        this.props = props;
        this.sets = sets;
        this.links = links;
        this.cards = cards;
        this.definitions = definitions;
        this.activeSnapshots = activeSnapshots;
        this.instances = instances;
        this.history = history;
        this.decks = decks;
        this.deckService = deckService;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Gives the player a starter deck for each faction they have no deck of; failures are logged, never thrown. */
    public void provisionQuietly(UUID userId) {
        // read in a transaction of its own too: this may run right after the registration committed
        Set<String> have = tx.execute(status -> decks.findByOwnerIdOrderByUpdatedAtDesc(userId).stream()
                .map(Deck::getFaction).collect(Collectors.toSet()));
        for (Faction faction : Faction.values()) {
            if (have.contains(faction.name())) {
                continue;
            }
            try {
                tx.executeWithoutResult(status -> provision(userId, faction));
            } catch (RuntimeException e) {
                LOG.warn("no starter deck for user {} in {}: {}", userId, faction, e.getMessage(), e);
            }
        }
    }

    /**
     * Builds, owns, saves and sets as defense the starter deck of one faction, in the caller's transaction.
     *
     * @return the deck, or null if the starter set cannot make one (it is logged)
     */
    UUID provision(UUID userId, Faction faction) {
        CardSet set = sets.findByCode(props.starter().set()).orElse(null);
        UUID snapshotId = activeSnapshots.activeId(faction.name()).orElse(null);
        if (set == null || snapshotId == null) {
            LOG.warn("no starter deck in {}: the starter set {} or the catalogue is not there yet", faction, props.starter().set());
            return null;
        }
        Set<UUID> inSet = links.findBySetId(set.getId()).stream().map(CardSetCard::getCardId).collect(Collectors.toSet());
        Set<UUID> inSnapshot = definitions.findBySnapshotId(snapshotId).stream().map(CardDefinition::getCardId)
                .collect(Collectors.toSet());
        List<Card> pool = cards.findAllById(inSet.stream().filter(inSnapshot::contains).toList()).stream()
                .filter(c -> c.getFaction().equals(faction.name())).toList();

        List<Card> chosen = choose(faction, pool);
        if (chosen == null) {
            LOG.warn("no starter deck in {}: the starter set {} has too few cards of the faction for a legal deck", faction,
                    set.getCode());
            return null;
        }

        Instant now = Instant.now();
        List<CardInstance> owned = instances.saveAll(chosen.stream()
                .map(c -> new CardInstance(userId, c.getId(), CardOrigin.STARTER, null, now)).toList());
        history.saveAll(owned.stream()
                .map(i -> new CardOwnershipHistory(i.getId(), null, userId, OwnershipReason.STARTER, null)).toList());
        instances.flush();

        // saved like any deck: DeckValidator refuses it if the selection ever breaks a rule
        DeckResponse deck = deckService.create(userId, new DeckRequest(deckName(faction), faction.name(), snapshotId,
                owned.stream().map(CardInstance::getId).toList()));
        deckService.setDefense(userId, deck.id());
        return deck.id();
    }

    /**
     * The fixed starter selection: the lowest creatures up to the fewest the faction needs, then spells and equipment
     * alternately up to a full deck; null if the pool cannot make one.
     */
    static List<Card> choose(Faction faction, List<Card> pool) {
        int creatures = DeckValidator.minCreatures(faction);
        List<Card> chosen = new ArrayList<>();
        Set<String> slots = new HashSet<>();
        for (Card c : pool.stream().filter(c -> c.getKind() == CardKind.CREATURE).sorted(LOWEST_CREATURE_FIRST).toList()) {
            if (chosen.size() < creatures && slots.add(c.getDeckSlot())) {
                chosen.add(c);
            }
        }
        if (chosen.size() < creatures) {
            return null;
        }
        List<Card> spells = pool.stream().filter(c -> c.getKind() == CardKind.SPELL)
                .sorted(Comparator.comparing(Card::getCardCode)).toList();
        List<Card> equipment = pool.stream().filter(c -> c.getKind() == CardKind.EQUIPMENT)
                .sorted(Comparator.comparing(Card::getCardCode)).toList();
        Set<Card> support = new LinkedHashSet<>();
        for (int i = 0; i < Math.max(spells.size(), equipment.size()); i++) {
            if (i < spells.size()) {
                support.add(spells.get(i));
            }
            if (i < equipment.size()) {
                support.add(equipment.get(i));
            }
        }
        int needed = DeckValidator.DECK_SIZE - chosen.size();
        if (support.size() < needed) {
            return null;
        }
        chosen.addAll(support.stream().limit(needed).toList());
        return chosen;
    }

    private static String deckName(Faction faction) {
        return "Mazzo iniziale (" + (faction == Faction.RESISTENZA_UMANA ? "Resistenza Umana" : "Potenze Artificiali") + ")";
    }
}
