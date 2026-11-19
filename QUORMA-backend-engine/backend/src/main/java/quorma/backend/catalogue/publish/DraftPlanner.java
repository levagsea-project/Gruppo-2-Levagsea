package quorma.backend.catalogue.publish;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
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
import quorma.backend.catalogue.CardSetStatus;
import quorma.backend.catalogue.CardSetText;
import quorma.backend.catalogue.CardSetTextRepository;
import quorma.backend.catalogue.CardText;
import quorma.backend.catalogue.CardTextRepository;
import quorma.backend.catalogue.CatalogueDraft;
import quorma.backend.catalogue.DraftBase;
import quorma.backend.catalogue.DraftBaseRepository;
import quorma.backend.catalogue.DraftItem;
import quorma.backend.catalogue.DraftItemRepository;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.PoolSnapshot;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.catalogue.Subfaction;
import quorma.backend.catalogue.publish.DraftReport.FactionPreview;
import quorma.backend.catalogue.publish.DraftReport.Problem;
import quorma.backend.pack.StoredPackType;
import quorma.backend.pack.StoredPackTypeRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Works out what a draft does, without writing anything: reads the draft items and the database, builds the candidate
 * snapshot of each faction (the active definitions plus the draft's cards), asks the engine to check the whole
 * candidate, hashes it, and lists the texts, sets and links to write. {@link DraftService} shows the result as a
 * preview and {@link CataloguePublisher} applies it.
 *
 * <p>Rules (docs/plans/cards-into-game.md): a card change is a new snapshot, never an update; a snapshot's card pool
 * only grows (the candidate starts from the active one); a card keeps the identity it was created with (faction, kind,
 * subfaction, suit, rank); a new card needs its Italian text; texts are only written where missing; a new snapshot is
 * only built for a faction whose pool the draft changes.
 */
@Component
class DraftPlanner {

    static final JsonMapper MAPPER = JsonMapper.builder().build();

    static final String ITEM_INVALID = "draft.item_invalid";
    static final String CARD_INVALID = "draft.card_invalid";
    static final String CARD_IDENTITY_CHANGED = "draft.card_identity_changed";
    static final String CARD_NO_ITALIAN_TEXT = "draft.card_no_italian_text";
    static final String SET_NO_ITALIAN_TEXT = "draft.set_no_italian_text";
    static final String SET_UNKNOWN = "draft.set_unknown";
    static final String TEXT_UNKNOWN_TARGET = "draft.text_unknown_target";
    static final String TEXT_NAME_TAKEN = "draft.text_name_taken";
    static final String CARD_IN_NO_SET = "draft.card_in_no_set";

    private static final Pattern LOCALE = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");
    private static final Pattern SET_CODE = Pattern.compile("^[A-Z0-9_]{1,32}$");
    private static final String ITALIAN = "it";

    private final CardRepository cards;
    private final CardDefinitionRepository definitions;
    private final PoolSnapshotRepository snapshots;
    private final ActiveSnapshotService active;
    private final CardTextRepository cardTexts;
    private final CardSetRepository sets;
    private final CardSetTextRepository setTexts;
    private final CardSetCardRepository links;
    private final StoredPackTypeRepository packTypes;
    private final DraftItemRepository items;
    private final DraftBaseRepository bases;
    private final CardChecker checker;

    DraftPlanner(CardRepository cards, CardDefinitionRepository definitions, PoolSnapshotRepository snapshots,
                 ActiveSnapshotService active, CardTextRepository cardTexts, CardSetRepository sets,
                 CardSetTextRepository setTexts, CardSetCardRepository links, StoredPackTypeRepository packTypes,
                 DraftItemRepository items, DraftBaseRepository bases, CardChecker checker) {
        this.cards = cards;
        this.definitions = definitions;
        this.snapshots = snapshots;
        this.active = active;
        this.cardTexts = cardTexts;
        this.sets = sets;
        this.setTexts = setTexts;
        this.links = links;
        this.packTypes = packTypes;
        this.items = items;
        this.bases = bases;
        this.checker = checker;
    }

    // ---- the plan ----

    record NewSet(String code, CardSetStatus status, LocalDate releaseDate, LocalDate closeDate) {
    }

    record NewCard(String code, String faction, CardKind kind, Subfaction subfaction, String suit, Short rank) {
    }

    /** {@code kind} is CARD or SET; {@code extra} is the flavour of a card or the description of a set. */
    record TextWrite(String kind, String code, String locale, String name, String extra) {
    }

    record LinkAdd(String cardCode, String setCode) {
    }

    /**
     * @param candidate     the candidate snapshot: card code to engine card, sorted by code
     * @param changed       codes whose definition is new or differs from the active one
     * @param basedOn       the draft's recorded base for this faction (null: none yet)
     * @param activeNow     the active snapshot now (null: none yet)
     * @param existing      an older snapshot with the same content, to re-activate instead of inserting
     */
    record FactionPlan(
            String faction, Map<String, ObjectNode> candidate, Set<String> changed, int added, UUID basedOn,
            UUID activeNow, String contentHash, PoolSnapshot existing, String rulesetVersion) {

        boolean touched() {
            return !changed.isEmpty();
        }
    }

    record Plan(
            CatalogueDraft draft, List<Problem> problems, List<Problem> warnings, List<NewSet> newSets,
            List<NewCard> newCards, List<TextWrite> textWrites, List<LinkAdd> links, List<FactionPlan> factions) {

        boolean ok() {
            return problems.isEmpty();
        }

        DraftReport report() {
            List<FactionPreview> previews = new ArrayList<>();
            for (FactionPlan f : factions) {
                previews.add(new FactionPreview(f.faction(), f.touched(), f.basedOn(), f.added(),
                        f.changed().size() - f.added(), f.candidate().size() - f.changed().size(), f.candidate().size(),
                        f.touched() ? f.contentHash() : null, f.existing() != null));
            }
            return new DraftReport(draft.getId(), ok(), problems, warnings, previews, newCards.size(),
                    textWrites.size(), newSets.size(), links.size());
        }
    }

    private record CardItem(String code, String faction, CardKind kind, Subfaction subfaction, String suit, Short rank,
                            ObjectNode engineCard, List<String> sets) {
    }

    private record TextItem(String kind, String code, String locale, String name, String extra) {
    }

    Plan plan(CatalogueDraft draft) {
        List<Problem> problems = new ArrayList<>();
        List<Problem> warnings = new ArrayList<>();

        // 1. read the items
        Map<String, CardItem> cardItems = new TreeMap<>();
        Set<String> cardItemKeys = new LinkedHashSet<>();
        Map<String, NewSet> setItems = new TreeMap<>();
        List<TextItem> textItems = new ArrayList<>();
        List<DraftItem> all = new ArrayList<>(items.findByDraftId(draft.getId()));
        all.sort((a, b) -> (a.getItemType().name() + a.getItemKey()).compareTo(b.getItemType().name() + b.getItemKey()));
        for (DraftItem item : all) {
            JsonNode payload;
            try {
                payload = MAPPER.readTree(item.getPayload());
            } catch (RuntimeException e) {
                problems.add(new Problem(ITEM_INVALID, null, item.getItemKey(), "the payload is not JSON"));
                continue;
            }
            switch (item.getItemType()) {
                case CARD -> {
                    cardItemKeys.add(item.getItemKey());
                    CardItem card = parseCard(item, payload, problems);
                    if (card != null) {
                        cardItems.put(card.code(), card);
                    }
                }
                case SET -> {
                    NewSet set = parseSet(item, payload, problems);
                    if (set != null) {
                        setItems.put(set.code(), set);
                    }
                }
                case TEXT -> {
                    TextItem text = parseText(item, payload, problems);
                    if (text != null) {
                        textItems.add(text);
                    }
                }
            }
        }

        // 2. what exists
        Map<String, Card> existingCards = new HashMap<>();
        Map<UUID, Card> cardsById = new HashMap<>();
        for (Card c : cards.findAll()) {
            existingCards.put(c.getCardCode(), c);
            cardsById.put(c.getId(), c);
        }
        Map<String, CardSet> existingSets = new HashMap<>();
        for (CardSet s : sets.findAll()) {
            existingSets.put(s.getCode(), s);
        }

        // 3. sets: only the ones that do not exist yet are created; an existing set is left as it is
        List<NewSet> newSets = new ArrayList<>();
        for (NewSet s : setItems.values()) {
            if (!existingSets.containsKey(s.code())) {
                newSets.add(s);
            }
        }

        // 4. cards: identity, Italian text, sets
        Set<String> textKeysIt = new LinkedHashSet<>();
        for (TextItem t : textItems) {
            if (t.locale().equals(ITALIAN)) {
                textKeysIt.add(t.kind() + ":" + t.code());
            }
        }
        List<NewCard> newCards = new ArrayList<>();
        for (CardItem c : cardItems.values()) {
            Card existing = existingCards.get(c.code());
            if (existing == null) {
                newCards.add(new NewCard(c.code(), c.faction(), c.kind(), c.subfaction(), c.suit(), c.rank()));
                if (!textKeysIt.contains("CARD:" + c.code())) {
                    problems.add(new Problem(CARD_NO_ITALIAN_TEXT, c.faction(), c.code(),
                            "a new card needs its Italian text (a TEXT item with locale it)"));
                }
            } else if (!sameIdentity(existing, c)) {
                problems.add(new Problem(CARD_IDENTITY_CHANGED, c.faction(), c.code(), "card " + c.code()
                        + " already exists as " + describe(existing) + ": faction, kind, subfaction, suit and rank never change"));
            }
            for (String setCode : c.sets()) {
                if (!existingSets.containsKey(setCode) && !setItems.containsKey(setCode)) {
                    problems.add(new Problem(SET_UNKNOWN, c.faction(), c.code(), "card " + c.code() + " is in set "
                            + setCode + ", which does not exist and is not in the draft"));
                }
            }
        }
        for (NewSet s : newSets) {
            if (!textKeysIt.contains("SET:" + s.code())) {
                problems.add(new Problem(SET_NO_ITALIAN_TEXT, null, s.code(),
                        "a new set needs its Italian name (a TEXT item with locale it)"));
            }
        }

        // 5. texts: written only where missing, names unique per language
        List<TextWrite> textWrites = new ArrayList<>();
        Map<String, String> namesInDraft = new HashMap<>();
        for (TextItem t : textItems) {
            boolean isCard = t.kind().equals("CARD");
            Card card = isCard ? existingCards.get(t.code()) : null;
            CardSet set = isCard ? null : existingSets.get(t.code());
            boolean known = isCard
                    ? card != null || cardItemKeys.contains(t.code())
                    : set != null || setItems.containsKey(t.code());
            if (!known) {
                problems.add(new Problem(TEXT_UNKNOWN_TARGET, null, t.kind() + ":" + t.code(),
                        "text for " + t.kind() + " " + t.code() + ", which does not exist and is not in the draft"));
                continue;
            }
            boolean present = isCard
                    ? card != null && cardTexts.existsById(new CardText.Key(card.getId(), t.locale()))
                    : set != null && setTexts.existsById(new CardSetText.Key(set.getId(), t.locale()));
            if (present) {
                continue; // seed only what is missing: an existing text is never replaced
            }
            if (isCard) {
                String key = t.locale() + "|" + t.name().toLowerCase();
                String other = namesInDraft.putIfAbsent(key, t.code());
                boolean taken = !cardTexts.findByLocaleAndNameIgnoreCase(t.locale(), t.name()).isEmpty();
                if (taken || (other != null && !other.equals(t.code()))) {
                    problems.add(new Problem(TEXT_NAME_TAKEN, null, t.code(), "the name '" + t.name() + "' ("
                            + t.locale() + ") is already used by another card"));
                    continue;
                }
            }
            textWrites.add(new TextWrite(t.kind(), t.code(), t.locale(), t.name(), t.extra()));
        }

        // 6. links card to set: only missing ones are added
        List<LinkAdd> linkAdds = new ArrayList<>();
        Map<String, Set<String>> draftMembers = new HashMap<>();
        for (CardItem c : cardItems.values()) {
            Card existing = existingCards.get(c.code());
            boolean anyLink = false;
            for (String setCode : new LinkedHashSet<>(c.sets())) {
                draftMembers.computeIfAbsent(setCode, k -> new LinkedHashSet<>()).add(c.code());
                CardSet set = existingSets.get(setCode);
                if (set != null && existing != null && links.existsById(new CardSetCard.Key(existing.getId(), set.getId()))) {
                    anyLink = true;
                    continue;
                }
                linkAdds.add(new LinkAdd(c.code(), setCode));
                anyLink = true;
            }
            if (!anyLink && (existing == null || links.findByCardIdIn(List.of(existing.getId())).isEmpty())) {
                warnings.add(new Problem(CARD_IN_NO_SET, c.faction(), c.code(),
                        "card " + c.code() + " is in no set: it is in the game but in no pack"));
            }
        }

        // 7. the candidate snapshot of each faction, checked by the engine
        List<FactionPlan> factions = new ArrayList<>();
        Map<String, UUID> basedOn = new HashMap<>();
        for (DraftBase b : bases.findByDraftId(draft.getId())) {
            basedOn.put(b.getFaction(), b.getSnapshotId());
        }
        for (Faction faction : Faction.values()) {
            factions.add(planFaction(faction.name(), cardItems, cardsById, existingSets, draftMembers, basedOn.get(faction.name()),
                    problems));
        }
        return new Plan(draft, problems, warnings, newSets, newCards, textWrites, linkAdds, factions);
    }

    private FactionPlan planFaction(String faction, Map<String, CardItem> cardItems, Map<UUID, Card> cardsById,
                                    Map<String, CardSet> existingSets, Map<String, Set<String>> draftMembers,
                                    UUID basedOn, List<Problem> problems) {
        UUID activeNow = active.activeId(faction).orElse(null);
        Map<String, ObjectNode> activeCards = activeCards(activeNow, cardsById);
        Map<String, ObjectNode> candidate = new TreeMap<>(activeCards);
        Set<String> changed = new LinkedHashSet<>();
        int added = 0;
        for (CardItem c : cardItems.values()) {
            if (!c.faction().equals(faction)) {
                continue;
            }
            candidate.put(c.code(), c.engineCard());
            ObjectNode before = activeCards.get(c.code());
            if (before == null) {
                changed.add(c.code());
                added++;
            } else if (!CanonicalJson.write(before).equals(CanonicalJson.write(c.engineCard()))) {
                changed.add(c.code());
            }
        }
        if (changed.isEmpty()) {
            return new FactionPlan(faction, candidate, changed, 0, basedOn, activeNow, null, null, null);
        }

        ObjectNode content = MAPPER.createObjectNode();
        content.put("faction", faction);
        ArrayNode cardArray = content.putArray("cards");
        candidate.values().forEach(cardArray::add);
        String hash = CanonicalJson.sha256(content);
        PoolSnapshot existing = snapshots.findByContentHash(hash).orElse(null);

        JsonNode report = checker.check(engineRequest(faction, candidate, draftMembers, existingSets));
        for (JsonNode e : report.path("card_errors")) {
            problems.add(new Problem(e.path("code").asString(), faction, e.path("card_id").asString(null),
                    e.path("message").asString()));
        }
        for (JsonNode e : report.path("pool_errors")) {
            String subject = e.path("pool").isNull() ? e.path("subject").asString(null)
                    : e.path("pool").asString() + (e.path("subject").isNull() ? "" : ":" + e.path("subject").asString());
            problems.add(new Problem(e.path("code").asString(), faction, subject, e.path("message").asString()));
        }
        return new FactionPlan(faction, candidate, changed, added, basedOn, activeNow, hash, existing,
                report.path("ruleset_version").asString(null));
    }

    /** The cards of the active snapshot as the engine reads them (the snapshot hash is of exactly this shape). */
    private Map<String, ObjectNode> activeCards(UUID snapshotId, Map<UUID, Card> cardsById) {
        Map<String, ObjectNode> out = new TreeMap<>();
        if (snapshotId == null) {
            return out;
        }
        for (CardDefinition d : definitions.findBySnapshotId(snapshotId)) {
            Card c = cardsById.get(d.getCardId());
            ObjectNode n = MAPPER.createObjectNode();
            n.put("id", c.getCardCode());
            n.put("kind", c.getKind().name());
            if (c.getKind() == CardKind.CREATURE) {
                n.put("rank", c.getRank().intValue());
                n.put("suit", c.getSuit());
                n.put("subfaction", c.getSubfaction().name());
                n.put("atk", d.getAtk());
                n.put("def", d.getDef());
            } else {
                n.put("faction", c.getFaction());
            }
            n.set("abilities", MAPPER.readTree(d.getAbilities()));
            out.put(c.getCardCode(), n);
        }
        return out;
    }

    /** The engine request: the whole candidate, and one pack pool per pack type (its set's cards of this faction). */
    private ObjectNode engineRequest(String faction, Map<String, ObjectNode> candidate,
                                     Map<String, Set<String>> draftMembers, Map<String, CardSet> existingSets) {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("schema_version", 1);
        request.put("faction", faction);
        ArrayNode cardArray = request.putArray("cards");
        candidate.values().forEach(cardArray::add);
        List<StoredPackType> types = packTypes.findAll();
        if (!types.isEmpty()) {
            Map<UUID, String> setCodes = new HashMap<>();
            existingSets.values().forEach(s -> setCodes.put(s.getId(), s.getCode()));
            Map<UUID, String> codeById = new HashMap<>();
            cards.findAll().forEach(c -> codeById.put(c.getId(), c.getCardCode()));
            ArrayNode pools = request.putArray("pools");
            types.stream().sorted((a, b) -> a.getCode().compareTo(b.getCode())).forEach(type -> {
                Set<String> members = new LinkedHashSet<>();
                for (CardSetCard link : links.findBySetId(type.getSetId())) {
                    members.add(codeById.get(link.getCardId()));
                }
                members.addAll(draftMembers.getOrDefault(setCodes.get(type.getSetId()), Set.of()));
                ObjectNode pool = pools.addObject();
                pool.put("name", type.getCode());
                ArrayNode ids = pool.putArray("card_ids");
                members.stream().filter(candidate::containsKey).sorted().forEach(ids::add);
                ObjectNode slots = pool.putObject("tier_slots");
                if (type.getSlotsTier1() > 0) {
                    slots.put("TIER_1", (int) type.getSlotsTier1());
                }
                if (type.getSlotsTier2() > 0) {
                    slots.put("TIER_2", (int) type.getSlotsTier2());
                }
                if (type.getSlotsTier3() > 0) {
                    slots.put("TIER_3", (int) type.getSlotsTier3());
                }
                if (type.getSlotsTier4() > 0) {
                    slots.put("TIER_4", (int) type.getSlotsTier4());
                }
                pool.put("support_slots", (int) type.getSupportSlots());
            });
        }
        return request;
    }

    // ---- reading the items ----

    private CardItem parseCard(DraftItem item, JsonNode p, List<Problem> problems) {
        String code = text(p, "id");
        if (!p.isObject() || code == null || !code.equals(item.getItemKey())) {
            problems.add(new Problem(CARD_INVALID, null, item.getItemKey(), "a CARD item is a card object whose id is its key"));
            return null;
        }
        CardKind kind;
        try {
            kind = CardKind.valueOf(String.valueOf(text(p, "kind")));
        } catch (IllegalArgumentException e) {
            problems.add(new Problem(CARD_INVALID, null, code, "kind must be CREATURE, SPELL or EQUIPMENT"));
            return null;
        }
        String faction;
        Subfaction subfaction = null;
        String suit = null;
        Short rank = null;
        if (kind == CardKind.CREATURE) {
            try {
                subfaction = Subfaction.valueOf(String.valueOf(text(p, "subfaction")));
            } catch (IllegalArgumentException e) {
                problems.add(new Problem(CARD_INVALID, null, code, "unknown subfaction"));
                return null;
            }
            faction = subfaction.faction().name();
            suit = text(p, "suit");
            JsonNode r = p.get("rank");
            if (suit == null || r == null || !r.isIntegralNumber() || r.asInt() < 1 || r.asInt() > 13) {
                problems.add(new Problem(CARD_INVALID, faction, code, "a creature has a suit and a rank 1..13"));
                return null;
            }
            rank = (short) r.asInt();
        } else {
            faction = text(p, "faction");
            if (faction == null || java.util.Arrays.stream(Faction.values()).noneMatch(f -> f.name().equals(faction))) {
                problems.add(new Problem(CARD_INVALID, null, code, "a spell or equipment has a faction"));
                return null;
            }
        }
        List<String> setCodes = new ArrayList<>();
        JsonNode rawSets = p.get("sets");
        if (rawSets != null) {
            if (!rawSets.isArray()) {
                problems.add(new Problem(CARD_INVALID, faction, code, "sets must be a list of set codes"));
                return null;
            }
            for (JsonNode s : rawSets) {
                if (!s.isString() || !SET_CODE.matcher(s.asString()).matches()) {
                    problems.add(new Problem(CARD_INVALID, faction, code, "sets must be a list of set codes (A-Z, 0-9, _)"));
                    return null;
                }
                setCodes.add(s.asString());
            }
        }
        ObjectNode engineCard = ((ObjectNode) p).deepCopy();
        engineCard.remove("sets");
        return new CardItem(code, faction, kind, subfaction, suit, rank, engineCard, setCodes);
    }

    private NewSet parseSet(DraftItem item, JsonNode p, List<Problem> problems) {
        String code = text(p, "code");
        if (!p.isObject() || code == null || !code.equals(item.getItemKey()) || !SET_CODE.matcher(code).matches()) {
            problems.add(new Problem(ITEM_INVALID, null, item.getItemKey(), "a SET item has a code (A-Z, 0-9, _) that is its key"));
            return null;
        }
        try {
            CardSetStatus status = text(p, "status") == null ? CardSetStatus.BOZZA : CardSetStatus.valueOf(text(p, "status"));
            LocalDate release = text(p, "release_date") == null ? null : LocalDate.parse(text(p, "release_date"));
            LocalDate close = text(p, "close_date") == null ? null : LocalDate.parse(text(p, "close_date"));
            if (release != null && close != null && close.isBefore(release)) {
                problems.add(new Problem(ITEM_INVALID, null, code, "close_date is before release_date"));
                return null;
            }
            return new NewSet(code, status, release, close);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            problems.add(new Problem(ITEM_INVALID, null, code, "status is BOZZA, IN_VENDITA or CHIUSA; dates are yyyy-mm-dd"));
            return null;
        }
    }

    private TextItem parseText(DraftItem item, JsonNode p, List<Problem> problems) {
        String kind = text(p, "kind");
        String code = text(p, "code");
        String locale = text(p, "locale");
        String name = text(p, "name");
        String extra = text(p, kind != null && kind.equals("SET") ? "description" : "flavour");
        boolean shapeOk = p.isObject() && kind != null && (kind.equals("CARD") || kind.equals("SET")) && code != null
                && locale != null && LOCALE.matcher(locale).matches() && name != null && !name.isBlank()
                && name.length() <= 100 && (extra == null || !extra.isBlank());
        if (!shapeOk || !item.getItemKey().equals(kind + ":" + code + ":" + locale)) {
            problems.add(new Problem(ITEM_INVALID, null, item.getItemKey(),
                    "a TEXT item has kind CARD or SET, code, locale, a name up to 100 characters and optionally flavour or description"));
            return null;
        }
        return new TextItem(kind, code, locale, name.strip(), extra);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && v.isString() ? v.asString() : null;
    }

    private static boolean sameIdentity(Card existing, CardItem c) {
        return existing.getFaction().equals(c.faction()) && existing.getKind() == c.kind()
                && existing.getSubfaction() == c.subfaction()
                && java.util.Objects.equals(existing.getSuit(), c.suit())
                && java.util.Objects.equals(existing.getRank(), c.rank());
    }

    private static String describe(Card c) {
        return c.getKind() + " " + c.getFaction()
                + (c.getKind() == CardKind.CREATURE ? " " + c.getSubfaction() + " " + c.getSuit() + " " + c.getRank() : "");
    }
}
