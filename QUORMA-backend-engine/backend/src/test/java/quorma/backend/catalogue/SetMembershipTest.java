package quorma.backend.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import quorma.backend.auth.AuthService;
import quorma.backend.auth.JwtService;
import quorma.backend.auth.RegisterRequest;
import quorma.backend.catalogue.admin.CatalogueAdminService;
import quorma.backend.catalogue.publish.CatalogueBootstrap;
import quorma.backend.deck.Deck;
import quorma.backend.deck.DeckRepository;
import quorma.backend.deck.DeckService;
import quorma.backend.deck.DeckUpdateRequest;
import quorma.backend.support.ApiTest;
import quorma.backend.support.BaseSet;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;

/**
 * Taking a card out of a set is a commercial decision about packs, never about the game: the card stays in the snapshot,
 * in the decks that hold it and in the collections that own it.
 */
class SetMembershipTest extends ApiTest {

    @Autowired
    CatalogueBootstrap bootstrap;
    @Autowired
    CatalogueAdminService admin;
    @Autowired
    AuthService auth;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwt;
    @Autowired
    DeckRepository decks;
    @Autowired
    DeckService deckService;

    private static JsonNode cardNamed(JsonNode cards, String code) {
        for (JsonNode c : cards) {
            if (c.get("cardCode").asString().equals(code)) {
                return c;
            }
        }
        throw new AssertionError("no card " + code);
    }

    @Test
    void aCardTakenOutOfASetStaysInTheSnapshotInDecksAndInCollections() throws Exception {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        auth.register(new RegisterRequest("giocatore", "g@quorma.local", "password123"));
        var user = users.findByUsernameIgnoreCase("giocatore").orElseThrow();
        String token = jwt.issue(user);
        Deck deck = decks.findByOwnerIdOrderByUpdatedAtDesc(user.getId()).stream()
                .filter(d -> d.getFaction().equals("RESISTENZA_UMANA")).findFirst().orElseThrow();
        String code = jdbc.queryForObject("select c.card_code from deck_cards dc join cards c on c.id = dc.card_id "
                + "where dc.deck_id = ? and c.kind = 'SPELL' order by c.card_code limit 1", String.class, deck.getId());
        int definitions = count("card_definitions");

        assertEquals("BASE", cardNamed(call("GET", "/decks/" + deck.getId(), token, null).body().get("cards"), code).get("sets").get(0).get("code").asString());

        admin.removeCardFromSet("BASE", code);

        // the deck still holds it, with 32 cards, and the card now says it is in no set
        JsonNode inDeck = call("GET", "/decks/" + deck.getId(), token, null).body();
        assertEquals(32, inDeck.get("cards").size());
        assertEquals(0, cardNamed(inDeck.get("cards"), code).get("sets").size());
        assertEquals(false, inDeck.get("outdated").asBoolean());
        // the collection still owns it
        JsonNode owned = call("GET", "/collection", token, null).body().get("cards");
        assertEquals(0, cardNamed(owned, code).get("sets").size());
        assertEquals(64, owned.size());
        // the snapshot still defines it, and the deck can still be saved as it is
        assertEquals(definitions, count("card_definitions"));
        List<UUID> instanceIds = jdbc.queryForList("select card_instance_id from deck_cards where deck_id = ?", UUID.class, deck.getId());
        deckService.update(user.getId(), deck.getId(), new DeckUpdateRequest(deck.getName(), deck.getSnapshotId(), instanceIds));
        assertTrue(call("GET", "/decks/play-status", token, null).body().get("canPlay").asBoolean());

        // and it can go back into the set
        admin.addCardToSet("BASE", code);
        assertEquals("BASE", cardNamed(call("GET", "/collection", token, null).body().get("cards"), code).get("sets").get(0).get("code").asString());
    }
}
