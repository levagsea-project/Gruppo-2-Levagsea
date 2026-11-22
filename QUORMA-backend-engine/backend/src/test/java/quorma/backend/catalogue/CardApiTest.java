package quorma.backend.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import quorma.backend.auth.JwtService;
import quorma.backend.catalogue.admin.CatalogueAdminService;
import quorma.backend.catalogue.publish.CatalogueBootstrap;
import quorma.backend.collection.CardInstance;
import quorma.backend.collection.CardInstanceRepository;
import quorma.backend.collection.CardOrigin;
import quorma.backend.deck.DeckRequest;
import quorma.backend.deck.DeckService;
import quorma.backend.support.ApiTest;
import quorma.backend.support.BaseSet;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;

/**
 * What the card-returning endpoints say about a card's text: the language follows {@code Accept-Language} with Italian
 * as the fallback (per card, so a half-translated catalogue still reads), and every card carries its sets.
 */
class CardApiTest extends ApiTest {

    private static final String RES = "RESISTENZA_UMANA";
    private static final String TRANSLATED = "BASE-RES-7-SPI-SPE";
    private static final String UNTRANSLATED = "BASE-RES-A-FRA-SPE";

    @Autowired
    CatalogueBootstrap bootstrap;
    @Autowired
    CatalogueAdminService admin;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwt;
    @Autowired
    CardRepository cards;
    @Autowired
    CardInstanceRepository instances;
    @Autowired
    DeckService deckService;

    UUID player;
    String token;
    private String italianName;

    @BeforeEach
    void setUp() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        User user = users.save(new User("giocatore", "g@quorma.local", "x", Role.PLAYER, 1000));
        player = user.getId();
        token = jwt.issue(user);
        italianName = BaseSet.cardText(TRANSLATED).get("name").asString();

        // English exists for one card and for the set; every other card is Italian only
        admin.putText("CARD", TRANSLATED, "en", BaseSet.MAPPER.readTree("{\"name\": \"Seventh Spirit\", \"flavour\": \"An English line.\"}"));
        admin.putText("SET", "BASE", "en", BaseSet.MAPPER.readTree("{\"name\": \"Base set\", \"description\": \"The first cards.\"}"));
    }

    private Map<String, String> lang(String header) {
        return Map.of("Accept-Language", header);
    }

    private JsonNode card(JsonNode collection, String code) {
        for (JsonNode c : collection.get("cards")) {
            if (c.get("cardCode").asString().equals(code)) {
                return c;
            }
        }
        throw new AssertionError("no card " + code + " in " + collection);
    }

    private void own(String... codes) {
        for (String code : codes) {
            Card card = cards.findByCardCode(code).orElseThrow();
            instances.save(new CardInstance(player, card.getId(), CardOrigin.GRANT, null, Instant.now()));
        }
    }

    // ---- the collection ----

    @Test
    void withoutAHeaderTheTextIsItalian() throws Exception {
        own(TRANSLATED, UNTRANSLATED);
        Reply r = call("GET", "/collection", token, null);
        assertEquals(200, r.status());
        JsonNode c = card(r.body(), TRANSLATED);
        assertEquals(italianName, c.get("name").asString());
        assertEquals("it", c.get("textLocale").asString());
        assertEquals(BaseSet.cardText(TRANSLATED).get("flavour").asString(), c.get("flavour").asString());
        assertEquals(1, c.get("sets").size());
        assertEquals("BASE", c.get("sets").get(0).get("code").asString());
        assertEquals("Set base", c.get("sets").get(0).get("name").asString());
    }

    @Test
    void theLanguageAskedForIsUsedPerCardWithItalianWhereACardHasNone() throws Exception {
        own(TRANSLATED, UNTRANSLATED);
        Reply r = call("GET", "/collection", token, null, lang("en-US,en;q=0.9,it;q=0.5"));
        assertEquals(200, r.status());

        JsonNode translated = card(r.body(), TRANSLATED);
        assertEquals("Seventh Spirit", translated.get("name").asString());
        assertEquals("An English line.", translated.get("flavour").asString());
        assertEquals("en", translated.get("textLocale").asString());
        assertEquals("Base set", translated.get("sets").get(0).get("name").asString(), "the set has an English name too");

        JsonNode plain = card(r.body(), UNTRANSLATED);
        assertEquals(BaseSet.cardText(UNTRANSLATED).get("name").asString(), plain.get("name").asString(), "no English: Italian");
        assertEquals("it", plain.get("textLocale").asString());
        assertEquals("Base set", plain.get("sets").get(0).get("name").asString(), "the set name follows the language of its own text");
    }

    @Test
    void aSecondChoiceIsUsedWhenTheFirstIsNotThere() throws Exception {
        own(TRANSLATED);
        Reply r = call("GET", "/collection", token, null, lang("fr, en;q=0.8"));
        assertEquals("Seventh Spirit", card(r.body(), TRANSLATED).get("name").asString());
        assertEquals("en", card(r.body(), TRANSLATED).get("textLocale").asString());
    }

    @Test
    void aHeaderThatMakesNoSenseMeansItalianAndNeverAnError() throws Exception {
        own(TRANSLATED);
        for (String bad : List.of(";;;", "*", "zzz", "en;q=oops", "x".repeat(600))) {
            Reply r = call("GET", "/collection", token, null, lang(bad));
            assertEquals(200, r.status(), bad);
            assertEquals("it", card(r.body(), TRANSLATED).get("textLocale").asString(), bad);
        }
    }

    @Test
    void aCardWithoutFlavourInTheLanguageHasNoFlavour() throws Exception {
        own(TRANSLATED);
        admin.putText("CARD", TRANSLATED, "en", BaseSet.MAPPER.readTree("{\"name\": \"Seventh Spirit\", \"flavour\": null}"));
        JsonNode c = card(call("GET", "/collection", token, null, lang("en")).body(), TRANSLATED);
        assertEquals("en", c.get("textLocale").asString());
        assertTrue(c.get("flavour").isNull(), "the English text has no flavour; it is not borrowed from the Italian one");
    }

    @Test
    void theParamsOfAnAbilityAreInTheAbilities() throws Exception {
        own(TRANSLATED);
        JsonNode c = card(call("GET", "/collection", token, null).body(), TRANSLATED);
        assertEquals("K_PERFORANTE", c.get("abilities").get(0).get("id").asString());
        assertEquals(30, c.get("abilities").get(0).get("params").get("x").asInt(), "the params that fill the text placeholders");
    }

    // ---- decks ----

    @Test
    void aDeckShowsItsCardsInTheLanguageAskedFor() throws Exception {
        List<UUID> deck = ownDeck();
        UUID snapshot = jdbc.queryForObject("select snapshot_id from active_snapshots where faction = ?", UUID.class, RES);
        UUID id = deckService.create(player, new DeckRequest("Mio", RES, snapshot, deck)).id();

        Reply it = call("GET", "/decks/" + id, token, null);
        Reply en = call("GET", "/decks/" + id, token, null, lang("en"));
        assertEquals(200, en.status());
        assertEquals(32, en.body().get("cards").size());
        JsonNode translated = null;
        for (JsonNode c : en.body().get("cards")) {
            if (c.get("cardCode").asString().equals(TRANSLATED)) {
                translated = c;
            }
        }
        assertEquals("Seventh Spirit", translated.get("name").asString());
        assertEquals("en", translated.get("textLocale").asString());
        assertEquals("BASE", translated.get("sets").get(0).get("code").asString());
        assertTrue(it.body().get("cards").toString().contains("\"textLocale\":\"it\""));
        assertTrue(!it.body().get("cards").toString().contains("\"textLocale\":\"en\""));
    }

    // ---- packs ----

    @Test
    void anOpenedPackShowsItsCardsInTheLanguageAskedFor() throws Exception {
        // the pack types of the base set came with it (standard, advanced and elite, from data/sets/BASE.json)
        String body = "{\"packType\": \"standard\", \"faction\": \"" + RES + "\"}";
        Reply r = call("POST", "/packs/open", token, body, Map.of("Idempotency-Key", UUID.randomUUID().toString(), "Accept-Language", "en"));
        assertEquals(201, r.status(), r.body().toString());
        assertEquals(6, r.body().get("cards").size());
        for (JsonNode c : r.body().get("cards")) {
            assertTrue(c.get("textLocale").asString().equals("en") || c.get("textLocale").asString().equals("it"));
            assertEquals("BASE", c.get("sets").get(0).get("code").asString());
            assertTrue(!c.get("name").isNull() && !c.get("name").asString().isBlank(), "every card has a name");
        }

        // the same purchase again returns the same pack, read now without a language: Italian
        String key = UUID.randomUUID().toString();
        Reply first = call("POST", "/packs/open", token, body, Map.of("Idempotency-Key", key, "Accept-Language", "en"));
        Reply again = call("POST", "/packs/open", token, body, Map.of("Idempotency-Key", key));
        assertEquals(first.body().get("packId"), again.body().get("packId"));
        assertEquals(first.body().get("cards").size(), again.body().get("cards").size());
        for (JsonNode c : again.body().get("cards")) {
            assertEquals("it", c.get("textLocale").asString());
        }
    }

    // ---- helpers ----

    /** 21 Resistenza creatures (the translated one among them) and 11 others, owned by the player. */
    private List<UUID> ownDeck() {
        List<Card> mine = cards.findAll().stream().filter(c -> c.getFaction().equals(RES))
                .sorted(Comparator.comparing(Card::getCardCode)).toList();
        List<Card> chosen = new ArrayList<>(mine.stream().filter(c -> c.getKind() == CardKind.CREATURE && !c.getCardCode().equals(TRANSLATED)).limit(20).toList());
        chosen.add(cards.findByCardCode(TRANSLATED).orElseThrow());
        chosen.addAll(mine.stream().filter(c -> c.getKind() != CardKind.CREATURE).limit(11).toList());
        return instances.saveAll(chosen.stream().map(c -> new CardInstance(player, c.getId(), CardOrigin.GRANT, null, Instant.now())).toList())
                .stream().map(CardInstance::getId).toList();
    }
}
