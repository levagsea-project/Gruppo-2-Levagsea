package quorma.backend.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import quorma.backend.auth.AuthService;
import quorma.backend.auth.LoginRequest;
import quorma.backend.auth.RegisterRequest;
import quorma.backend.catalogue.publish.CatalogueBootstrap;
import quorma.backend.support.BaseSet;
import quorma.backend.support.IntegrationTest;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/**
 * Every player starts with two pre-made decks, one per faction, owned as STARTER cards and set as the defense decks, so
 * a new player can play at once. A catalogue that is not ready never stops anyone from registering.
 */
class StarterDeckTest extends IntegrationTest {

    @Autowired
    CatalogueBootstrap bootstrap;
    @Autowired
    AuthService auth;
    @Autowired
    UserRepository users;
    @Autowired
    DeckService deckService;
    @Autowired
    DeckEligibility eligibility;
    @Autowired
    DeckRepository decks;
    @Autowired
    PasswordEncoder encoder;

    private UUID register(String name) {
        auth.register(new RegisterRequest(name, name + "@quorma.local", "password123"));
        return users.findByUsernameIgnoreCase(name).orElseThrow().getId();
    }

    private int kindCount(UUID deckId, String kind) {
        return jdbc.queryForObject("select count(*) from deck_cards dc join cards c on c.id = dc.card_id "
                + "where dc.deck_id = ? and c.kind = ?", Integer.class, deckId, kind);
    }

    private String ranks(UUID deckId) {
        return String.join(",", jdbc.queryForList("select distinct c.rank::text from deck_cards dc join cards c on c.id = dc.card_id "
                + "where dc.deck_id = ? and c.kind = 'CREATURE' order by 1", String.class, deckId));
    }

    @Test
    void aNewPlayerGetsADefenseDeckForEachFactionFromTheBaseSet() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        UUID player = register("giocatore");

        List<Deck> mine = decks.findByOwnerIdOrderByUpdatedAtDesc(player);
        assertEquals(2, mine.size());
        assertTrue(mine.stream().allMatch(d -> d.isDefense() && !d.isOutdated()));
        assertEquals(2, mine.stream().map(Deck::getFaction).distinct().count());
        assertTrue(eligibility.status(player).canPlay(), "a new player can play at once");
        assertEquals(64, jdbc.queryForObject("select count(*) from card_instances where owner_id = ? and origin_type = 'STARTER'", Integer.class, player));
        assertEquals(64, jdbc.queryForObject("select count(*) from card_ownership_history h join card_instances i on i.id = h.card_instance_id "
                + "where i.owner_id = ? and h.reason = 'STARTER' and h.from_user_id is null and h.to_user_id = ?", Integer.class, player, player));

        for (Deck d : mine) {
            assertEquals(32, jdbc.queryForObject("select count(*) from deck_cards where deck_id = ?", Integer.class, d.getId()));
            UUID active = jdbc.queryForObject("select snapshot_id from active_snapshots where faction = ?", UUID.class, d.getFaction());
            assertEquals(active, d.getSnapshotId());
            boolean res = d.getFaction().equals("RESISTENZA_UMANA");
            assertEquals(res ? 20 : 24, kindCount(d.getId(), "CREATURE"), "the fewest creatures the faction's rules allow");
            assertEquals(res ? 6 : 4, kindCount(d.getId(), "SPELL"), "spells and equipment alternate");
            assertEquals(res ? 6 : 4, kindCount(d.getId(), "EQUIPMENT"));
            assertEquals(res ? "2,3,4,5,6" : "2,3,4,5,6,7", ranks(d.getId()), "the lowest creatures first, no Ace");
        }
        assertEquals("Mazzo iniziale (Resistenza Umana)", mine.stream().filter(d -> d.getFaction().equals("RESISTENZA_UMANA"))
                .findFirst().orElseThrow().getName());
    }

    @Test
    void everyPlayerGetsTheSameStarterDecks() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        UUID a = register("primo");
        UUID b = register("secondo");
        String sql = "select string_agg(c.card_code, ',' order by c.card_code) from deck_cards dc join cards c on c.id = dc.card_id "
                + "join decks d on d.id = dc.deck_id where d.owner_id = ? and d.faction = ?";
        for (String faction : List.of("RESISTENZA_UMANA", "POTENZE_ARTIFICIALI")) {
            assertEquals(jdbc.queryForObject(sql, String.class, a, faction), jdbc.queryForObject(sql, String.class, b, faction));
        }
    }

    @Test
    void registeringBeforeTheCatalogueExistsStillWorksAndTheDecksComeAtLogin() {
        UUID player = register("giocatore");
        assertNotNull(player, "the registration did not fail");
        assertEquals(0, decks.findByOwnerIdOrderByUpdatedAtDesc(player).size());
        assertFalse(eligibility.status(player).canPlay());

        bootstrap.bootstrap(BaseSet.SETS_DIR);
        auth.login(new LoginRequest("giocatore", "password123"));

        assertEquals(2, decks.findByOwnerIdOrderByUpdatedAtDesc(player).size());
        assertTrue(eligibility.status(player).canPlay());

        auth.login(new LoginRequest("giocatore", "password123"));
        assertEquals(2, decks.findByOwnerIdOrderByUpdatedAtDesc(player).size(), "a second login adds nothing");
        assertEquals(64, jdbc.queryForObject("select count(*) from card_instances where owner_id = ?", Integer.class, player));
    }

    @Test
    void aFactionThePlayerAlreadyHasADeckOfIsLeftAlone() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        UUID player = register("giocatore");
        UUID res = decks.findByOwnerIdOrderByUpdatedAtDesc(player).stream().filter(d -> d.getFaction().equals("RESISTENZA_UMANA"))
                .findFirst().orElseThrow().getId();
        jdbc.update("update decks set is_defense = false where id = ?", res); // defense is only a flag: the deck stays
        auth.login(new LoginRequest("giocatore", "password123"));
        assertEquals(2, decks.findByOwnerIdOrderByUpdatedAtDesc(player).size());
    }

    @Test
    void anAdminDoesNotGetStarterDecks() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        users.save(new User("amministratore", "a@quorma.local", encoder.encode("password123"), Role.ADMIN, 0, "amministratore", "avatar-01"));
        auth.login(new LoginRequest("amministratore", "password123"));
        assertEquals(0, count("decks"));
        assertEquals(0, count("card_instances"));
    }

    @Test
    void theStarterDecksAreLegalDecksLikeAnyOther() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        UUID player = register("giocatore");
        for (Deck d : decks.findByOwnerIdOrderByUpdatedAtDesc(player)) {
            // saved again as it is, the deck passes the same validation (the starter deck went through it when it was made)
            List<UUID> instanceIds = jdbc.queryForList("select card_instance_id from deck_cards where deck_id = ?", UUID.class, d.getId());
            deckService.update(player, d.getId(), new DeckUpdateRequest("Rinominato " + d.getFaction(), d.getSnapshotId(), instanceIds));
        }
        assertEquals(2, decks.findByOwnerIdOrderByUpdatedAtDesc(player).stream().filter(d -> d.getName().startsWith("Rinominato ")).count());
    }
}
