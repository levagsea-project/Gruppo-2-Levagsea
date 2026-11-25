package quorma.backend.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import quorma.backend.catalogue.publish.CatalogueBootstrap;
import quorma.backend.support.BaseSet;
import quorma.backend.support.IntegrationTest;

/**
 * What the database itself guarantees, whatever the application does: what a match plays is append-only, what is only
 * commercial or textual can change, and a card cannot exist without its Italian text.
 */
class CatalogueTriggersTest extends IntegrationTest {

    @Autowired
    CatalogueBootstrap bootstrap;
    @Autowired
    PlatformTransactionManager txManager;

    @BeforeEach
    void loadTheBaseSet() {
        bootstrap.bootstrap(BaseSet.SETS_DIR);
    }

    /** The message of the root cause: a trigger that fails at commit is reported by Spring as "Unable to commit". */
    private static String rootMessage(Throwable t) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(t);
        return cause.getMessage();
    }

    private String refused(String sql) {
        return rootMessage(assertThrows(RuntimeException.class, () -> jdbc.update(sql)));
    }

    @Test
    void whatAMatchPlaysIsAppendOnly() {
        for (String table : List.of("cards", "card_definitions", "pool_snapshots", "snapshot_publications")) {
            String update = switch (table) {
                case "cards" -> "update cards set suit = suit";
                case "card_definitions" -> "update card_definitions set atk = atk";
                case "pool_snapshots" -> "update pool_snapshots set version_label = version_label";
                default -> "update snapshot_publications set note = note";
            };
            assertTrue(refused(update).contains("immutable"), table + " cannot be updated");
            assertTrue(refused("delete from " + table).contains("immutable"), table + " cannot be deleted from");
        }
        assertEquals(150, count("cards"));
        assertEquals(150, count("card_definitions"));
    }

    @Test
    void whatIsOnlyCommercialOrTextualCanChange() {
        assertEquals(1, jdbc.update("update card_sets set status = 'CHIUSA'"));
        assertEquals(1, jdbc.update("update card_sets set status = 'IN_VENDITA'"));
        assertEquals(150, jdbc.update("update card_texts set flavour = flavour"));
        assertEquals(1, jdbc.update("update card_set_texts set name = name"));
        assertEquals(3, jdbc.update("update pack_types set price = price"));
        assertEquals(2, jdbc.update("update active_snapshots set activated_at = now()"));
        assertEquals(1, jdbc.update("delete from card_set_cards where card_id = (select id from cards where card_code = 'BASE-RES-7-SPI-SPE')"));
        assertEquals(149, count("card_set_cards"));
        assertEquals(150, count("cards"), "the card is still there");
    }

    @Test
    void aCardCannotExistWithoutItsItalianText() {
        String insertCard = "insert into cards (card_code, faction, kind) values ('NO-TEXT', 'RESISTENZA_UMANA', 'SPELL')";
        assertTrue(refused(insertCard).contains("no Italian text"));
        assertEquals(0, count("cards where card_code = 'NO-TEXT'"));

        // only another language is not enough
        TransactionTemplate tx = new TransactionTemplate(txManager);
        String message = rootMessage(assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> {
            jdbc.update(insertCard);
            jdbc.update("insert into card_texts (card_id, locale, name) select id, 'en', 'No text' from cards where card_code = 'NO-TEXT'");
        })));
        assertTrue(message.contains("no Italian text"), message);

        // card and text in one transaction, in either order of statements, is fine
        tx.executeWithoutResult(s -> {
            jdbc.update(insertCard);
            jdbc.update("insert into card_texts (card_id, locale, name) select id, 'it', 'Con testo' from cards where card_code = 'NO-TEXT'");
        });
        assertEquals(1, count("cards where card_code = 'NO-TEXT'"));
    }

    @Test
    void namesAreUniquePerLanguageIgnoringCase() {
        String taken = jdbc.queryForObject("select name from card_texts where locale = 'it' and card_id = "
                + "(select id from cards where card_code = 'BASE-RES-A-FRA-SPE')", String.class);
        String message = rootMessage(assertThrows(RuntimeException.class, () -> jdbc.update(
                "update card_texts set name = upper(?) where locale = 'it' and card_id = (select id from cards where card_code = 'BASE-RES-7-SPI-SPE')",
                taken)));
        assertTrue(message.contains("card_texts_name_uq"), message);
        // the same name in another language is fine
        assertEquals(1, jdbc.update("insert into card_texts (card_id, locale, name) "
                + "select id, 'en', ? from cards where card_code = 'BASE-RES-7-SPI-SPE'", taken));
    }

    @Test
    void oneDraftIsOpenAtATime() {
        jdbc.update("insert into users (username, email, password_hash, role, display_name, avatar) values ('adm', 'adm@quorma.local', 'x', 'ADMIN', 'adm', 'avatar-01')");
        String insertDraft = "insert into catalogue_drafts (source, created_by) select 'api', id from users where username = 'adm'";
        assertEquals(1, jdbc.update(insertDraft));
        assertTrue(refused(insertDraft).contains("catalogue_drafts_one_open_uq"));
    }
}
