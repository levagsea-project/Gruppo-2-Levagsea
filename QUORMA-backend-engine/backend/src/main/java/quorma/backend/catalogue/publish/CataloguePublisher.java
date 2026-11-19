package quorma.backend.catalogue.publish;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.ActiveSnapshot;
import quorma.backend.catalogue.ActiveSnapshotRepository;
import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardDefinition;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardKind;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.CardSet;
import quorma.backend.catalogue.CardSetCard;
import quorma.backend.catalogue.CardSetCardRepository;
import quorma.backend.catalogue.CardSetRepository;
import quorma.backend.catalogue.CardSetText;
import quorma.backend.catalogue.CardSetTextRepository;
import quorma.backend.catalogue.CardText;
import quorma.backend.catalogue.CardTextRepository;
import quorma.backend.catalogue.CatalogueDraft;
import quorma.backend.catalogue.CatalogueDraftRepository;
import quorma.backend.catalogue.DraftBase;
import quorma.backend.catalogue.DraftBaseRepository;
import quorma.backend.catalogue.DraftStatus;
import quorma.backend.catalogue.PoolSnapshot;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.catalogue.SnapshotPublication;
import quorma.backend.catalogue.SnapshotPublicationRepository;
import quorma.backend.catalogue.publish.DraftPlanner.FactionPlan;
import quorma.backend.catalogue.publish.DraftPlanner.LinkAdd;
import quorma.backend.catalogue.publish.DraftPlanner.NewCard;
import quorma.backend.catalogue.publish.DraftPlanner.NewSet;
import quorma.backend.catalogue.publish.DraftPlanner.Plan;
import quorma.backend.catalogue.publish.DraftPlanner.TextWrite;
import quorma.backend.common.ErrorCodes;
import tools.jackson.databind.JsonNode;

/**
 * Publishes the open draft: the only way a snapshot is written (docs/plans/cards-into-game.md).
 *
 * <p>Everything happens in one transaction, so a failure leaves nothing behind and both factions publish together or
 * not at all. The transaction first takes a database-wide lock on publishing, then refuses a draft whose base is no
 * longer the active snapshot of a faction, then plans the draft again against the database as it is now (the engine
 * checks the whole candidate pool of each touched faction), and only then writes: new sets, new cards, the texts that
 * are missing, one snapshot per touched faction with all its definitions (or an older snapshot with the same content
 * hash, brought back), the active snapshot, the card-to-set links and one audit row per snapshot. Decks are not
 * touched: they move to the active snapshot when their owner logs in ({@code DeckMigrator}).
 */
@Service
public class CataloguePublisher {

    /** Key of the advisory lock that serialises publishes ("QUORMA" in ASCII). */
    private static final long LOCK_KEY = 0x51554F524D41L;

    private static final DateTimeFormatter LABEL_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    /** What happened to one faction's pool. */
    public enum Outcome { NEW_SNAPSHOT, REACTIVATED, UNCHANGED }

    public record FactionResult(String faction, UUID snapshotId, Outcome outcome) {
    }

    public record PublishResult(
            UUID draftId, List<FactionResult> factions, int cardsCreated, int textsWritten, int setsCreated,
            int linksAdded) {
    }

    @PersistenceContext
    private EntityManager em;

    private final CatalogueDraftRepository drafts;
    private final DraftBaseRepository bases;
    private final DraftPlanner planner;
    private final ActiveSnapshotService activeService;
    private final ActiveSnapshotRepository activeSnapshots;
    private final PoolSnapshotRepository snapshots;
    private final CardDefinitionRepository definitions;
    private final CardRepository cards;
    private final CardTextRepository cardTexts;
    private final CardSetRepository sets;
    private final CardSetTextRepository setTexts;
    private final CardSetCardRepository links;
    private final SnapshotPublicationRepository publications;

    public CataloguePublisher(CatalogueDraftRepository drafts, DraftBaseRepository bases, DraftPlanner planner,
                              ActiveSnapshotService activeService, ActiveSnapshotRepository activeSnapshots,
                              PoolSnapshotRepository snapshots, CardDefinitionRepository definitions,
                              CardRepository cards, CardTextRepository cardTexts, CardSetRepository sets,
                              CardSetTextRepository setTexts, CardSetCardRepository links,
                              SnapshotPublicationRepository publications) {
        this.drafts = drafts;
        this.bases = bases;
        this.planner = planner;
        this.activeService = activeService;
        this.activeSnapshots = activeSnapshots;
        this.snapshots = snapshots;
        this.definitions = definitions;
        this.cards = cards;
        this.cardTexts = cardTexts;
        this.sets = sets;
        this.setTexts = setTexts;
        this.links = links;
        this.publications = publications;
    }

    /**
     * @param actor the user who publishes (a system user for the startup seed)
     * @throws CatalogueException 404 {@code catalogue.draft_not_found}, 409 {@code catalogue.draft_closed}, 409
     *                            {@code catalogue.draft_stale} (the active snapshot moved since the draft was opened:
     *                            rebase it), 422 {@code catalogue.draft_invalid} with the report of every problem
     */
    @Transactional
    public PublishResult publish(UUID draftId, UUID actor, String note) {
        em.createNativeQuery("select cast(pg_advisory_xact_lock(:key) as text)").setParameter("key", LOCK_KEY)
                .getSingleResult();

        CatalogueDraft draft = drafts.findById(draftId)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_DRAFT_NOT_FOUND));
        if (draft.getStatus() != DraftStatus.OPEN) {
            throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_DRAFT_CLOSED);
        }
        for (DraftBase base : bases.findByDraftId(draftId)) {
            if (!Objects.equals(base.getSnapshotId(), activeService.activeId(base.getFaction()).orElse(null))) {
                throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_DRAFT_STALE);
            }
        }

        Plan plan = planner.plan(draft);
        if (!plan.ok()) {
            throw new CatalogueException(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.CATALOGUE_DRAFT_INVALID, plan.report());
        }

        // sets, then cards, then texts (a card needs its Italian text by the end of the transaction)
        Map<String, CardSet> setByCode = new HashMap<>();
        sets.findAll().forEach(s -> setByCode.put(s.getCode(), s));
        for (NewSet s : plan.newSets()) {
            setByCode.put(s.code(), sets.save(new CardSet(s.code(), s.status(), s.releaseDate(), s.closeDate())));
        }
        sets.flush();
        Map<String, Card> cardByCode = new HashMap<>();
        cards.findAll().forEach(c -> cardByCode.put(c.getCardCode(), c));
        for (NewCard c : plan.newCards()) {
            cardByCode.put(c.code(), cards.save(new Card(c.code(), c.faction(), c.kind(), c.subfaction(), c.suit(), c.rank())));
        }
        cards.flush();
        for (TextWrite t : plan.textWrites()) {
            if (t.kind().equals("CARD")) {
                cardTexts.save(new CardText(cardByCode.get(t.code()).getId(), t.locale(), t.name(), t.extra()));
            } else {
                setTexts.save(new CardSetText(setByCode.get(t.code()).getId(), t.locale(), t.name(), t.extra()));
            }
        }
        cardTexts.flush();
        setTexts.flush();

        // one snapshot per faction the draft changes
        List<FactionResult> results = new ArrayList<>();
        for (FactionPlan f : plan.factions()) {
            if (!f.touched()) {
                results.add(new FactionResult(f.faction(), f.activeNow(), Outcome.UNCHANGED));
                continue;
            }
            UUID snapshotId;
            boolean reactivated = f.existing() != null;
            if (reactivated) {
                snapshotId = f.existing().getId();
            } else {
                snapshotId = insertSnapshot(f, cardByCode);
            }
            moveActive(f.faction(), snapshotId);
            publications.save(new SnapshotPublication(snapshotId, draftId, f.basedOn(), draft.getSource(), reactivated,
                    actor, note));
            results.add(new FactionResult(f.faction(), snapshotId, reactivated ? Outcome.REACTIVATED : Outcome.NEW_SNAPSHOT));
        }

        int linksAdded = 0;
        for (LinkAdd l : plan.links()) {
            CardSetCard.Key key = new CardSetCard.Key(cardByCode.get(l.cardCode()).getId(), setByCode.get(l.setCode()).getId());
            if (!links.existsById(key)) {
                links.save(new CardSetCard(key.cardId(), key.setId()));
                linksAdded++;
            }
        }

        draft.setStatus(DraftStatus.PUBLISHED);
        draft.setClosedAt(Instant.now());
        drafts.save(draft);
        em.flush();
        return new PublishResult(draftId, results, plan.newCards().size(), plan.textWrites().size(), plan.newSets().size(),
                linksAdded);
    }

    /** A new snapshot with the whole candidate pool, definitions inserted in this transaction (the sealing trigger). */
    private UUID insertSnapshot(FactionPlan f, Map<String, Card> cardByCode) {
        String label = LABEL_TIME.format(Instant.now()) + "-" + f.contentHash().substring(0, 8);
        PoolSnapshot snapshot = snapshots.saveAndFlush(new PoolSnapshot(f.faction(), label, f.rulesetVersion(), f.contentHash()));
        List<CardDefinition> defs = new ArrayList<>();
        f.candidate().forEach((code, node) -> {
            Card card = cardByCode.get(code);
            boolean creature = card.getKind() == CardKind.CREATURE;
            JsonNode abilities = node.get("abilities");
            defs.add(new CardDefinition(snapshot.getId(), card.getId(), f.faction(), card.getKind(),
                    creature ? node.get("atk").asInt() : null, creature ? node.get("def").asInt() : null,
                    DraftPlanner.MAPPER.writeValueAsString(abilities)));
        });
        definitions.saveAll(defs);
        definitions.flush();
        return snapshot.getId();
    }

    private void moveActive(String faction, UUID snapshotId) {
        ActiveSnapshot row = activeSnapshots.findById(faction).orElse(null);
        if (row == null) {
            activeSnapshots.saveAndFlush(new ActiveSnapshot(faction, snapshotId, Instant.now()));
        } else {
            row.setSnapshotId(snapshotId);
            row.setActivatedAt(Instant.now());
            activeSnapshots.saveAndFlush(row);
        }
    }
}
