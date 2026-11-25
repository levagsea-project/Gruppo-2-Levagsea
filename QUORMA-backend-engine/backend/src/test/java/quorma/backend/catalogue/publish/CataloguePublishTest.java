package quorma.backend.catalogue.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import quorma.backend.catalogue.DraftItemType;
import quorma.backend.catalogue.publish.CataloguePublisher.Outcome;
import quorma.backend.catalogue.publish.CataloguePublisher.PublishResult;
import quorma.backend.catalogue.publish.DraftReport.Problem;
import quorma.backend.common.ErrorCodes;
import quorma.backend.support.BaseSet;
import quorma.backend.support.IntegrationTest;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** The publishing pipeline against a real PostgreSQL and the real engine's card check. */
class CataloguePublishTest extends IntegrationTest {

    private static final String RES = "RESISTENZA_UMANA";
    private static final String AI = "POTENZE_ARTIFICIALI";

    @Autowired
    DraftService drafts;
    @Autowired
    CataloguePublisher publisher;
    @Autowired
    UserRepository users;

    UUID admin;

    @BeforeEach
    void createAdmin() {
        admin = users.save(new User("sistema", "sistema@quorma.local", "x", Role.ADMIN, 0, "sistema", "avatar-01")).getId();
    }

    private UUID openWithBaseSet() {
        UUID id = drafts.open("seed", "test", admin).getId();
        BaseSet.fill(drafts, id);
        return id;
    }

    private UUID activeOf(String faction) {
        return jdbc.queryForObject("select snapshot_id from active_snapshots where faction = ?", UUID.class, faction);
    }

    private List<String> codes(List<Problem> problems) {
        return problems.stream().map(Problem::code).toList();
    }

    // ---- the happy path ----

    @Test
    void publishesTheWholeBaseSet() {
        UUID draft = openWithBaseSet();

        DraftReport preview = drafts.preview(draft);
        assertEquals(List.of(), preview.problems());
        assertEquals(List.of(), preview.warnings());
        assertTrue(preview.ok());
        assertEquals(2, preview.factions().size());
        assertTrue(preview.factions().stream().allMatch(f -> f.touched() && f.added() == 75 && f.total() == 75
                && f.basedOn() == null && !f.reactivates() && f.contentHash().matches("^[0-9a-f]{64}$")));
        assertEquals(0, count("pool_snapshots"), "a preview writes nothing");

        PublishResult result = publisher.publish(draft, admin, "base set");

        assertEquals(150, result.cardsCreated());
        assertEquals(151, result.textsWritten(), "150 card texts and the text of the set");
        assertEquals(1, result.setsCreated());
        assertEquals(150, result.linksAdded());
        assertTrue(result.factions().stream().allMatch(f -> f.outcome() == Outcome.NEW_SNAPSHOT));

        assertEquals(2, count("pool_snapshots"));
        assertEquals(150, count("card_definitions"));
        assertEquals(150, count("cards"));
        assertEquals(150, count("card_texts"));
        assertEquals(150, count("card_set_cards"));
        assertEquals(2, count("active_snapshots"));
        assertEquals(result.factions().get(0).snapshotId(), activeOf(result.factions().get(0).faction()));
        assertEquals(result.factions().get(1).snapshotId(), activeOf(result.factions().get(1).faction()));
        assertEquals(75, jdbc.queryForObject("select count(*) from card_definitions where snapshot_id = ?", Integer.class, activeOf(RES)));
        assertEquals("seed", jdbc.queryForObject("select distinct source from snapshot_publications", String.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from snapshot_publications where not reactivated and published_by = ?",
                Integer.class, admin));
        assertEquals("PUBLISHED", jdbc.queryForObject("select status from catalogue_drafts where id = ?", String.class, draft));
        assertEquals("it", jdbc.queryForObject("select distinct locale from card_texts", String.class));
        assertEquals("1.0-alpha", jdbc.queryForObject("select distinct ruleset_version from pool_snapshots", String.class));
    }

    @Test
    void aPublishedSnapshotIsSealed() {
        publisher.publish(openWithBaseSet(), admin, null);
        DataAccessException e = assertThrows(DataAccessException.class, () -> jdbc.update(
                "insert into card_definitions (snapshot_id, card_id, faction, kind, abilities) "
                        + "select ?, id, faction, kind, '[]' from cards where card_code = 'BASE-RES-EQU-10'", activeOf(RES)));
        assertTrue(e.getMessage().contains("sealed"), e.getMessage());
    }

    @Test
    void publishingTheSameCardsAgainWritesNothing() {
        publisher.publish(openWithBaseSet(), admin, null);
        UUID resBefore = activeOf(RES);

        UUID again = openWithBaseSet();
        DraftReport preview = drafts.preview(again);
        assertTrue(preview.ok());
        assertTrue(preview.factions().stream().noneMatch(f -> f.touched()));
        assertEquals(0, preview.cardsToCreate());
        assertEquals(0, preview.textsToWrite());
        assertEquals(0, preview.linksToAdd());

        PublishResult result = publisher.publish(again, admin, null);
        assertTrue(result.factions().stream().allMatch(f -> f.outcome() == Outcome.UNCHANGED));
        assertEquals(2, count("pool_snapshots"));
        assertEquals(2, count("snapshot_publications"));
        assertEquals(resBefore, activeOf(RES));
        assertEquals("PUBLISHED", jdbc.queryForObject("select status from catalogue_drafts where id = ?", String.class, again));
    }

    // ---- A, B, A ----

    @Test
    void aChangedCardIsANewSnapshotAndRevertingBringsTheOldOneBack() {
        publisher.publish(openWithBaseSet(), admin, null);
        UUID resA = activeOf(RES);
        UUID aiA = activeOf(AI);

        // B: one Resistenza creature gets 1 more attack
        ObjectNode changed = BaseSet.card("BASE-RES-7-SPI-SPE");
        changed.put("atk", changed.get("atk").asInt() + 1);
        UUID b = drafts.open("api", "tweak", admin).getId();
        drafts.putItem(b, DraftItemType.CARD, changed);
        DraftReport preview = drafts.preview(b);
        assertEquals(List.of(), preview.problems());
        DraftReport.FactionPreview res = preview.factions().stream().filter(f -> f.faction().equals(RES)).findFirst().orElseThrow();
        assertTrue(res.touched());
        assertEquals(0, res.added());
        assertEquals(1, res.changed());
        assertEquals(74, res.unchanged());
        assertEquals(resA, res.basedOn());
        assertFalse(preview.factions().stream().filter(f -> f.faction().equals(AI)).findFirst().orElseThrow().touched());

        PublishResult resultB = publisher.publish(b, admin, null);
        UUID resB = activeOf(RES);
        assertNotEquals(resA, resB);
        assertEquals(aiA, activeOf(AI), "the other faction keeps its snapshot");
        assertEquals(3, count("pool_snapshots"));
        assertEquals(75, jdbc.queryForObject("select count(*) from card_definitions where snapshot_id = ?", Integer.class, resB),
                "a snapshot holds the whole pool");
        assertEquals(150, count("cards"), "a changed card is the same card with a new definition");
        assertEquals(Outcome.NEW_SNAPSHOT, resultB.factions().stream().filter(f -> f.faction().equals(RES)).findFirst().orElseThrow().outcome());
        assertEquals(Outcome.UNCHANGED, resultB.factions().stream().filter(f -> f.faction().equals(AI)).findFirst().orElseThrow().outcome());

        // A again: the old snapshot comes back, nothing is inserted
        UUID back = drafts.open("api", "revert", admin).getId();
        drafts.putItem(back, DraftItemType.CARD, BaseSet.card("BASE-RES-7-SPI-SPE"));
        DraftReport revert = drafts.preview(back);
        assertTrue(revert.factions().stream().filter(f -> f.faction().equals(RES)).findFirst().orElseThrow().reactivates());
        PublishResult resultA = publisher.publish(back, admin, null);
        assertEquals(resA, activeOf(RES), "the original snapshot is active again");
        assertEquals(3, count("pool_snapshots"));
        assertEquals(Outcome.REACTIVATED, resultA.factions().stream().filter(f -> f.faction().equals(RES)).findFirst().orElseThrow().outcome());
        assertEquals(1, jdbc.queryForObject("select count(*) from snapshot_publications where reactivated", Integer.class));
        assertEquals(resB, jdbc.queryForObject("select based_on_id from snapshot_publications where reactivated", UUID.class));
    }

    // ---- a bad draft ----

    @Test
    void aCardTheEngineRejectsStopsThePublishAndNothingIsWritten() {
        UUID draft = openWithBaseSet();
        String rank5 = BaseSet.cards().stream()
                .filter(c -> c.get("kind").asString().equals("CREATURE") && c.get("id").asString().startsWith("BASE-RES-")
                        && c.get("rank").asInt() == 5)
                .findFirst().orElseThrow().get("id").asString();
        ObjectNode bad = BaseSet.card(rank5);
        // K_LETALE is only allowed on the Ace
        ((ArrayNode) bad.get("abilities")).addObject().put("id", "K_LETALE");
        drafts.putItem(draft, DraftItemType.CARD, bad);

        CatalogueException e = assertThrows(CatalogueException.class, () -> publisher.publish(draft, admin, null));
        assertEquals(ErrorCodes.CATALOGUE_DRAFT_INVALID, e.code());
        assertTrue(codes(e.report().problems()).contains("card.rank_not_allowed"), e.report().problems().toString());
        assertEquals(bad.get("id").asString(), e.report().problems().stream()
                .filter(p -> p.code().equals("card.rank_not_allowed")).findFirst().orElseThrow().subject());

        assertEquals(0, count("pool_snapshots"));
        assertEquals(0, count("cards"));
        assertEquals(0, count("card_texts"));
        assertEquals(0, count("card_sets"));
        assertEquals(0, count("active_snapshots"));
        assertEquals("OPEN", jdbc.queryForObject("select status from catalogue_drafts where id = ?", String.class, draft));
    }

    @Test
    void theDraftChecksThingsTheEngineDoesNotKnow() {
        UUID draft = drafts.open("batch", null, admin).getId();
        ObjectNode inUnknownSet = creature("T-A", 5, "[]");
        inUnknownSet.putArray("sets").add("NOPE");
        drafts.putItem(draft, DraftItemType.CARD, inUnknownSet);
        drafts.putItem(draft, DraftItemType.CARD, creature("T-B", 6, "[]"));
        drafts.putItem(draft, DraftItemType.CARD, creature("T-C", 6, "[]"));
        drafts.putItem(draft, DraftItemType.TEXT, text("CARD", "T-A", "it", "Uno"));
        drafts.putItem(draft, DraftItemType.TEXT, text("CARD", "T-B", "it", "Due"));
        drafts.putItem(draft, DraftItemType.TEXT, text("CARD", "T-C", "it", "due"));
        drafts.putItem(draft, DraftItemType.TEXT, text("CARD", "T-GHOST", "it", "Fantasma"));

        DraftReport report = drafts.preview(draft);
        assertFalse(report.ok());
        assertTrue(codes(report.problems()).contains("draft.set_unknown"));
        assertTrue(codes(report.problems()).contains("draft.text_name_taken"), "Due and due are the same name");
        assertTrue(codes(report.problems()).contains("draft.text_unknown_target"));
        assertEquals(List.of("draft.card_in_no_set", "draft.card_in_no_set"), codes(report.warnings()),
                "T-B and T-C are in no set");
    }

    @Test
    void aNewCardNeedsItsItalianText() {
        UUID draft = drafts.open("api", null, admin).getId();
        drafts.putItem(draft, DraftItemType.CARD, creature("T-A", 5, "[]"));
        drafts.putItem(draft, DraftItemType.TEXT, text("CARD", "T-A", "en", "One"));
        assertTrue(codes(drafts.preview(draft).problems()).contains("draft.card_no_italian_text"));
    }

    @Test
    void aCardKeepsItsIdentity() {
        publisher.publish(openWithBaseSet(), admin, null);
        ObjectNode other = BaseSet.card("BASE-RES-7-SPI-SPE");
        other.put("rank", 8);
        UUID draft = drafts.open("api", null, admin).getId();
        drafts.putItem(draft, DraftItemType.CARD, other);
        assertTrue(codes(drafts.preview(draft).problems()).contains("draft.card_identity_changed"));
        CatalogueException e = assertThrows(CatalogueException.class, () -> publisher.publish(draft, admin, null));
        assertEquals(ErrorCodes.CATALOGUE_DRAFT_INVALID, e.code());
    }

    // ---- texts ----

    @Test
    void anExistingTextIsNeverReplaced() {
        publisher.publish(openWithBaseSet(), admin, null);
        jdbc.update("update card_texts set name = 'Rinominata' where locale = 'it' and card_id = "
                + "(select id from cards where card_code = 'BASE-RES-7-SPI-SPE')");

        PublishResult result = publisher.publish(openWithBaseSet(), admin, null);

        assertEquals(0, result.textsWritten());
        assertEquals("Rinominata", jdbc.queryForObject("select name from card_texts where locale = 'it' and card_id = "
                + "(select id from cards where card_code = 'BASE-RES-7-SPI-SPE')", String.class));
    }

    // ---- the draft itself ----

    @Test
    void onlyOneDraftIsOpenAtATime() {
        drafts.open("api", null, admin);
        CatalogueException e = assertThrows(CatalogueException.class, () -> drafts.open("api", null, admin));
        assertEquals(ErrorCodes.CATALOGUE_DRAFT_OPEN_EXISTS, e.code());
    }

    @Test
    void aDiscardedOrPublishedDraftCannotBeUsed() {
        UUID d = drafts.open("api", null, admin).getId();
        drafts.discard(d);
        assertEquals(ErrorCodes.CATALOGUE_DRAFT_CLOSED,
                assertThrows(CatalogueException.class, () -> drafts.putItem(d, DraftItemType.SET, setItem("X"))).code());
        assertEquals(ErrorCodes.CATALOGUE_DRAFT_CLOSED,
                assertThrows(CatalogueException.class, () -> publisher.publish(d, admin, null)).code());
        assertEquals(ErrorCodes.CATALOGUE_DRAFT_NOT_FOUND,
                assertThrows(CatalogueException.class, () -> drafts.get(UUID.randomUUID())).code());
        assertTrue(drafts.open("api", null, admin).getId() != null, "after a discard a new draft can be opened");
    }

    @Test
    void anItemWithoutAKeyIsRefused() {
        UUID d = drafts.open("api", null, admin).getId();
        ObjectNode noId = BaseSet.MAPPER.createObjectNode();
        noId.put("kind", "CREATURE");
        assertEquals(ErrorCodes.CATALOGUE_ITEM_INVALID,
                assertThrows(CatalogueException.class, () -> drafts.putItem(d, DraftItemType.CARD, noId)).code());
    }

    @Test
    void puttingAnItemTwiceReplacesItAndItCanBeRemoved() {
        UUID d = drafts.open("api", null, admin).getId();
        drafts.putItem(d, DraftItemType.SET, setItem("X1"));
        ObjectNode closed = setItem("X1");
        closed.put("status", "CHIUSA");
        drafts.putItem(d, DraftItemType.SET, closed);
        assertEquals(1, drafts.items(d).size());
        assertTrue(drafts.items(d).get(0).getPayload().contains("CHIUSA"));
        assertTrue(drafts.removeItem(d, DraftItemType.SET, "X1"));
        assertFalse(drafts.removeItem(d, DraftItemType.SET, "X1"));
    }

    // ---- stale base, concurrency ----

    @Test
    void aDraftWhoseBaseMovedIsRefusedUntilItIsRebased() {
        publisher.publish(openWithBaseSet(), admin, null);
        UUID draft = drafts.open("api", null, admin).getId();
        drafts.putItem(draft, DraftItemType.CARD, BaseSet.card("BASE-RES-7-SPI-SPE"));
        // as if the active snapshot had moved since the draft was opened
        jdbc.update("update draft_bases set snapshot_id = null where draft_id = ? and faction = ?", draft, RES);

        assertEquals(ErrorCodes.CATALOGUE_DRAFT_STALE,
                assertThrows(CatalogueException.class, () -> publisher.publish(draft, admin, null)).code());
        assertEquals("OPEN", jdbc.queryForObject("select status from catalogue_drafts where id = ?", String.class, draft));

        drafts.rebase(draft);
        assertEquals(activeOf(RES), jdbc.queryForObject("select snapshot_id from draft_bases where draft_id = ? and faction = ?",
                UUID.class, draft, RES));
        publisher.publish(draft, admin, null);
        assertEquals("PUBLISHED", jdbc.queryForObject("select status from catalogue_drafts where id = ?", String.class, draft));
    }

    @Test
    void twoPublishesAtOnceAreSerialisedAndOnlyOneWins() throws Exception {
        UUID draft = openWithBaseSet();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> {
                go.await();
                try {
                    return publisher.publish(draft, admin, null);
                } catch (CatalogueException e) {
                    return e;
                }
            });
        }
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<Object> t : tasks) {
            futures.add(pool.submit(t));
        }
        go.countDown();
        int wins = 0;
        int refused = 0;
        for (Future<Object> f : futures) {
            try {
                Object r = f.get();
                if (r instanceof PublishResult) {
                    wins++;
                } else if (r instanceof CatalogueException e && e.code().equals(ErrorCodes.CATALOGUE_DRAFT_CLOSED)) {
                    refused++;
                }
            } catch (ExecutionException e) {
                throw new AssertionError("a publish failed with something other than a refusal", e.getCause());
            }
        }
        pool.shutdown();
        assertEquals(1, wins);
        assertEquals(1, refused);
        assertEquals(2, count("pool_snapshots"));
        assertEquals(150, count("card_definitions"));
    }

    // ---- helpers ----

    private static ObjectNode creature(String id, int rank, String abilitiesJson) {
        ObjectNode c = BaseSet.MAPPER.createObjectNode();
        c.put("id", id);
        c.put("kind", "CREATURE");
        c.put("rank", rank);
        c.put("suit", "VINCOLO");
        c.put("subfaction", "ADATTATI");
        c.put("atk", 3000);
        c.put("def", 5000);
        c.set("abilities", BaseSet.MAPPER.readTree(abilitiesJson));
        return c;
    }

    private static ObjectNode text(String kind, String code, String locale, String name) {
        ObjectNode t = BaseSet.MAPPER.createObjectNode();
        t.put("kind", kind);
        t.put("code", code);
        t.put("locale", locale);
        t.put("name", name);
        return t;
    }

    private static ObjectNode setItem(String code) {
        ObjectNode s = BaseSet.MAPPER.createObjectNode();
        s.put("code", code);
        return s;
    }
}
