package quorma.backend.catalogue.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import quorma.backend.catalogue.CatalogueDraft;
import quorma.backend.catalogue.DraftItem;
import quorma.backend.catalogue.DraftItemType;
import quorma.backend.catalogue.publish.BundledCatalogue;
import quorma.backend.catalogue.publish.CatalogueDrift;
import quorma.backend.catalogue.publish.CatalogueDrift.DriftReport;
import quorma.backend.catalogue.publish.CatalogueException;
import quorma.backend.catalogue.publish.CatalogueExporter;
import quorma.backend.catalogue.publish.CatalogueProperties;
import quorma.backend.catalogue.publish.CataloguePublisher;
import quorma.backend.catalogue.publish.CataloguePublisher.PublishResult;
import quorma.backend.catalogue.publish.Changeset;
import quorma.backend.catalogue.publish.ChangesetImporter;
import quorma.backend.catalogue.publish.ChangesetImporter.ImportResponse;
import quorma.backend.catalogue.publish.ChangesetImporter.Mode;
import quorma.backend.catalogue.publish.DraftReport;
import quorma.backend.catalogue.publish.DraftService;
import quorma.backend.common.ErrorCodes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The catalogue admin API (docs/plans/cards-into-game.md). Admins only. Every change to what a match plays goes through
 * the one draft (open, fill, preview, publish); texts, set status, set membership and pack types are edited directly,
 * because none of them is part of a snapshot. Errors carry a {@code code} (a key of the label files); a draft that
 * fails its check is a 422 {@code catalogue.draft_invalid} whose {@code report} lists every problem.
 */
@RestController
@RequestMapping("/admin/catalogue")
@Tag(name = "Catalogue admin", description = "Admins only. Drafts, publish, batch import and export, drift, texts, sets and pack types. "
        + "Errors carry a `code` (a key of the label files); `reason` is English for developers.")
public class AdminCatalogueController {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final DraftService drafts;
    private final CataloguePublisher publisher;
    private final ChangesetImporter importer;
    private final CatalogueExporter exporter;
    private final CatalogueDrift drift;
    private final BundledCatalogue bundled;
    private final CatalogueAdminService admin;
    private final CatalogueProperties props;

    public AdminCatalogueController(DraftService drafts, CataloguePublisher publisher, ChangesetImporter importer,
                                    CatalogueExporter exporter, CatalogueDrift drift, BundledCatalogue bundled,
                                    CatalogueAdminService admin, CatalogueProperties props) {
        this.drafts = drafts;
        this.publisher = publisher;
        this.importer = importer;
        this.exporter = exporter;
        this.drift = drift;
        this.bundled = bundled;
        this.admin = admin;
        this.props = props;
    }

    public record DraftView(UUID id, String status, String source, String note, UUID createdBy, Instant createdAt,
                            Instant closedAt) {
    }

    public record ItemView(String type, String key, JsonNode payload) {
    }

    public record DraftDetail(DraftView draft, List<ItemView> items) {
    }

    // ---- the draft ----

    @GetMapping("/draft")
    @Operation(summary = "The open draft", description = "404 `catalogue.draft_not_found` when none is open.")
    public DraftView currentDraft() {
        return view(drafts.current().orElseThrow(
                () -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_DRAFT_NOT_FOUND)));
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a draft", description = "Only one draft is open at a time (409 `catalogue.draft_open_exists`). "
            + "It remembers the snapshot each faction has active now.")
    public DraftView openDraft(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) JsonNode body) {
        String note = body != null && body.has("note") && body.get("note").isString() ? body.get("note").asString() : null;
        return view(drafts.open("api", note, UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/drafts/{id}")
    @Operation(summary = "A draft with its items")
    public DraftDetail draft(@PathVariable UUID id) {
        CatalogueDraft draft = drafts.get(id);
        return new DraftDetail(view(draft), drafts.items(id).stream().map(AdminCatalogueController::view).toList());
    }

    @DeleteMapping("/drafts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Discard an open draft")
    public void discard(@PathVariable UUID id) {
        drafts.discard(id);
    }

    @PutMapping("/drafts/{id}/cards/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Put a card in the draft", description = "The body is a card as `data/cards.json` writes it (engine fields plus "
            + "`sets`); its `id` must be the code. A card that exists keeps its identity; a changed definition becomes a new snapshot.")
    public void putCard(@PathVariable UUID id, @PathVariable String code, @RequestBody JsonNode card) {
        requireKey(card, "id", code);
        drafts.putItem(id, DraftItemType.CARD, card);
    }

    @PutMapping("/drafts/{id}/sets/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Put a new set in the draft", description = "Body `{status, release_date, close_date}`, all optional (BOZZA by default). "
            + "Created when the draft is published; an existing set is never changed this way (use PUT /sets/{code}).")
    public void putSet(@PathVariable UUID id, @PathVariable String code, @RequestBody(required = false) JsonNode body) {
        ObjectNode set = body != null && body.isObject() ? ((ObjectNode) body).deepCopy() : MAPPER.createObjectNode();
        set.put("code", code);
        drafts.putItem(id, DraftItemType.SET, set);
    }

    @PutMapping("/drafts/{id}/texts/{kind}/{code}/{locale}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Put a text in the draft", description = "`kind` is CARD or SET. Body `{name, flavour}` or `{name, description}`. "
            + "A publish writes it only if that text is missing; to replace a text use PUT /texts/{kind}/{code}/{locale}. "
            + "A new card and a new set need their Italian text.")
    public void putText(@PathVariable UUID id, @PathVariable String kind, @PathVariable String code,
                        @PathVariable String locale, @RequestBody JsonNode body) {
        ObjectNode text = body != null && body.isObject() ? ((ObjectNode) body).deepCopy() : MAPPER.createObjectNode();
        text.put("kind", kind);
        text.put("code", code);
        text.put("locale", locale);
        drafts.putItem(id, DraftItemType.TEXT, text);
    }

    @DeleteMapping("/drafts/{id}/items/{type}/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an item from the draft", description = "`type` is CARD, SET or TEXT; the key is the card id, the set code or "
            + "`kind:code:locale`. 404 `catalogue.item_invalid` if the draft has no such item.")
    public void removeItem(@PathVariable UUID id, @PathVariable DraftItemType type, @PathVariable String key) {
        if (!drafts.removeItem(id, type, key)) {
            throw new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_ITEM_INVALID);
        }
    }

    @GetMapping("/drafts/{id}/preview")
    @Operation(summary = "What the draft would do", description = "Checks it with the engine and lists problems, warnings, what changes in each "
            + "faction's pool, and the texts, sets and links that would be written. Writes nothing.")
    public DraftReport preview(@PathVariable UUID id) {
        return drafts.preview(id);
    }

    @PostMapping("/drafts/{id}/rebase")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Rebase the draft onto the snapshots active now", description = "Keeps its items. A publish refuses a draft whose base "
            + "moved (409 `catalogue.draft_stale`).")
    public void rebase(@PathVariable UUID id) {
        drafts.rebase(id);
    }

    @PostMapping("/drafts/{id}/publish")
    @Operation(summary = "Publish the draft", description = "One transaction under a lock: all or nothing, both factions together. 422 "
            + "`catalogue.draft_invalid` with the report if the draft has a problem. Decks are not touched: they move to the new snapshot "
            + "when their owner logs in.")
    public PublishResult publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @RequestBody(required = false) JsonNode body) {
        String note = body != null && body.has("note") && body.get("note").isString() ? body.get("note").asString() : null;
        return publisher.publish(id, UUID.fromString(jwt.getSubject()), note);
    }

    // ---- batch import, export, drift ----

    @PostMapping("/import")
    @Operation(summary = "Import a batch of cards", description = "The body is a batch in the format of `data/cards.json` (cards, set_definitions, "
            + "texts; see the export). `mode` is `add-only` (cards that exist are never touched; texts and sets are still filled in) or "
            + "`upsert` (changed cards become a new snapshot). `dryRun` defaults to true: it checks everything and keeps nothing; send "
            + "`dryRun=false` to import. All or nothing. 413 `catalogue.import_too_large`, 400 `catalogue.import_invalid`, 422 "
            + "`catalogue.draft_invalid` with the report.")
    public ImportResponse importBatch(@AuthenticationPrincipal Jwt jwt,
                                      @Parameter(description = "add-only or upsert") @RequestParam(defaultValue = "add-only") String mode,
                                      @RequestParam(defaultValue = "true") boolean dryRun,
                                      @RequestBody String body) {
        if (body.getBytes(StandardCharsets.UTF_8).length > props.maxImportBytes().toBytes()) {
            throw new CatalogueException(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCodes.CATALOGUE_IMPORT_TOO_LARGE);
        }
        Mode m = switch (mode) {
            case "add-only" -> Mode.ADD_ONLY;
            case "upsert" -> Mode.UPSERT;
            default -> throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_IMPORT_INVALID, "mode is add-only or upsert");
        };
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (RuntimeException e) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_IMPORT_INVALID, "the body is not JSON");
        }
        return importer.run(Changeset.parse(root), m, dryRun, "batch", UUID.fromString(jwt.getSubject()), null);
    }

    @GetMapping("/export")
    @Operation(summary = "Export the active catalogue", description = "The cards of the active snapshots with their sets, every text, and the sets, "
            + "in the format the import reads: export, edit outside, import again with mode upsert.")
    public ObjectNode export() {
        return exporter.export();
    }

    @GetMapping("/drift")
    @Operation(summary = "Compare the file bundled with the backend to the database", description = "A startup never changes a card that exists, "
            + "so the two differ when an admin edited a card or the bundled file changed after the card was created.")
    public DriftReport drift() {
        return drift.compare(bundled.load());
    }

    // ---- texts, sets, pack types: not part of a snapshot, edited directly ----

    @PutMapping("/texts/{kind}/{code}/{locale}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Replace a text", description = "`kind` is CARD or SET. Body `{name, flavour}` or `{name, description}`. Takes effect "
            + "at once and changes no snapshot. 409 `catalogue.text_name_taken` if another card has that name in that language.")
    public void putTextNow(@PathVariable String kind, @PathVariable String code, @PathVariable String locale,
                           @RequestBody JsonNode body) {
        admin.putText(kind, code, locale, body);
    }

    @GetMapping("/sets")
    @Operation(summary = "The sets", description = "With status, dates, number of cards and names by language.")
    public List<CatalogueAdminService.SetView> sets() {
        return admin.listSets();
    }

    @PutMapping("/sets/{code}")
    @Operation(summary = "Change the status and dates of a set", description = "Body `{status, release_date, close_date}`, each optional. "
            + "BOZZA is not sold, IN_VENDITA is, CHIUSA is not (its cards stay in the game).")
    public CatalogueAdminService.SetView updateSet(@PathVariable String code, @RequestBody JsonNode body) {
        return admin.updateSet(code, body);
    }

    @PutMapping("/sets/{code}/cards/{cardCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Put a card in a set")
    public void addToSet(@PathVariable String code, @PathVariable String cardCode) {
        admin.addCardToSet(code, cardCode);
    }

    @DeleteMapping("/sets/{code}/cards/{cardCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Take a card out of a set", description = "Out of that set's packs only: the card stays in the snapshot, in decks and in collections.")
    public void removeFromSet(@PathVariable String code, @PathVariable String cardCode) {
        admin.removeCardFromSet(code, cardCode);
    }

    @GetMapping("/pack-types")
    @Operation(summary = "The pack types", description = "Each has exactly one set.")
    public List<CatalogueAdminService.PackTypeView> packTypes() {
        return admin.listPackTypes();
    }

    @PutMapping("/pack-types/{code}")
    @Operation(summary = "Create or update a pack type", description = "Body `{set, price, slots: {TIER_1..TIER_4: n}, support_slots, featured}`. "
            + "The set of an existing type cannot change (409 `catalogue.pack_type_conflict`).")
    public CatalogueAdminService.PackTypeView putPackType(@PathVariable String code, @RequestBody JsonNode body) {
        return admin.putPackType(code, body);
    }

    @DeleteMapping("/pack-types/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a pack type that was never sold", description = "409 `catalogue.pack_type_conflict` once packs of it were opened.")
    public void deletePackType(@PathVariable String code) {
        admin.deletePackType(code);
    }

    // ---- helpers ----

    private static void requireKey(JsonNode body, String field, String expected) {
        if (body == null || !body.isObject() || !body.has(field) || !expected.equals(body.get(field).asString(null))) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_ITEM_INVALID, field + " must be " + expected);
        }
    }

    private static DraftView view(CatalogueDraft d) {
        return new DraftView(d.getId(), d.getStatus().name(), d.getSource(), d.getNote(), d.getCreatedBy(), d.getCreatedAt(),
                d.getClosedAt());
    }

    private static ItemView view(DraftItem item) {
        return new ItemView(item.getItemType().name(), item.getItemKey(), MAPPER.readTree(item.getPayload()));
    }
}
