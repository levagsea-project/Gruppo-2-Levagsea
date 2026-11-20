package quorma.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import quorma.backend.catalogue.publish.CatalogueBootstrap;
import quorma.backend.support.ApiTest;
import quorma.backend.support.BaseSet;
import quorma.gameengine.GameEngine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The whole path a new player takes, over HTTP with the real security and a real database: register, find the starter
 * decks, buy a pack, build a deck, and play a match with the decks the API returned (on the real engine, in-process).
 * Every step reads what the previous one wrote, so a break anywhere shows here.
 */
class FullFlowTest extends ApiTest {

    private static final GameEngine ENGINE = new GameEngine();

    @Autowired
    CatalogueBootstrap bootstrap;

    private JsonNode ok(Reply r) {
        assertTrue(r.status() >= 200 && r.status() < 300, "unexpected " + r.status() + ": " + r.body());
        return r.body();
    }

    /** A deck as {@code GET /decks/{id}} returns it, as the engine reads a deck. */
    private ObjectNode engineSide(JsonNode deck) {
        ObjectNode side = BaseSet.MAPPER.createObjectNode();
        String faction = deck.get("faction").asString();
        side.put("faction", faction);
        ArrayNode cards = side.putArray("deck");
        for (JsonNode c : deck.get("cards")) {
            ObjectNode card = BaseSet.MAPPER.createObjectNode();
            card.put("id", c.get("cardCode").asString());
            card.put("kind", c.get("kind").asString());
            if (c.get("kind").asString().equals("CREATURE")) {
                card.put("rank", c.get("rank").asInt());
                card.put("suit", c.get("suit").asString());
                card.put("subfaction", c.get("subfaction").asString());
                card.put("atk", c.get("atk").asInt());
                card.put("def", c.get("def").asInt());
            } else {
                card.put("faction", faction);
            }
            card.set("abilities", c.get("abilities"));
            cards.add(card);
        }
        return side;
    }

    @Test
    void aNewPlayerRegistersBuysAPackBuildsADeckAndPlaysAMatch() throws Exception {
        bootstrap.bootstrap(BaseSet.SETS_DIR);

        // 1. register: the response is a token, and the player already has the starter decks
        JsonNode registered = ok(call("POST", "/auth/register", null,
                "{\"username\": \"nuovo\", \"email\": \"nuovo@quorma.it\", \"password\": \"password123\"}"));
        String token = registered.get("token").asString();
        assertEquals("PLAYER", registered.get("role").asString());

        JsonNode decks = ok(call("GET", "/decks", token, null));
        assertEquals(2, decks.size());
        assertTrue(ok(call("GET", "/decks/play-status", token, null)).get("canPlay").asBoolean(), "a new player can play at once");
        assertEquals(64, ok(call("GET", "/collection", token, null)).get("cards").size());

        // 2. the shop: three packs on sale; buy one (the credits are a purchase of the economy, set here directly)
        JsonNode shop = ok(call("GET", "/packs", token, null));
        assertEquals(3, shop.size());
        jdbc.update("update users set credits = 1000 where username = 'nuovo'");
        Reply bought = call("POST", "/packs/open", token, "{\"packType\": \"standard\", \"faction\": \"RESISTENZA_UMANA\"}",
                Map.of("Idempotency-Key", UUID.randomUUID().toString()));
        assertEquals(201, bought.status(), bought.body().toString());
        assertEquals(800, bought.body().get("creditsAfter").asInt());
        assertEquals(6, bought.body().get("cards").size());
        JsonNode collection = ok(call("GET", "/collection", token, null));
        assertEquals(70, collection.get("cards").size(), "the pack is in the collection beside the starter cards");

        // 3. build a deck of their own: the Resistenza starter cards again, under a new name (instances may be reused)
        String resStarter = null;
        String aiStarter = null;
        for (JsonNode d : decks) {
            if (d.get("faction").asString().equals("RESISTENZA_UMANA")) {
                resStarter = d.get("id").asString();
            } else {
                aiStarter = d.get("id").asString();
            }
        }
        JsonNode starter = ok(call("GET", "/decks/" + resStarter, token, null));
        List<String> instanceIds = new ArrayList<>();
        starter.get("cards").forEach(c -> instanceIds.add("\"" + c.get("cardInstanceId").asString() + "\""));
        JsonNode built = ok(call("POST", "/decks", token, "{\"name\": \"Il mio mazzo\", \"faction\": \"RESISTENZA_UMANA\", "
                + "\"snapshotId\": \"" + starter.get("snapshotId").asString() + "\", \"cardInstanceIds\": [" + String.join(",", instanceIds) + "]}"));
        assertEquals(32, built.get("cards").size());
        assertEquals(false, built.get("outdated").asBoolean());
        assertEquals(3, ok(call("GET", "/decks", token, null)).size());

        // 4. play: the deck the player built against the Potenze starter deck, on the real engine
        JsonNode mine = ok(call("GET", "/decks/" + built.get("id").asString(), token, null));
        JsonNode theirs = ok(call("GET", "/decks/" + aiStarter, token, null));
        ObjectNode input = BaseSet.MAPPER.createObjectNode();
        input.put("schema_version", 1);
        input.put("seed", 2026);
        input.put("ruleset_version", ENGINE.rules().rulesetVersion());
        ObjectNode players = input.putObject("players");
        players.set("A", engineSide(mine));
        players.set("B", engineSide(theirs));
        String request = BaseSet.MAPPER.writeValueAsString(input);

        String log = ENGINE.matchLog(request);
        JsonNode result = BaseSet.MAPPER.readTree(log).get("result");
        assertTrue(List.of("A_WIN", "B_WIN", "DRAW").contains(result.get("outcome").asString()), result.toString());
        assertEquals(log, ENGINE.matchLog(request), "same decks, same seed, same match");
    }
}
