package quorma.backend.catalogue.publish;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.DraftItemType;
import quorma.backend.catalogue.DraftStatus;
import quorma.backend.catalogue.publish.Changeset.SetDefinition;
import quorma.backend.catalogue.publish.Changeset.TextKey;
import quorma.backend.catalogue.publish.CataloguePublisher.PublishResult;
import quorma.backend.catalogue.publish.DraftService.ItemInput;
import quorma.backend.common.ErrorCodes;
import tools.jackson.databind.node.ObjectNode;

/**
 * Turns a {@link Changeset} into a draft and publishes it: the batch import, and the bootstrap from set files (an
 * add-only import by the system user). Nothing here writes a snapshot itself: it only fills the draft and calls
 * {@link CataloguePublisher}.
 *
 * <ul>
 *   <li>{@link Mode#ADD_ONLY}: only the cards that do not exist yet are put in the draft. A card that exists is never
 *       touched. Texts are put for every card (the publish writes only the rows that are missing, so a new language
 *       file fills in without replacing anything), and the sets the batch defines are created if missing.
 *   <li>{@link Mode#UPSERT}: every card of the batch is put in the draft; the ones that differ from the active
 *       definition become a new snapshot. A card's sets are only ever added (a card leaves a set through the admin API).
 * </ul>
 *
 * <p>Every run first checks the batch in a transaction that is rolled back (the draft, its items and the engine's
 * check happen, nothing is kept): a dry run stops there. A real run then refuses a batch with a problem, does nothing
 * when the batch changes nothing, and otherwise publishes it in one transaction: all or nothing. No draft is left
 * behind in any case.
 */
@Service
public class ChangesetImporter {

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetImporter.class);

    public enum Mode { ADD_ONLY, UPSERT }

    /**
     * @param report  the check of the draft (what it would do, or the problems)
     * @param result  what was published; null for a dry run (draftId null when the batch changed nothing)
     * @param cards   cards put in the draft
     * @param texts   text items put in the draft
     * @param sets    set items put in the draft
     */
    public record ImportResponse(Mode mode, boolean dryRun, DraftReport report, PublishResult result, int cards, int texts, int sets) {
    }

    private final DraftService drafts;
    private final CataloguePublisher publisher;
    private final CardRepository cards;
    private final TransactionTemplate tx;

    public ChangesetImporter(DraftService drafts, CataloguePublisher publisher, CardRepository cards,
                             PlatformTransactionManager txManager) {
        this.drafts = drafts;
        this.publisher = publisher;
        this.cards = cards;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * @param source {@code batch} for an admin's import, {@code seed} for the bootstrap from set files
     * @throws CatalogueException 409 {@code catalogue.draft_open_exists} if a draft is open; 422
     *                            {@code catalogue.draft_invalid} with the report if the draft has a problem (a real
     *                            run only: a dry run returns the report)
     */
    public ImportResponse run(Changeset changeset, Mode mode, boolean dryRun, String source, UUID actor, String note) {
        List<ItemInput> items = items(changeset, mode);
        int cardCount = (int) items.stream().filter(i -> i.type() == DraftItemType.CARD).count();
        int textCount = (int) items.stream().filter(i -> i.type() == DraftItemType.TEXT).count();
        int setCount = (int) items.stream().filter(i -> i.type() == DraftItemType.SET).count();

        // Every run first checks the batch in a transaction that is rolled back: the draft, its items and the engine's check
        // happen, nothing is kept. This is the validation outside the publish lock, and it leaves no draft behind when the
        // batch is wrong or changes nothing.
        ImportResponse preview = tx.execute(status -> {
            UUID draft = drafts.open(source, note, actor).getId();
            drafts.putItems(draft, items);
            DraftReport report = drafts.preview(draft);
            status.setRollbackOnly();
            return new ImportResponse(mode, dryRun, report, null, cardCount, textCount, setCount);
        });
        if (dryRun) {
            return preview;
        }
        DraftReport report = preview.report();
        if (!report.ok()) {
            throw new CatalogueException(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.CATALOGUE_DRAFT_INVALID, report);
        }
        if (changesNothing(report)) {
            List<CataloguePublisher.FactionResult> unchanged = report.factions().stream()
                    .map(f -> new CataloguePublisher.FactionResult(f.faction(), f.basedOn(), CataloguePublisher.Outcome.UNCHANGED))
                    .toList();
            return new ImportResponse(mode, false, report, new PublishResult(null, unchanged, 0, 0, 0, 0), cardCount,
                    textCount, setCount);
        }

        // The publish checks the draft again inside its lock: the database may have moved since the preview.
        UUID draft = drafts.open(source, note, actor).getId();
        try {
            drafts.putItems(draft, items);
            PublishResult result = publisher.publish(draft, actor, note);
            return new ImportResponse(mode, false, report, result, cardCount, textCount, setCount);
        } catch (RuntimeException e) {
            discardQuietly(draft);
            throw e;
        }
    }

    private static boolean changesNothing(DraftReport r) {
        return r.factions().stream().noneMatch(DraftReport.FactionPreview::touched) && r.cardsToCreate() == 0
                && r.textsToWrite() == 0 && r.setsToCreate() == 0 && r.linksToAdd() == 0;
    }

    private void discardQuietly(UUID draft) {
        try {
            if (drafts.get(draft).getStatus() == DraftStatus.OPEN) {
                drafts.discard(draft);
            }
        } catch (RuntimeException e) {
            LOG.warn("could not discard draft {} after a failed import", draft, e);
        }
    }

    // ---- changeset to draft items ----

    private List<ItemInput> items(Changeset cs, Mode mode) {
        Set<String> existing = cards.findAll().stream().map(Card::getCardCode).collect(Collectors.toSet());
        List<ItemInput> out = new ArrayList<>();

        // sets: the ones the batch defines, and any a card names (created as BOZZA if missing; an existing set is left alone)
        Map<String, SetDefinition> sets = new LinkedHashMap<>();
        cs.sets().forEach(s -> sets.put(s.code(), s));
        List<ObjectNode> chosen = new ArrayList<>();
        for (ObjectNode card : cs.cards()) {
            if (mode == Mode.UPSERT || !existing.contains(card.get("id").asString())) {
                chosen.add(card);
                if (card.has("sets") && card.get("sets").isArray()) {
                    card.get("sets").forEach(s -> {
                        if (s.isString()) {
                            sets.putIfAbsent(s.asString(), new SetDefinition(s.asString(), null, null, null));
                        }
                    });
                }
            }
        }
        for (SetDefinition s : sets.values()) {
            ObjectNode n = DraftPlanner.MAPPER.createObjectNode();
            n.put("code", s.code());
            if (s.status() != null) {
                n.put("status", s.status());
            }
            if (s.releaseDate() != null) {
                n.put("release_date", s.releaseDate());
            }
            if (s.closeDate() != null) {
                n.put("close_date", s.closeDate());
            }
            out.add(new ItemInput(DraftItemType.SET, n));
        }
        for (ObjectNode card : chosen) {
            out.add(new ItemInput(DraftItemType.CARD, card));
        }

        // texts: one item per language and card or set, from the flat label keys
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, Map<String, String>> locale : cs.texts().entrySet()) {
            Map<String, ObjectNode> byTarget = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : locale.getValue().entrySet()) {
                TextKey k = TextKey.of(e.getKey());
                ObjectNode n = byTarget.computeIfAbsent(k.kind() + ":" + k.code(), x -> {
                    ObjectNode t = DraftPlanner.MAPPER.createObjectNode();
                    t.put("kind", k.kind());
                    t.put("code", k.code());
                    t.put("locale", locale.getKey());
                    return t;
                });
                n.put(k.field(), e.getValue());
            }
            for (Map.Entry<String, ObjectNode> e : byTarget.entrySet()) {
                if (!e.getValue().has("name")) {
                    throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_IMPORT_INVALID,
                            "texts." + locale.getKey() + ": " + e.getKey() + " has a flavour or description but no name");
                }
                if (seen.add(locale.getKey() + "|" + e.getKey())) {
                    out.add(new ItemInput(DraftItemType.TEXT, e.getValue()));
                }
            }
        }
        return out;
    }
}
