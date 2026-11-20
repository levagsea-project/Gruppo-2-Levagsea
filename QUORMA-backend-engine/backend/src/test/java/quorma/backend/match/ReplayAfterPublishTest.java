package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import quorma.backend.catalogue.publish.CatalogueBootstrap;
import quorma.backend.catalogue.publish.CatalogueExporter;
import quorma.backend.catalogue.publish.Changeset;
import quorma.backend.catalogue.publish.ChangesetImporter;
import quorma.backend.catalogue.publish.ChangesetImporter.Mode;
import quorma.backend.support.BaseSet;
import quorma.backend.support.IntegrationTest;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;
import quorma.gameengine.GameEngine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A match is stored as a recipe: the decks as they were, the seed and the versions. A new snapshot of the catalogue
 * changes what the next match is built from, never a match already played: the same recipe on the same engine gives the
 * same result, byte for byte, whatever has been published since.
 */
class ReplayAfterPublishTest extends IntegrationTest {

    private static final GameEngine ENGINE = new GameEngine();

    @Autowired
    CatalogueBootstrap bootstrap;
    @Autowired
    CatalogueExporter exporter;
    @Autowired
    ChangesetImporter importer;
    @Autowired
    UserRepository users;

    /** The match recipe: the first 32 cards of each faction in the active snapshot, a seed, and the versions that play it. */
    private String recipe() {
        ObjectNode input = BaseSet.MAPPER.createObjectNode();
        input.put("schema_version", 1);
        input.put("seed", 7);
        input.put("ruleset_version", ENGINE.rules().rulesetVersion());
        input.put("ruleset_hash", ENGINE.rulesetHash());
        input.put("engine_version", quorma.gameengine.EngineVersion.VERSION);
        ObjectNode players = input.putObject("players");
        deck(players.putObject("A"), "RESISTENZA_UMANA", "BASE-RES-");
        deck(players.putObject("B"), "POTENZE_ARTIFICIALI", "BASE-AI-");
        return BaseSet.MAPPER.writeValueAsString(input);
    }

    private void deck(ObjectNode player, String faction, String prefix) {
        player.put("faction", faction);
        ArrayNode deck = player.putArray("deck");
        List<JsonNode> mine = new ArrayList<>();
        for (JsonNode card : exporter.export().get("cards")) {
            if (card.get("id").asString().startsWith(prefix)) {
                mine.add(card);
            }
        }
        for (JsonNode card : mine.subList(0, 32)) {
            ObjectNode engineCard = ((ObjectNode) card).deepCopy();
            engineCard.remove("sets");
            deck.add(engineCard);
        }
    }

    private String resultHash(String log) {
        return BaseSet.MAPPER.readTree(log).get("result_hash").asString();
    }

    @Test
    void theSameRecipeGivesTheSameMatchAfterANewSnapshot() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
        String before = recipe();
        String log = ENGINE.matchLog(before);
        assertTrue(resultHash(log).matches("^[0-9a-f]{64}$"));
        assertEquals(log, ENGINE.matchLog(before), "determinism: same recipe, same bytes");

        // a rebalance: every creature of the first deck gains attack, and a new snapshot is published for the faction
        UUID admin = users.save(new User("admin1", "admin1@quorma.local", "x", Role.ADMIN, 0)).getId();
        ObjectNode tweaked = BaseSet.batch();
        for (JsonNode card : tweaked.get("cards")) {
            if (card.get("id").asString().startsWith("BASE-RES-") && card.get("kind").asString().equals("CREATURE")) {
                ((ObjectNode) card).put("atk", card.get("atk").asInt() + 1000);
            }
        }
        importer.run(Changeset.parse(tweaked), Mode.UPSERT, false, "batch", admin, "rebalance");
        assertEquals(3, count("pool_snapshots"), "a new snapshot of the Resistenza pool exists");

        // the match already played is unchanged: its recipe holds the cards as they were
        assertEquals(log, ENGINE.matchLog(before));
        assertEquals(resultHash(log), resultHash(ENGINE.matchLog(before)));

        // a match built from the catalogue as it is now is another recipe, with the new numbers in it
        String after = recipe();
        assertNotEquals(before, after);
        assertNotEquals(resultHash(log), resultHash(ENGINE.matchLog(after)), "other numbers, another match");
    }
}
