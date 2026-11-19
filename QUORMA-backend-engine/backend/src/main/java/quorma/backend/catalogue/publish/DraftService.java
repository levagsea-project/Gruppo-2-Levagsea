package quorma.backend.catalogue.publish;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.CatalogueDraft;
import quorma.backend.catalogue.CatalogueDraftRepository;
import quorma.backend.catalogue.DraftBase;
import quorma.backend.catalogue.DraftBaseRepository;
import quorma.backend.catalogue.DraftItem;
import quorma.backend.catalogue.DraftItemRepository;
import quorma.backend.catalogue.DraftItemType;
import quorma.backend.catalogue.DraftStatus;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import tools.jackson.databind.JsonNode;

/**
 * The draft side of the publishing pipeline: open the one draft, fill it (the API, a batch file and the startup seed
 * all do this), preview what it would do, rebase it onto the current active snapshots, or discard it. Publishing is
 * {@link CataloguePublisher}. Nothing here writes a snapshot.
 *
 * <p>An item is identified inside the draft by its key, so putting it twice replaces it: a CARD by its id, a SET by
 * its code, a TEXT by {@code kind:code:locale}. The payloads are described in {@code docs/plans/cards-into-game.md}
 * and checked by {@link DraftPlanner}.
 */
@Service
public class DraftService {

    private static final Set<String> SOURCES = Set.of("seed", "api", "batch");

    private final CatalogueDraftRepository drafts;
    private final DraftBaseRepository bases;
    private final DraftItemRepository items;
    private final ActiveSnapshotService active;
    private final DraftPlanner planner;

    public DraftService(CatalogueDraftRepository drafts, DraftBaseRepository bases, DraftItemRepository items,
                        ActiveSnapshotService active, DraftPlanner planner) {
        this.drafts = drafts;
        this.bases = bases;
        this.items = items;
        this.active = active;
        this.planner = planner;
    }

    /**
     * Opens the draft, recording the snapshot each faction has active now (the draft's {@code based_on}).
     *
     * @param source {@code seed}, {@code api} or {@code batch}
     * @throws CatalogueException 409 {@code catalogue.draft_open_exists} if a draft is already open
     */
    @Transactional
    public CatalogueDraft open(String source, String note, UUID actor) {
        if (!SOURCES.contains(source)) {
            throw new IllegalArgumentException("source must be seed, api or batch, got " + source);
        }
        if (drafts.findFirstByStatus(DraftStatus.OPEN).isPresent()) {
            throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_DRAFT_OPEN_EXISTS);
        }
        CatalogueDraft draft;
        try {
            draft = drafts.saveAndFlush(new CatalogueDraft(source, note, actor));
        } catch (DataIntegrityViolationException e) {
            // Lost a race with another open: the unique index allows one open draft.
            throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_DRAFT_OPEN_EXISTS);
        }
        recordBases(draft.getId());
        return draft;
    }

    @Transactional(readOnly = true)
    public Optional<CatalogueDraft> current() {
        return drafts.findFirstByStatus(DraftStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public CatalogueDraft get(UUID draftId) {
        return drafts.findById(draftId)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_DRAFT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<DraftItem> items(UUID draftId) {
        get(draftId);
        return items.findByDraftId(draftId);
    }

    /** Adds the item, or replaces the one with the same key. 400 {@code catalogue.item_invalid} if it has no key. */
    @Transactional
    public void putItem(UUID draftId, DraftItemType type, JsonNode payload) {
        requireOpen(draftId);
        String key = itemKey(type, payload);
        String json = DraftPlanner.MAPPER.writeValueAsString(payload);
        Optional<DraftItem> existing = items.findByDraftIdAndItemTypeAndItemKey(draftId, type, key);
        if (existing.isPresent()) {
            existing.get().setPayload(json);
            items.save(existing.get());
        } else {
            items.save(new DraftItem(draftId, type, key, json));
        }
    }

    /** One item to put in a draft. */
    public record ItemInput(DraftItemType type, JsonNode payload) {
    }

    /**
     * Adds or replaces many items in one transaction (an import puts hundreds): all or none.
     *
     * @throws CatalogueException 400 {@code catalogue.item_invalid} if any item has no key
     */
    @Transactional
    public void putItems(UUID draftId, List<ItemInput> inputs) {
        requireOpen(draftId);
        Map<String, DraftItem> existing = new HashMap<>();
        for (DraftItem item : items.findByDraftId(draftId)) {
            existing.put(item.getItemType() + "|" + item.getItemKey(), item);
        }
        List<DraftItem> toSave = new ArrayList<>();
        for (ItemInput in : inputs) {
            String key = itemKey(in.type(), in.payload());
            String json = DraftPlanner.MAPPER.writeValueAsString(in.payload());
            DraftItem current = existing.get(in.type() + "|" + key);
            if (current != null) {
                current.setPayload(json);
                toSave.add(current);
            } else {
                DraftItem created = new DraftItem(draftId, in.type(), key, json);
                existing.put(in.type() + "|" + key, created);
                toSave.add(created);
            }
        }
        items.saveAll(toSave);
    }

    /** Removes an item; false if the draft had none with that key. */
    @Transactional
    public boolean removeItem(UUID draftId, DraftItemType type, String key) {
        requireOpen(draftId);
        Optional<DraftItem> existing = items.findByDraftIdAndItemTypeAndItemKey(draftId, type, key);
        existing.ifPresent(items::delete);
        return existing.isPresent();
    }

    @Transactional
    public void discard(UUID draftId) {
        CatalogueDraft draft = requireOpen(draftId);
        draft.setStatus(DraftStatus.DISCARDED);
        draft.setClosedAt(Instant.now());
        drafts.save(draft);
    }

    /**
     * Moves the draft's base to the snapshots that are active now, keeping its items. A publish refuses a draft whose
     * base is no longer the active snapshot (409 {@code catalogue.draft_stale}); this is how it is recomputed.
     */
    @Transactional
    public void rebase(UUID draftId) {
        requireOpen(draftId);
        bases.deleteByDraftId(draftId);
        bases.flush();
        recordBases(draftId);
    }

    /** What the draft would do, checked by the engine, with nothing written. */
    @Transactional(readOnly = true)
    public DraftReport preview(UUID draftId) {
        return planner.plan(get(draftId)).report();
    }

    private void recordBases(UUID draftId) {
        for (Faction faction : Faction.values()) {
            bases.save(new DraftBase(draftId, faction.name(), active.activeId(faction.name()).orElse(null)));
        }
    }

    private CatalogueDraft requireOpen(UUID draftId) {
        CatalogueDraft draft = get(draftId);
        if (draft.getStatus() != DraftStatus.OPEN) {
            throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_DRAFT_CLOSED);
        }
        return draft;
    }

    private static String itemKey(DraftItemType type, JsonNode payload) {
        String key = null;
        if (payload != null && payload.isObject()) {
            switch (type) {
                case CARD -> key = string(payload, "id");
                case SET -> key = string(payload, "code");
                case TEXT -> {
                    String kind = string(payload, "kind");
                    String code = string(payload, "code");
                    String locale = string(payload, "locale");
                    key = kind == null || code == null || locale == null ? null : kind + ":" + code + ":" + locale;
                }
            }
        }
        if (key == null || key.isBlank() || key.length() > 160) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_ITEM_INVALID);
        }
        return key;
    }

    private static String string(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && v.isString() ? v.asString() : null;
    }
}
