package quorma.backend.catalogue.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import quorma.backend.auth.JwtService;
import quorma.backend.support.BaseSet;
import quorma.backend.support.IntegrationTest;
import quorma.backend.support.IntegrationTestConfig;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;

/**
 * The admin API over HTTP, with the real security: who may call it, and each endpoint. The pipeline itself is tested in
 * CataloguePublishTest and CatalogueImportTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "catalogue.max-import-bytes=600KB")
@Import(IntegrationTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class AdminCatalogueApiTest extends IntegrationTest {

    @LocalServerPort
    int port;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwt;

    private final HttpClient http = HttpClient.newHttpClient();
    private String admin;
    private String player;

    private record Reply(int status, JsonNode body) {
        String code() {
            return body.path("code").asString(null);
        }
    }

    @BeforeEach
    void tokens() {
        admin = jwt.issue(users.save(new User("admin1", "admin1@quorma.local", "x", Role.ADMIN, 0)));
        player = jwt.issue(users.save(new User("giocatore", "g@quorma.local", "x", Role.PLAYER, 0)));
    }

    private Reply call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json");
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Reply(r.statusCode(), r.body().isBlank() ? BaseSet.MAPPER.createObjectNode() : BaseSet.MAPPER.readTree(r.body()));
    }

    private Reply asAdmin(String method, String path, String body) throws Exception {
        return call(method, path, admin, body);
    }

    private String seedViaImport() throws Exception {
        Reply r = asAdmin("POST", "/admin/catalogue/import?mode=add-only&dryRun=false", BaseSet.MAPPER.writeValueAsString(BaseSet.batch()));
        assertEquals(200, r.status(), r.body().toString());
        return r.body().toString();
    }

    // ---- who may call it ----

    @Test
    void onlyAnAdminMayCallIt() throws Exception {
        assertEquals(401, call("GET", "/admin/catalogue/draft", null, null).status());
        assertEquals(403, call("GET", "/admin/catalogue/draft", player, null).status());
        assertEquals(403, call("GET", "/admin/catalogue/export", player, null).status());
        assertEquals(404, asAdmin("GET", "/admin/catalogue/draft", null).status());
    }

    // ---- the draft ----

    @Test
    void aDraftIsOpenedFilledPreviewedAndPublished() throws Exception {
        Reply none = asAdmin("GET", "/admin/catalogue/draft", null);
        assertEquals("catalogue.draft_not_found", none.code());

        Reply opened = asAdmin("POST", "/admin/catalogue/drafts", "{\"note\": \"a card\"}");
        assertEquals(201, opened.status());
        assertEquals("OPEN", opened.body().get("status").asString());
        assertEquals("api", opened.body().get("source").asString());
        String id = opened.body().get("id").asString();
        assertEquals("catalogue.draft_open_exists", asAdmin("POST", "/admin/catalogue/drafts", null).code());
        assertEquals(id, asAdmin("GET", "/admin/catalogue/draft", null).body().get("id").asString());

        String card = "{\"id\": \"T-1\", \"sets\": [\"S1\"], \"kind\": \"CREATURE\", \"rank\": 5, \"suit\": \"VINCOLO\", "
                + "\"subfaction\": \"ADATTATI\", \"atk\": 3000, \"def\": 5000, \"abilities\": []}";
        assertEquals(204, asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/sets/S1", "{\"status\": \"IN_VENDITA\"}").status());
        assertEquals(204, asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/cards/T-1", card).status());
        assertEquals(204, asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/texts/SET/S1/it", "{\"name\": \"Primo\"}").status());
        assertEquals("catalogue.item_invalid", asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/cards/OTHER", card).code(),
                "the id of the card must be the one in the path");

        Reply before = asAdmin("GET", "/admin/catalogue/drafts/" + id + "/preview", null);
        assertFalse(before.body().get("ok").asBoolean());
        assertTrue(before.body().toString().contains("draft.card_no_italian_text"));

        assertEquals(204, asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/texts/CARD/T-1/it", "{\"name\": \"Uno\", \"flavour\": \"Testo\"}").status());
        Reply ok = asAdmin("GET", "/admin/catalogue/drafts/" + id + "/preview", null);
        assertTrue(ok.body().get("ok").asBoolean(), ok.body().toString());
        assertEquals(1, ok.body().get("cardsToCreate").asInt());

        Reply detail = asAdmin("GET", "/admin/catalogue/drafts/" + id, null);
        assertEquals(4, detail.body().get("items").size());
        assertEquals(204, asAdmin("DELETE", "/admin/catalogue/drafts/" + id + "/items/SET/S1", null).status());
        assertEquals("catalogue.item_invalid", asAdmin("DELETE", "/admin/catalogue/drafts/" + id + "/items/SET/S1", null).code());
        assertEquals(204, asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/sets/S1", "{\"status\": \"IN_VENDITA\"}").status());

        Reply published = asAdmin("POST", "/admin/catalogue/drafts/" + id + "/publish", "{\"note\": \"first\"}");
        assertEquals(200, published.status(), published.body().toString());
        assertEquals(1, published.body().get("cardsCreated").asInt());
        assertEquals("catalogue.draft_closed", asAdmin("POST", "/admin/catalogue/drafts/" + id + "/publish", null).code());
        assertEquals(1, count("cards"));
    }

    @Test
    void aDraftWithAProblemIs422WithItsReport() throws Exception {
        String id = asAdmin("POST", "/admin/catalogue/drafts", null).body().get("id").asString();
        asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/cards/T-1", "{\"id\": \"T-1\", \"sets\": [\"NOPE\"], \"kind\": \"CREATURE\", "
                + "\"rank\": 5, \"suit\": \"VINCOLO\", \"subfaction\": \"ADATTATI\", \"atk\": 1, \"def\": 1, \"abilities\": []}");
        Reply r = asAdmin("POST", "/admin/catalogue/drafts/" + id + "/publish", null);
        assertEquals(422, r.status());
        assertEquals("catalogue.draft_invalid", r.code());
        assertFalse(r.body().get("report").get("ok").asBoolean());
        assertTrue(r.body().get("report").get("problems").toString().contains("draft.set_unknown"));
        assertEquals(204, asAdmin("DELETE", "/admin/catalogue/drafts/" + id, null).status());
        assertEquals("catalogue.draft_closed", asAdmin("PUT", "/admin/catalogue/drafts/" + id + "/sets/S1", "{}").code());
        assertEquals("catalogue.draft_not_found", asAdmin("GET", "/admin/catalogue/drafts/" + UUID.randomUUID(), null).code());
        assertEquals(400, asAdmin("GET", "/admin/catalogue/drafts/not-a-uuid", null).status());
    }

    // ---- import, export, drift ----

    @Test
    void anImportIsADryRunUnlessToldOtherwise() throws Exception {
        String body = BaseSet.MAPPER.writeValueAsString(BaseSet.batch());
        Reply dry = asAdmin("POST", "/admin/catalogue/import", body);
        assertEquals(200, dry.status(), dry.body().toString());
        assertTrue(dry.body().get("dryRun").asBoolean());
        assertEquals("ADD_ONLY", dry.body().get("mode").asString());
        assertTrue(dry.body().get("report").get("ok").asBoolean());
        assertEquals(0, count("cards"));

        seedViaImport();
        assertEquals(150, count("cards"));
        assertEquals(2, count("pool_snapshots"));
        Reply again = asAdmin("POST", "/admin/catalogue/import?dryRun=false", body);
        assertEquals(200, again.status());
        assertTrue(again.body().get("result").get("draftId").isNull(), "the second import has nothing to do");
        assertEquals(2, count("pool_snapshots"));
    }

    @Test
    void aBadImportIsRefused() throws Exception {
        assertEquals("catalogue.import_invalid", asAdmin("POST", "/admin/catalogue/import", "not json").code());
        assertEquals("catalogue.import_invalid", asAdmin("POST", "/admin/catalogue/import", "{}").code());
        assertEquals("catalogue.import_invalid", asAdmin("POST", "/admin/catalogue/import?mode=merge", "{\"cards\": []}").code());
        Reply big = asAdmin("POST", "/admin/catalogue/import", " ".repeat(700 * 1024) + "{\"cards\": []}");
        assertEquals(413, big.status());
        assertEquals("catalogue.import_too_large", big.code());
        assertEquals(403, call("POST", "/admin/catalogue/import", player, "{\"cards\": []}").status());
    }

    @Test
    void exportAndDriftAfterAnImport() throws Exception {
        seedViaImport();
        Reply export = asAdmin("GET", "/admin/catalogue/export", null);
        assertEquals(200, export.status());
        assertEquals(150, export.body().get("cards").size());
        assertEquals("BASE", export.body().get("set_definitions").get(0).get("code").asString());

        Reply drift = asAdmin("POST", "/admin/catalogue/drift", BaseSet.MAPPER.writeValueAsString(BaseSet.batch()));
        assertEquals(200, drift.status());
        assertTrue(drift.body().get("inSync").asBoolean(), drift.body().toString());
        assertEquals(403, call("POST", "/admin/catalogue/drift", player, "{\"cards\": []}").status());
        assertEquals("catalogue.import_invalid", asAdmin("POST", "/admin/catalogue/drift", "{}").code());
    }

    // ---- texts, sets, pack types ----

    @Test
    void textsAreReplacedDirectlyAndNamesStayUnique() throws Exception {
        seedViaImport();
        String a = "BASE-RES-7-SPI-SPE";
        String b = "BASE-RES-A-FRA-SPE";
        assertEquals(204, asAdmin("PUT", "/admin/catalogue/texts/CARD/" + a + "/it", "{\"name\": \"Nome nuovo\"}").status());
        assertEquals("Nome nuovo", jdbc.queryForObject("select name from card_texts where locale = 'it' and card_id = "
                + "(select id from cards where card_code = ?)", String.class, a));
        assertTrue(jdbc.queryForObject("select flavour from card_texts where locale = 'it' and card_id = "
                + "(select id from cards where card_code = ?)", String.class, a).length() > 5, "the flavour was left out, so it is kept");
        assertEquals(204, asAdmin("PUT", "/admin/catalogue/texts/CARD/" + a + "/en", "{\"name\": \"New name\", \"flavour\": \"Line\"}").status());

        Reply taken = asAdmin("PUT", "/admin/catalogue/texts/CARD/" + b + "/it", "{\"name\": \"nome NUOVO\"}");
        assertEquals(409, taken.status());
        assertEquals("catalogue.text_name_taken", taken.code());
        assertEquals("catalogue.card_not_found", asAdmin("PUT", "/admin/catalogue/texts/CARD/NOPE/it", "{\"name\": \"x\"}").code());
        assertEquals("catalogue.set_not_found", asAdmin("PUT", "/admin/catalogue/texts/SET/NOPE/it", "{\"name\": \"x\"}").code());
        assertEquals("catalogue.text_invalid", asAdmin("PUT", "/admin/catalogue/texts/CARD/" + a + "/ITALIANO", "{\"name\": \"x\"}").code());
        assertEquals("catalogue.text_invalid", asAdmin("PUT", "/admin/catalogue/texts/CARD/" + a + "/it", "{\"name\": \"  \"}").code());
        assertEquals("catalogue.text_invalid", asAdmin("PUT", "/admin/catalogue/texts/THING/" + a + "/it", "{\"name\": \"x\"}").code());
    }

    @Test
    void aSetsStatusAndMembershipCanBeChangedWithoutTouchingTheSnapshots() throws Exception {
        seedViaImport();
        JsonNode sets = asAdmin("GET", "/admin/catalogue/sets", null).body();
        assertEquals(1, sets.size());
        assertEquals("BASE", sets.get(0).get("code").asString());
        assertEquals(150, sets.get(0).get("cards").asInt());
        assertEquals("Set base", sets.get(0).get("names").get("it").asString());

        Reply closed = asAdmin("PUT", "/admin/catalogue/sets/BASE", "{\"status\": \"CHIUSA\", \"close_date\": \"2027-01-31\"}");
        assertEquals(200, closed.status(), closed.body().toString());
        assertEquals("CHIUSA", closed.body().get("status").asString());
        assertEquals("catalogue.set_invalid", asAdmin("PUT", "/admin/catalogue/sets/BASE", "{\"status\": \"WEIRD\"}").code());
        assertEquals("catalogue.set_invalid", asAdmin("PUT", "/admin/catalogue/sets/BASE", "{\"release_date\": \"2028-01-01\"}").code(),
                "closing before opening");
        assertEquals("catalogue.set_not_found", asAdmin("PUT", "/admin/catalogue/sets/NOPE", "{}").code());

        int snapshots = count("pool_snapshots");
        int definitions = count("card_definitions");
        String card = "BASE-RES-7-SPI-SPE";
        assertEquals(204, asAdmin("DELETE", "/admin/catalogue/sets/BASE/cards/" + card, null).status());
        assertEquals(149, asAdmin("GET", "/admin/catalogue/sets", null).body().get(0).get("cards").asInt());
        assertEquals(204, asAdmin("DELETE", "/admin/catalogue/sets/BASE/cards/" + card, null).status(), "taking it out twice is fine");
        assertEquals(snapshots, count("pool_snapshots"));
        assertEquals(definitions, count("card_definitions"), "the card stays in the snapshot");
        assertEquals(1, count("cards where card_code = '" + card + "'"));
        assertEquals(204, asAdmin("PUT", "/admin/catalogue/sets/BASE/cards/" + card, null).status());
        assertEquals(150, asAdmin("GET", "/admin/catalogue/sets", null).body().get(0).get("cards").asInt());
        assertEquals("catalogue.card_not_found", asAdmin("PUT", "/admin/catalogue/sets/BASE/cards/NOPE", null).code());
    }

    @Test
    void packTypesAreCreatedUpdatedAndDeleted() throws Exception {
        seedViaImport();
        String standard = "{\"set\": \"BASE\", \"price\": 200, \"slots\": {\"TIER_1\": 2, \"TIER_2\": 2}, \"support_slots\": 2}";
        Reply created = asAdmin("PUT", "/admin/catalogue/pack-types/standard", standard);
        assertEquals(200, created.status(), created.body().toString());
        assertEquals("BASE", created.body().get("set").asString());
        assertEquals(2, created.body().get("slots").get("TIER_2").asInt());
        assertEquals(0, created.body().get("slots").get("TIER_4").asInt());
        assertFalse(created.body().get("featured").asBoolean());

        Reply updated = asAdmin("PUT", "/admin/catalogue/pack-types/standard",
                "{\"set\": \"BASE\", \"price\": 250, \"slots\": {\"TIER_1\": 3}, \"support_slots\": 1, \"featured\": true}");
        assertEquals(250, updated.body().get("price").asInt());
        assertTrue(updated.body().get("featured").asBoolean());
        assertEquals(3, asAdmin("GET", "/admin/catalogue/pack-types", null).body().size(), "standard, advanced and elite came with the set");

        assertEquals("catalogue.pack_type_invalid", asAdmin("PUT", "/admin/catalogue/pack-types/free", "{\"set\": \"BASE\", \"price\": 0, \"support_slots\": 1}").code());
        assertEquals("catalogue.pack_type_invalid", asAdmin("PUT", "/admin/catalogue/pack-types/empty", "{\"set\": \"BASE\", \"price\": 5}").code());
        assertEquals("catalogue.pack_type_invalid", asAdmin("PUT", "/admin/catalogue/pack-types/Bad-Code", standard).code());
        assertEquals("catalogue.set_not_found", asAdmin("PUT", "/admin/catalogue/pack-types/other", standard.replace("BASE", "NOPE")).code());

        // a type that was sold keeps its set and cannot be deleted
        asAdmin("PUT", "/admin/catalogue/sets/BASE", "{}");
        jdbc.update("insert into users (username, email, password_hash, credits) values ('buyer', 'b@quorma.local', 'x', 1000)");
        jdbc.update("insert into pack_openings (user_id, idempotency_key, pack_type, set_id, faction, snapshot_id, price, credits_after) "
                + "select u.id, gen_random_uuid(), 'standard', t.set_id, 'RESISTENZA_UMANA', (select snapshot_id from active_snapshots where faction = 'RESISTENZA_UMANA'), 250, 750 "
                + "from users u, pack_types t where u.username = 'buyer' and t.code = 'standard'");
        Reply delete = asAdmin("DELETE", "/admin/catalogue/pack-types/standard", null);
        assertEquals(409, delete.status());
        assertEquals("catalogue.pack_type_conflict", delete.code());

        assertEquals(200, asAdmin("PUT", "/admin/catalogue/pack-types/spare", standard).status());
        assertEquals(204, asAdmin("DELETE", "/admin/catalogue/pack-types/spare", null).status());
        assertEquals("catalogue.pack_type_not_found", asAdmin("DELETE", "/admin/catalogue/pack-types/spare", null).code());
    }
}
