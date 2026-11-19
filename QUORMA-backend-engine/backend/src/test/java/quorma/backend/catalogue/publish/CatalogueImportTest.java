package quorma.backend.catalogue.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import quorma.backend.catalogue.admin.CatalogueAdminService;
import quorma.backend.catalogue.publish.CatalogueDrift.DriftReport;
import quorma.backend.catalogue.publish.CatalogueSeeder.SeedOutcome;
import quorma.backend.catalogue.publish.CataloguePublisher.Outcome;
import quorma.backend.catalogue.publish.ChangesetImporter.ImportResponse;
import quorma.backend.catalogue.publish.ChangesetImporter.Mode;
import quorma.backend.common.ErrorCodes;
import quorma.backend.support.BaseSet;
import quorma.backend.support.IntegrationTest;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** The entrances to the pipeline: the startup seed, the batch import, the export and the drift report. */
class CatalogueImportTest extends IntegrationTest {

    private static final String RES = "RESISTENZA_UMANA";

    @Autowired
    CatalogueSeeder seeder;
    @Autowired
    ChangesetImporter importer;
    @Autowired
    CatalogueExporter exporter;
    @Autowired
    CatalogueDrift drift;
    @Autowired
    BundledCatalogue bundled;
    @Autowired
    CatalogueAdminService admin;
    @Autowired
    UserRepository users;

    UUID admin1;

    @BeforeEach
    void createAdmin() {
        admin1 = users.save(new User("admin1", "admin1@quorma.local", "x", Role.ADMIN, 0)).getId();
    }

    private Changeset batch(ObjectNode root) {
        return Changeset.parse(root);
    }

    private UUID activeOf(String faction) {
        return jdbc.queryForObject("select snapshot_id from active_snapshots where faction = ?", UUID.class, faction);
    }

    private int atkOf(String code) {
        return jdbc.queryForObject("select d.atk from card_definitions d join cards c on c.id = d.card_id "
                + "where c.card_code = ? and d.snapshot_id = ?", Integer.class, code, activeOf(RES));
    }

    // ---- the bundled file ----

    @Test
    void theBundledBaseSetIsOnTheClasspathAndIsTheSameAsTheDataFolder() {
        Changeset c = bundled.load();
        assertEquals(150, c.cards().size());
        assertEquals("IN_VENDITA", c.sets().get(0).status());
        assertEquals(302, c.texts().get("it").size(), "150 names, 150 flavours, the set name and description");
    }

    // ---- the startup seed ----

    @Test
    void theSeedAddsTheBaseSetOnceAndFromTheSystemUser() {
        assertEquals(SeedOutcome.SEEDED, seeder.seed());

        assertEquals(150, count("cards"));
        assertEquals(2, count("pool_snapshots"));
        assertEquals("IN_VENDITA", jdbc.queryForObject("select status from card_sets where code = 'BASE'", String.class));
        assertEquals(150, count("card_set_cards"));
        assertEquals("seed", jdbc.queryForObject("select distinct source from snapshot_publications", String.class));
        assertEquals(Role.ADMIN.name(), jdbc.queryForObject("select role from users where username = 'sistema'", String.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from snapshot_publications p join users u on u.id = p.published_by "
                + "where u.username = 'sistema'", Integer.class));

        UUID res = activeOf(RES);
        assertEquals(SeedOutcome.NOTHING_TO_ADD, seeder.seed(), "a second start adds nothing");
        assertEquals(res, activeOf(RES));
        assertEquals(1, count("catalogue_drafts"), "and leaves no draft behind");
    }

    @Test
    void aStartupNeverTouchesACardThatExistsEvenIfTheFileChanged() {
        seeder.seed();
        String code = "BASE-RES-7-SPI-SPE";
        int original = atkOf(code);

        // an admin edits the card (upsert), then the backend restarts with the file as it was
        ObjectNode edited = BaseSet.batch();
        ((ObjectNode) card(edited, code)).put("atk", original + 5);
        importer.run(batch(edited), Mode.UPSERT, false, "batch", admin1, "edit");
        assertEquals(original + 5, atkOf(code));

        assertEquals(SeedOutcome.NOTHING_TO_ADD, seeder.seed());
        assertEquals(original + 5, atkOf(code), "the admin's edit survives the restart");

        DriftReport report = drift.compare(bundled.load());
        assertEquals(java.util.List.of(code), report.changedCards());
        assertFalse(report.inSync());
    }

    // ---- the import ----

    @Test
    void aDryRunChecksEverythingAndKeepsNothing() {
        ImportResponse r = importer.run(batch(BaseSet.batch()), Mode.ADD_ONLY, true, "batch", admin1, null);

        assertTrue(r.dryRun());
        assertTrue(r.report().ok(), r.report().problems().toString());
        assertEquals(150, r.cards());
        assertEquals(150, r.report().cardsToCreate());
        assertNull(r.result());
        assertEquals(0, count("cards"));
        assertEquals(0, count("catalogue_drafts"));
        assertEquals(0, count("card_sets"));
        assertEquals(0, count("pool_snapshots"));
    }

    @Test
    void aBatchWithAProblemIsRefusedWithItsReportAndKeepsNothing() {
        ObjectNode bad = BaseSet.batch();
        String rank5 = null;
        for (JsonNode c : bad.get("cards")) {
            if (c.get("kind").asString().equals("CREATURE") && c.get("rank").asInt() == 5 && c.get("id").asString().startsWith("BASE-RES-")) {
                rank5 = c.get("id").asString();
                break;
            }
        }
        ((ArrayNode) card(bad, rank5).get("abilities")).addObject().put("id", "K_LETALE");

        CatalogueException e = assertThrows(CatalogueException.class,
                () -> importer.run(batch(bad), Mode.ADD_ONLY, false, "batch", admin1, null));

        assertEquals(ErrorCodes.CATALOGUE_DRAFT_INVALID, e.code());
        assertTrue(e.report().problems().stream().anyMatch(p -> p.code().equals("card.rank_not_allowed")));
        assertEquals(0, count("cards"));
        assertEquals(0, count("pool_snapshots"));
        assertEquals(0, count("catalogue_drafts"), "no draft is left behind");
    }

    @Test
    void addOnlyIgnoresAChangedCardAndUpsertAppliesIt() {
        seeder.seed();
        UUID before = activeOf(RES);
        ObjectNode changed = BaseSet.batch();
        ObjectNode card = (ObjectNode) card(changed, "BASE-RES-7-SPI-SPE");
        card.put("atk", card.get("atk").asInt() + 1);

        ImportResponse addOnly = importer.run(batch(changed), Mode.ADD_ONLY, false, "batch", admin1, null);
        assertNull(addOnly.result().draftId(), "nothing to do: no draft was opened");
        assertTrue(addOnly.result().factions().stream().allMatch(f -> f.outcome() == Outcome.UNCHANGED));
        assertEquals(before, activeOf(RES));

        ImportResponse upsert = importer.run(batch(changed), Mode.UPSERT, false, "batch", admin1, "tweak");
        assertEquals(Outcome.NEW_SNAPSHOT, upsert.result().factions().stream().filter(f -> f.faction().equals(RES)).findFirst().orElseThrow().outcome());
        assertEquals(1, upsert.report().factions().stream().filter(f -> f.faction().equals(RES)).findFirst().orElseThrow().changed());
        assertFalse(before.equals(activeOf(RES)));
        assertEquals("batch", jdbc.queryForObject("select source from snapshot_publications order by published_at desc limit 1", String.class));
    }

    @Test
    void aNewLanguageFileFillsInWithoutReplacingAnything() {
        seeder.seed();
        jdbc.update("update card_texts set name = 'Rinominata' where locale = 'it' and card_id = (select id from cards where card_code = 'BASE-RES-7-SPI-SPE')");

        ObjectNode texts = BaseSet.batch();
        texts.set("cards", MAPPER_ARRAY());
        ObjectNode en = ((ObjectNode) texts.get("texts")).putObject("en");
        en.put("card.BASE-RES-7-SPI-SPE.name", "Seventh");
        en.put("card.BASE-RES-7-SPI-SPE.flavour", "A line.");
        en.put("set.BASE.name", "Base set");
        // the Italian table of the batch carries the original names: the renamed one must stay as it is
        ImportResponse r = importer.run(batch(texts), Mode.ADD_ONLY, false, "batch", admin1, null);

        assertEquals(2, r.result().textsWritten(), "one row for the card (name and flavour) and one for the set");
        assertEquals("Seventh", jdbc.queryForObject("select name from card_texts where locale = 'en'", String.class));
        assertEquals("Rinominata", jdbc.queryForObject("select name from card_texts where locale = 'it' and card_id = "
                + "(select id from cards where card_code = 'BASE-RES-7-SPI-SPE')", String.class));
    }

    @Test
    void textForACardThatDoesNotExistIsAProblem() {
        seeder.seed();
        ObjectNode texts = BaseSet.batch();
        texts.set("cards", MAPPER_ARRAY());
        ((ObjectNode) texts.get("texts")).putObject("en").put("card.NO-SUCH-CARD.name", "Ghost");
        ImportResponse r = importer.run(batch(texts), Mode.ADD_ONLY, true, "batch", admin1, null);
        assertFalse(r.report().ok());
        assertTrue(r.report().problems().stream().anyMatch(p -> p.code().equals("draft.text_unknown_target")));
    }

    // ---- export ----

    @Test
    void exportThenImportAgainChangesNothing() {
        seeder.seed();
        ObjectNode exported = exporter.export();

        assertEquals(150, exported.get("cards").size());
        assertEquals("BASE", exported.get("set_definitions").get(0).get("code").asString());
        assertEquals(302, exported.get("texts").get("it").size());
        for (JsonNode c : exported.get("cards")) {
            assertEquals("BASE", c.get("sets").get(0).asString());
        }

        ImportResponse r = importer.run(batch(exported), Mode.UPSERT, false, "batch", admin1, null);
        assertNull(r.result().draftId(), "the export is exactly what is there: nothing to do");
        assertTrue(r.result().factions().stream().allMatch(f -> f.outcome() == Outcome.UNCHANGED));
        assertEquals(2, count("pool_snapshots"));
    }

    @Test
    void anExportCarriesTheEditsOfAnAdmin() {
        seeder.seed();
        admin.putText("CARD", "BASE-RES-7-SPI-SPE", "it", BaseSet.MAPPER.readTree("{\"name\": \"Nome nuovo\", \"flavour\": \"Testo nuovo\"}"));
        ObjectNode exported = exporter.export();
        assertEquals("Nome nuovo", exported.get("texts").get("it").get("card.BASE-RES-7-SPI-SPE.name").asString());
        assertEquals("Testo nuovo", exported.get("texts").get("it").get("card.BASE-RES-7-SPI-SPE.flavour").asString());
    }

    // ---- drift ----

    @Test
    void driftSaysWhatDiffers() {
        DriftReport empty = drift.compare(bundled.load());
        assertFalse(empty.inSync());
        assertEquals(150, empty.missingCards().size());

        seeder.seed();
        DriftReport synced = drift.compare(bundled.load());
        assertTrue(synced.inSync(), synced.toString());
        assertEquals(0, synced.extraCards().size());
        assertEquals(0, synced.differingTexts().size());

        admin.putText("CARD", "BASE-RES-7-SPI-SPE", "it", BaseSet.MAPPER.readTree("{\"name\": \"Nome nuovo\"}"));
        DriftReport edited = drift.compare(bundled.load());
        assertTrue(edited.inSync(), "an edited text is informational, not a missing piece");
        assertTrue(edited.differingTexts().contains("it:card.BASE-RES-7-SPI-SPE.name"));
        assertEquals(java.util.List.of("it:card.BASE-RES-7-SPI-SPE.name"), edited.differingTexts(), "the flavour was left out, so it is kept");
    }

    // ---- helpers ----

    private static JsonNode card(ObjectNode batch, String code) {
        for (JsonNode c : batch.get("cards")) {
            if (c.get("id").asString().equals(code)) {
                return c;
            }
        }
        throw new AssertionError("no card " + code);
    }

    private static ArrayNode MAPPER_ARRAY() {
        return BaseSet.MAPPER.createArrayNode();
    }
}
