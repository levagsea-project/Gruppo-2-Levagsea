package quorma.backend.catalogue.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import quorma.backend.common.ErrorCodes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The batch format: what is accepted, and what is refused with {@code catalogue.import_invalid}. */
class ChangesetTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static JsonNode json(String text) {
        return MAPPER.readTree(text);
    }

    private static void refused(String text) {
        CatalogueException e = assertThrows(CatalogueException.class, () -> Changeset.parse(json(text)), text);
        assertEquals(ErrorCodes.CATALOGUE_IMPORT_INVALID, e.code());
        assertEquals(400, e.status().value());
    }

    @Test
    void aBatchHasCardsSetsAndTexts() {
        Changeset c = Changeset.parse(json("""
                {"_readme": "ignored",
                 "set_definitions": [{"code": "BASE", "status": "IN_VENDITA"}],
                 "cards": [{"id": "A"}, {"id": "B"}],
                 "texts": {"it": {"card.A.name": "Uno", "card.A.flavour": "f", "set.BASE.name": "Base", "set.BASE.description": "d"},
                           "en": {"card.A.name": "One"}}}"""));
        assertEquals(2, c.cards().size());
        assertEquals("BASE", c.sets().get(0).code());
        assertEquals("IN_VENDITA", c.sets().get(0).status());
        assertEquals(4, c.texts().get("it").size());
        assertEquals("One", c.texts().get("en").get("card.A.name"));
    }

    @Test
    void onlyCardsAreRequired() {
        Changeset c = Changeset.parse(json("{\"cards\": []}"));
        assertEquals(0, c.cards().size());
        assertEquals(0, c.sets().size());
        assertEquals(0, c.texts().size());
    }

    @Test
    void aBrokenBatchIsRefused() {
        refused("[]");
        refused("{}");
        refused("{\"cards\": {}}");
        refused("{\"cards\": [\"A\"]}");
        refused("{\"cards\": [{}]}");
        refused("{\"cards\": [{\"id\": \"\"}]}");
        refused("{\"cards\": [], \"extra\": 1}");
        refused("{\"cards\": [], \"set_definitions\": [{\"status\": \"BOZZA\"}]}");
        refused("{\"cards\": [], \"set_definitions\": [{\"code\": \"X\", \"colour\": 1}]}");
        refused("{\"cards\": [], \"texts\": []}");
        refused("{\"cards\": [], \"texts\": {\"ITA\": {}}}");
        refused("{\"cards\": [], \"texts\": {\"it\": {\"card.A\": \"x\"}}}");
        refused("{\"cards\": [], \"texts\": {\"it\": {\"card.A.name\": \"\"}}}");
        refused("{\"cards\": [], \"texts\": {\"it\": {\"card.A.name\": 5}}}");
    }

    @Test
    void textKeysAreCheckedAgainstTheirKind() {
        assertEquals(new Changeset.TextKey("CARD", "A", "flavour"), Changeset.TextKey.of("card.A.flavour"));
        assertEquals(new Changeset.TextKey("SET", "BASE", "description"), Changeset.TextKey.of("set.BASE.description"));
        assertThrows(CatalogueException.class, () -> Changeset.TextKey.of("card.A.description"));
        assertThrows(CatalogueException.class, () -> Changeset.TextKey.of("set.BASE.flavour"));
    }

    @Test
    void tooManyCardsAreRefused() {
        StringBuilder sb = new StringBuilder("{\"cards\": [");
        for (int i = 0; i <= Changeset.MAX_CARDS; i++) {
            sb.append(i == 0 ? "" : ",").append("{\"id\": \"C").append(i).append("\"}");
        }
        refused(sb.append("]}").toString());
    }
}
