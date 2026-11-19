package quorma.backend.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import quorma.backend.auth.AuthService;
import quorma.backend.auth.LoginRequest;
import quorma.backend.auth.RegisterRequest;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardKind;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.DraftItemType;
import quorma.backend.catalogue.publish.CataloguePublisher;
import quorma.backend.catalogue.publish.DraftService;
import quorma.backend.collection.CardInstance;
import quorma.backend.collection.CardInstanceRepository;
import quorma.backend.collection.CardOrigin;
import quorma.backend.support.BaseSet;
import quorma.backend.support.IntegrationTest;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/**
 * Decks follow the active snapshot when their owner logs in: moved if legal there, flagged outdated if not, and a
 * failure never blocks the login. Publishing itself never touches a deck.
 */
class DeckMigrationTest extends IntegrationTest {

    private static final String RES = "RESISTENZA_UMANA";
    private static final String AI = "POTENZE_ARTIFICIALI";

    @Autowired
    DraftService drafts;
    @Autowired
    CataloguePublisher publisher;
    @Autowired
    UserRepository users;
    @Autowired
    AuthService auth;
    @Autowired
    DeckService deckService;
    @Autowired
    DeckMigrator migrator;
    @Autowired
    DeckEligibility eligibility;
    @Autowired
    DeckRepository decks;
    @Autowired
    DeckCardRepository deckCards;
    @Autowired
    CardRepository cards;
    @Autowired
    CardInstanceRepository instances;
    @MockitoSpyBean
    DeckLoader loader;

    UUID admin;
    UUID player;
    UUID s1;
    List<UUID> resDeck;
    List<UUID> aiDeck;

    @BeforeEach
    void setUp() {
        admin = users.save(new User("sistema", "sistema@quorma.local", "x", Role.ADMIN, 0)).getId();
        auth.register(new RegisterRequest("giocatore", "g@quorma.local", "password123"));
        player = users.findByUsernameIgnoreCase("giocatore").orElseThrow().getId();

        UUID draft = drafts.open("seed", null, admin).getId();
        BaseSet.fill(drafts, draft);
        publisher.publish(draft, admin, null);
        s1 = activeOf(RES);

        // The player owns 32 cards of each faction: Resistenza 21 creatures and 11 others, Potenze 26 and 6.
        resDeck = own(RES, 21, 11);
        aiDeck = own(AI, 26, 6);
    }

    private UUID activeOf(String faction) {
        return jdbc.queryForObject("select snapshot_id from active_snapshots where faction = ?", UUID.class, faction);
    }

    private List<UUID> own(String faction, int creatures, int others) {
        List<Card> mine = cards.findAll().stream().filter(c -> c.getFaction().equals(faction))
                .sorted(Comparator.comparing(Card::getCardCode)).toList();
        List<Card> chosen = new ArrayList<>(mine.stream().filter(c -> c.getKind() == CardKind.CREATURE).limit(creatures).toList());
        chosen.addAll(mine.stream().filter(c -> c.getKind() != CardKind.CREATURE).limit(others).toList());
        return instances.saveAll(chosen.stream()
                .map(c -> new CardInstance(player, c.getId(), CardOrigin.GRANT, null, Instant.now())).toList())
                .stream().map(CardInstance::getId).toList();
    }

    /** Publishes a new Resistenza snapshot: one creature of the deck gets 1 more attack. */
    private UUID publishChange() {
        String code = cards.findAll().stream().filter(c -> c.getFaction().equals(RES) && c.getKind() == CardKind.CREATURE)
                .map(Card::getCardCode).sorted().findFirst().orElseThrow();
        var changed = BaseSet.card(code);
        changed.put("atk", changed.get("atk").asInt() + 1);
        UUID draft = drafts.open("api", null, admin).getId();
        drafts.putItem(draft, DraftItemType.CARD, changed);
        publisher.publish(draft, admin, null);
        return activeOf(RES);
    }

    private Deck deck(UUID id) {
        return decks.findById(id).orElseThrow();
    }

    private UUID createResDeck(String name, UUID snapshot) {
        return deckService.create(player, new DeckRequest(name, RES, snapshot, resDeck)).id();
    }

    // ---- login ----

    @Test
    void aLegalDeckMovesToTheActiveSnapshotWhenItsOwnerLogsIn() {
        UUID s2 = publishChange();
        UUID deck = createResDeck("Vecchio", s1);
        assertEquals(s1, deck(deck).getSnapshotId());
        assertTrue(deck(deck).isOutdated(), "a deck saved on a snapshot that is not the active one is flagged");

        auth.login(new LoginRequest("giocatore", "password123"));

        assertEquals(s2, deck(deck).getSnapshotId());
        assertFalse(deck(deck).isOutdated());
        assertEquals(32, deckCards.findByDeckId(deck).size());
        assertTrue(deckCards.findByDeckId(deck).stream().allMatch(c -> c.getSnapshotId().equals(s2)));
    }

    @Test
    void publishingAloneNeverTouchesADeck() {
        UUID deck = createResDeck("Mio", s1);
        assertFalse(deck(deck).isOutdated());
        UUID s2 = publishChange();
        assertEquals(s1, deck(deck).getSnapshotId());
        assertFalse(deck(deck).isOutdated());
        assertEquals(s2, activeOf(RES));
    }

    @Test
    void aFailureDuringMigrationNeverBlocksTheLogin() {
        publishChange();
        UUID deck = createResDeck("Vecchio", s1);
        doThrow(new IllegalStateException("boom")).when(loader).load(any(), any(), any(), any(), any());

        var response = auth.login(new LoginRequest("giocatore", "password123"));

        assertNotNull(response.token());
        assertEquals(s1, deck(deck).getSnapshotId(), "the deck stays on its old snapshot, playable");
    }

    // ---- the migration itself ----

    @Test
    void migratingAgainDoesNothing() {
        publishChange();
        createResDeck("Vecchio", s1);
        assertEquals(new DeckMigrator.Result(1, 0, 0), migrator.migrateOwner(player));
        assertEquals(new DeckMigrator.Result(0, 0, 1), migrator.migrateOwner(player));
    }

    @Test
    void aDeckThatIsIllegalOnTheActiveSnapshotIsFlaggedAndStaysPut() {
        UUID s2 = publishChange();
        // an illegal deck (31 cards) written straight to the tables, as an old rule set could have allowed
        Deck broken = decks.saveAndFlush(new Deck(player, "Rotto", RES, s1, Instant.now()));
        List<DeckCard> rows = new ArrayList<>();
        for (UUID inst : resDeck.subList(0, 31)) {
            CardInstance instance = instances.findById(inst).orElseThrow();
            Card card = cards.findById(instance.getCardId()).orElseThrow();
            rows.add(new DeckCard(new DeckCardId(broken.getId(), inst), player, card.getId(), RES, s1, card.getDeckSlot()));
        }
        deckCards.saveAll(rows);

        assertEquals(new DeckMigrator.Result(0, 1, 0), migrator.migrateOwner(player));

        assertEquals(s1, deck(broken.getId()).getSnapshotId());
        assertTrue(deck(broken.getId()).isOutdated());
        assertEquals(List.of(AI, RES), eligibility.status(player).missingDefense(), "the player has no defense deck yet");
        assertEquals(s2, activeOf(RES));
    }

    @Test
    void requireCanPlayMovesTheDecksFirstAsASafetyNet() {
        UUID s2 = publishChange();
        UUID resDefense = createResDeck("Difesa RES", s2);
        UUID aiDefense = deckService.create(player, new DeckRequest("Difesa AI", AI, activeOf(AI), aiDeck)).id();
        deckService.setDefense(player, resDefense);
        deckService.setDefense(player, aiDefense);
        eligibility.requireCanPlay(player);

        UUID s3 = publishChange(); // the session outlives a publish: no login in between
        assertEquals(s2, deck(resDefense).getSnapshotId());

        eligibility.requireCanPlay(player);

        assertEquals(s3, deck(resDefense).getSnapshotId());
        assertFalse(deck(resDefense).isOutdated());
        assertEquals(activeOf(AI), deck(aiDefense).getSnapshotId());
    }
}
