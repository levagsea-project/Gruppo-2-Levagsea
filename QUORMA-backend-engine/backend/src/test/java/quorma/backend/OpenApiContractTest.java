package quorma.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import quorma.backend.support.ApiTest;
import tools.jackson.databind.JsonNode;

/**
 * The OpenAPI document is the contract with the Web team, and a change to it is a breaking change to tell them about.
 * This test lists every method and path the running backend publishes and compares it with the list below, so adding,
 * removing or renaming an endpoint fails here until the list is updated.
 * It also checks the properties of the contract that the card API promises.
 */
class OpenApiContractTest extends ApiTest {

    private static final List<String> EXPECTED = List.of(
            "DELETE /admin/catalogue/drafts/{id}",
            "DELETE /admin/catalogue/drafts/{id}/items/{type}/{key}",
            "DELETE /admin/catalogue/pack-types/{code}",
            "DELETE /admin/catalogue/sets/{code}/cards/{cardCode}",
            "DELETE /decks/{id}",
            "GET /admin/catalogue/draft",
            "GET /admin/catalogue/drafts/{id}",
            "GET /admin/catalogue/drafts/{id}/preview",
            "GET /admin/catalogue/export",
            "GET /admin/catalogue/pack-types",
            "GET /admin/catalogue/sets",
            "GET /collection",
            "GET /decks",
            "GET /decks/play-status",
            "GET /decks/{id}",
            "GET /packs",
            "POST /admin/catalogue/drafts",
            "POST /admin/catalogue/drafts/{id}/publish",
            "POST /admin/catalogue/drafts/{id}/rebase",
            "POST /admin/catalogue/drift",
            "POST /admin/catalogue/import",
            "POST /auth/login",
            "POST /auth/register",
            "POST /decks",
            "POST /decks/{id}/defense",
            "POST /packs/open",
            "POST /v1/simulate",
            "PUT /admin/catalogue/drafts/{id}/cards/{code}",
            "PUT /admin/catalogue/drafts/{id}/pack-types/{code}",
            "PUT /admin/catalogue/drafts/{id}/sets/{code}",
            "PUT /admin/catalogue/drafts/{id}/texts/{kind}/{code}/{locale}",
            "PUT /admin/catalogue/pack-types/{code}",
            "PUT /admin/catalogue/sets/{code}",
            "PUT /admin/catalogue/sets/{code}/cards/{cardCode}",
            "PUT /admin/catalogue/texts/{kind}/{code}/{locale}",
            "PUT /decks/{id}");

    private JsonNode spec() throws Exception {
        Reply r = call("GET", "/api-docs", null, null);
        assertEquals(200, r.status());
        return r.body();
    }

    private static List<String> operations(JsonNode spec) {
        TreeSet<String> out = new TreeSet<>();
        for (String path : spec.get("paths").propertyNames()) {
            for (String method : spec.get("paths").get(path).propertyNames()) {
                out.add(method.toUpperCase() + " " + path);
            }
        }
        return new ArrayList<>(out);
    }

    @Test
    void theEndpointsAreTheOnesThatAreListed() throws Exception {
        assertEquals(EXPECTED, operations(spec()));
    }

    /** The operations that return card text take the language of the caller. */
    @Test
    void everyOperationThatReturnsCardTextTakesAcceptLanguage() throws Exception {
        JsonNode paths = spec().get("paths");
        for (String op : List.of("get /collection", "get /packs", "post /packs/open", "get /decks/{id}", "post /decks",
                "put /decks/{id}", "post /decks/{id}/defense")) {
            String[] parts = op.split(" ");
            boolean found = false;
            for (JsonNode p : paths.get(parts[1]).get(parts[0]).get("parameters")) {
                found |= p.get("in").asString().equals("header") && p.get("name").asString().equals("Accept-Language");
            }
            assertTrue(found, op + " takes the Accept-Language header");
        }
    }

    /** What a card carries, as the frontend reads it. */
    @Test
    void aCardCarriesItsTextAndItsSets() throws Exception {
        JsonNode schemas = spec().get("components").get("schemas");
        for (String card : List.of("CollectionCardResponse", "DeckCardResponse")) {
            for (String field : List.of("name", "flavour", "textLocale", "sets", "abilities", "cardCode")) {
                assertTrue(schemas.get(card).get("properties").has(field), card + " has " + field);
            }
        }
        assertTrue(schemas.get("PackTypeResponse").get("properties").has("set"), "a pack type names its set");
        assertTrue(schemas.get("SetRef").get("properties").has("code") && schemas.get("SetRef").get("properties").has("name"));
    }
}
