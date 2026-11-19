package quorma.backend.catalogue.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import quorma.backend.catalogue.admin.CatalogueAdminService;
import quorma.backend.catalogue.publish.CatalogueDrift.DriftReport;
import quorma.backend.catalogue.publish.CatalogueBootstrap.BootstrapOutcome;
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

/** The entrances to the pipeline: the bootstrap from set files, the batch import, the export and the drift report. */
class CatalogueImportTest extends IntegrationTest {

    private static final String RES = "RESISTENZA_UMANA";

    @Autowired
    CatalogueBootstrap bootstrap;
    @Autowired
    ChangesetImporter importer;
    @Autowired
    CatalogueExporter exporter;
    @Autowired
    CatalogueDrift drift;
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

    /** Loads the set files of the repository, as a deployment bootstrap would. */
    private BootstrapOutcome seed() {
        return bootstrap.bootstrap(BaseSet.SETS_DIR);
    }

    private Changeset baseSetFile() {
        return Changeset.parse(BaseSet.batch());
    }

    private UUID activeOf(String faction) {
        return jdbc.queryForObject("select snapshot_id from active_snapshots where faction = ?", UUID.class, faction);
    }

    private int atkOf(String code) {
        return jdbc.queryForObject("select d.atk from card_definitions d join cards c on c.id = d.card_id "
                + "where c.card_code = ? and d.snapshot_id = ?", Integer.class, code, activeOf(RES));
    }

    // ---- the set file ----

    @Test
    void theBaseSetFileIsAValidBatchWithItsSetAndTexts() {
        Changeset c = baseSetFile();
        assertEquals(150, c.cards().size());
        assertEquals("IN_VENDITA", c.sets().get(0).status());
        assertEquals(302, c.texts().get("it").size(), "150 names, 150 flavours, the set name and description");
    }

    // ---- the bootstrap from a folder of set files ----

    @Test
    void theBootstrapAddsTheBaseSetOnceAndFromTheSystemUser() {
        assertEquals(BootstrapOutcome.LOADED, seed());

        assertEquals(150, count("cards"));
        assertEquals(2, count("pool_snapshots"));
        assertEquals("IN_VENDITA", jdbc.queryForObject("select status from card_sets where code = 'BASE'", String.class));
        assertEquals(150, count("card_set_cards"));
        assertEquals("seed", jdbc.queryForObject("select distinct source from snapshot_publications", String.class));
        assertEquals(Role.ADMIN.name(), jdbc.queryForObject("select role from users where username = 'sistema'", String.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from snapshot_publications p join users u on u.id = p.published_by "
                + "where u.username = 'sistema'", Integer.class));

        UUID res = activeOf(RES);
        assertEquals(BootstrapOutcome.NOTHING_TO_ADD, seed(), "a second start adds nothing");
        assertEquals(res, activeOf(RES));
        assertEquals(1, count("catalogue_drafts"), "and leaves no draft behind");
    }

    @Test
    void aBootstrapNeverTouchesACardThatExistsEvenIfTheFileChanged() {
        seed();
        String code = "BASE-RES-7-SPI-SPE";
        int original = atkOf(code);

        // an admin edits the card (upsert), then the database is bootstrapped again with the file as it was
        ObjectNode edited = BaseSet.batch();
        ((ObjectNode) card(edited, code)).put("atk", original + 5);
        importer.run(batch(edited), Mode.UPSERT, false, "batch", admin1, "edit");
        assertEquals(original + 5, atkOf(code));

        assertEquals(BootstrapOutcome.NOTHING_TO_ADD, seed());
        assertEquals(original + 5, atkOf(code), "the edit of the admin survives");

        DriftReport report = drift.compare(baseSetFile());
        assertEquals(java.util.List.of(code), report.changedCards());
        assertFalse(report.inSync());
    }

    @Test
    void theBootstrapIsOffUnlessAFolderIsGiven() {
        bootstrap.run(null);
        assertEquals(0, count("cards"));
        assertEquals(0, count("catalogue_drafts"));
    }

    @Test
    void aFolderOfSeveralSetFilesIsLoadedAsOneBatchWithReprints() throws IOException {
        Path dir = Files.createTempDirectory("sets");
        Files.writeString(dir.resolve("BASE.json"), BaseSet.MAPPER.writeValueAsString(BaseSet.batch()));
        // X1: one new creature, and a reprint of a base card (same definition, now in both sets)
        ObjectNode reprint = BaseSet.card("BASE-RES-7-SPI-SPE");
        reprint.putArray("sets").add("BASE").add("X1");
        ObjectNode fresh = BaseSet.card(firstCreatureOfRank(5));
        fresh.put("id", "X1-RES-NEW");
        fresh.putArray("sets").add("X1");
        ObjectNode x1 = BaseSet.MAPPER.createObjectNode();
        x1.putArray("set_definitions").addObject().put("code", "X1");
        x1.putArray("cards").add(reprint).add(fresh);
        ObjectNode it = x1.putObject("texts").putObject("it");
        it.put("card.X1-RES-NEW.name", "Nuova di X1");
        it.put("set.X1.name", "Primo ampliamento");
        Files.writeString(dir.resolve("X1.json"), BaseSet.MAPPER.writeValueAsString(x1));

        assertEquals(BootstrapOutcome.LOADED, bootstrap.bootstrap(dir));

        assertEquals(151, count("cards"));
        assertEquals("BOZZA", jdbc.queryForObject("select status from card_sets where code = 'X1'", String.class));
        assertEquals("IN_VENDITA", jdbc.queryForObject("select status from card_sets where code = 'BASE'", String.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from card_set_cards l join cards c on c.id = l.card_id "
                + "where c.card_code = 'BASE-RES-7-SPI-SPE'", Integer.class), "the reprint is in both sets");
        assertEquals(1, jdbc.queryForObject("select count(*) from card_set_cards l join card_sets s on s.id = l.set_id "
                + "join cards c on c.id = l.card_id where s.code = 'X1' and c.card_code = 'X1-RES-NEW'", Integer.class));
        assertEquals(2, count("pool_snapshots"), "one batch: one snapshot per faction, not one per file");
        assertEquals(1, count("catalogue_drafts"));
    }

    @Test
    void aBadFolderIsLoggedAndNeverThrows() throws IOException {
        assertEquals(BootstrapOutcome.FAILED, bootstrap.bootstrap(Path.of("no", "such", "folder")));
        assertEquals(BootstrapOutcome.FAILED, bootstrap.bootstrap(Files.createTempDirectory("empty")));
        Path broken = Files.createTempDirectory("broken");
        Files.writeString(broken.resolve("X.json"), "{\"cards\": 5}");
        assertEquals(BootstrapOutcome.FAILED, bootstrap.bootstrap(broken));
        assertEquals(0, count("cards"));
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
        seed();
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
        seed();
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
        seed();
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
        seed();
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
        seed();
        admin.putText("CARD", "BASE-RES-7-SPI-SPE", "it", BaseSet.MAPPER.readTree("{\"name\": \"Nome nuovo\", \"flavour\": \"Testo nuovo\"}"));
        ObjectNode exported = exporter.export();
        assertEquals("Nome nuovo", exported.get("texts").get("it").get("card.BASE-RES-7-SPI-SPE.name").asString());
        assertEquals("Testo nuovo", exported.get("texts").get("it").get("card.BASE-RES-7-SPI-SPE.flavour").asString());
    }

    // ---- drift ----

    @Test
    void driftSaysWhatDiffers() {
        DriftReport empty = drift.compare(baseSetFile());
        assertFalse(empty.inSync());
        assertEquals(150, empty.missingCards().size());

        seed();
        DriftReport synced = drift.compare(baseSetFile());
        assertTrue(synced.inSync(), synced.toString());
        assertEquals(0, synced.extraCards().size());
        assertEquals(0, synced.differingTexts().size());

        admin.putText("CARD", "BASE-RES-7-SPI-SPE", "it", BaseSet.MAPPER.readTree("{\"name\": \"Nome nuovo\"}"));
        DriftReport edited = drift.compare(baseSetFile());
        assertTrue(edited.inSync(), "an edited text is informational, not a missing piece");
        assertTrue(edited.differingTexts().contains("it:card.BASE-RES-7-SPI-SPE.name"));
        assertEquals(java.util.List.of("it:card.BASE-RES-7-SPI-SPE.name"), edited.differingTexts(), "the flavour was left out, so it is kept");
    }

    // ---- helpers ----

    private static String firstCreatureOfRank(int rank) {
        return BaseSet.cards().stream()
                .filter(c -> c.get("kind").asString().equals("CREATURE") && c.get("rank").asInt() == rank
                        && c.get("id").asString().startsWith("BASE-RES-"))
                .findFirst().orElseThrow().get("id").asString();
    }

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
