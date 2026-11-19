package quorma.backend.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import quorma.backend.auth.JwtService;
import quorma.backend.catalogue.admin.CatalogueAdminService;
import quorma.backend.catalogue.publish.CatalogueBootstrap;
import quorma.backend.support.ApiTest;
import quorma.backend.support.BaseSet;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;

/**
 * The shop reads the pack types from the database: a type is listed and sold while its set is on sale, and a pack is
 * drawn from the cards of that set only.
 */
class PackShopTest extends ApiTest {

    private static final String RES = "RESISTENZA_UMANA";

    @Autowired
    CatalogueBootstrap bootstrap;
    @Autowired
    CatalogueAdminService admin;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwt;

    UUID player;
    String token;

    @BeforeEach
    void setUp() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        User user = users.save(new User("giocatore", "g@quorma.local", "x", Role.PLAYER, 5000));
        player = user.getId();
        token = jwt.issue(user);
    }

    private Reply open(String type, String key) throws Exception {
        return call("POST", "/packs/open", token, "{\"packType\": \"" + type + "\", \"faction\": \"" + RES + "\"}",
                Map.of("Idempotency-Key", key));
    }

    private Reply open(String type) throws Exception {
        return open(type, UUID.randomUUID().toString());
    }

    private void setStatus(String status) {
        admin.updateSet("BASE", BaseSet.MAPPER.readTree("{\"status\": \"" + status + "\"}"));
    }

    // ---- what is for sale ----

    @Test
    void theTypesOfTheSetsOnSaleAreListedCheapestFirstWithTheirSet() throws Exception {
        Reply r = call("GET", "/packs", token, null);
        assertEquals(200, r.status());
        JsonNode packs = r.body();
        assertEquals(3, packs.size());
        assertEquals(List.of("standard", "advanced", "elite"),
                List.of(packs.get(0).get("code").asString(), packs.get(1).get("code").asString(), packs.get(2).get("code").asString()));
        assertEquals(200, packs.get(0).get("price").asInt());
        assertEquals(6, packs.get(0).get("size").asInt());
        assertEquals(2, packs.get(0).get("slots").get("TIER_1").asInt());
        assertEquals(2, packs.get(0).get("supportSlots").asInt());
        assertTrue(packs.get(1).get("featured").asBoolean());
        assertEquals(1, packs.get(2).get("slots").get("TIER_4").asInt());
        assertEquals("BASE", packs.get(0).get("set").get("code").asString());
        assertEquals("Set base", packs.get(0).get("set").get("name").asString());

        admin.putText("SET", "BASE", "en", BaseSet.MAPPER.readTree("{\"name\": \"Base set\"}"));
        Reply en = call("GET", "/packs", token, null, Map.of("Accept-Language", "en"));
        assertEquals("Base set", en.body().get(0).get("set").get("name").asString());
    }

    @Test
    void aSetThatIsNotOnSaleSellsNothing() throws Exception {
        for (String status : List.of("BOZZA", "CHIUSA")) {
            setStatus(status);
            assertEquals(0, call("GET", "/packs", token, null).body().size(), status);
            Reply r = open("standard");
            assertEquals(409, r.status(), status);
            assertEquals("pack.not_on_sale", r.code());
        }
        assertEquals(5000, jdbc.queryForObject("select credits from users where username = 'giocatore'", Integer.class), "nothing was charged");
        setStatus("IN_VENDITA");
        assertEquals(201, open("standard").status());
    }

    @Test
    void aTypeThatDoesNotExistIsRefused() throws Exception {
        Reply r = open("nope");
        assertEquals(400, r.status());
        assertEquals("pack.unknown_type", r.code());
    }

    @Test
    void aRetryOfAPurchaseMadeBeforeTheSetClosedStillGetsItsPack() throws Exception {
        String key = UUID.randomUUID().toString();
        Reply first = open("standard", key);
        assertEquals(201, first.status());
        setStatus("CHIUSA");

        Reply again = open("standard", key);
        assertEquals(201, again.status());
        assertEquals(first.body().get("packId"), again.body().get("packId"));
        assertEquals(1, count("pack_openings"));
        assertEquals("BASE", jdbc.queryForObject("select s.code from pack_openings o join card_sets s on s.id = o.set_id", String.class));
    }

    // ---- what is in a pack ----

    @Test
    void aPackHoldsExactlyWhatItsTypeSaysAndOnlyCardsOfItsSet() throws Exception {
        for (String type : List.of("standard", "advanced", "elite")) {
            Reply r = open(type);
            assertEquals(201, r.status(), r.body().toString());
            int expected = switch (type) {
                case "standard" -> 6;
                case "advanced" -> 7;
                default -> 8;
            };
            assertEquals(expected, r.body().get("cards").size(), type);
            for (JsonNode c : r.body().get("cards")) {
                assertEquals("BASE", c.get("sets").get(0).get("code").asString());
                assertEquals(RES, c.get("faction").asString());
            }
        }
        assertEquals(200 + 450 + 1000, 5000 - jdbc.queryForObject("select credits from users where username = 'giocatore'", Integer.class));
    }

    @Test
    void thePoolIsTheCardsOfTheSetNotTheWholeFaction() throws Exception {
        // every Resistenza spell and equipment is still in the snapshot, but no longer in the set: no pack can hold one
        List<String> support = jdbc.queryForList("select card_code from cards where faction = 'RESISTENZA_UMANA' and kind <> 'CREATURE'", String.class);
        assertEquals(23, support.size());
        support.forEach(code -> admin.removeCardFromSet("BASE", code));

        Reply r = open("standard");
        assertEquals(404, r.status(), "a pack of this type needs spells or equipment");
        assertEquals("pack.no_pool", r.code());
        assertEquals(5000, jdbc.queryForObject("select credits from users where username = 'giocatore'", Integer.class), "nothing was charged");
        assertEquals(0, count("pack_openings"));
        assertEquals(23, count("cards where faction = 'RESISTENZA_UMANA' and kind <> 'CREATURE'"), "the cards are still in the game");

        support.forEach(code -> admin.addCardToSet("BASE", code));
        assertEquals(201, open("standard").status());
    }

    @Test
    void aTypeWhoseTierHasNoCardInTheSetCannotBeOpenedButOthersCan() throws Exception {
        List<String> aces = jdbc.queryForList("select card_code from cards where faction = 'RESISTENZA_UMANA' and rank = 1", String.class);
        assertEquals(4, aces.size());
        aces.forEach(code -> admin.removeCardFromSet("BASE", code));

        assertEquals("pack.no_pool", open("elite").code(), "the elite pack holds an Ace");
        assertEquals(201, open("standard").status(), "the standard pack does not");
    }
}
