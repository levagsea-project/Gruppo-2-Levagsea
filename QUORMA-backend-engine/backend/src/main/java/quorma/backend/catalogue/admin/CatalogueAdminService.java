package quorma.backend.catalogue.admin;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Card;
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
import quorma.backend.catalogue.publish.CatalogueException;
import quorma.backend.catalogue.publish.PackTypeSpec;
import quorma.backend.common.ErrorCodes;
import quorma.backend.pack.StoredPackType;
import quorma.backend.pack.StoredPackTypeRepository;
import tools.jackson.databind.JsonNode;

/**
 * The catalogue edits that are not a snapshot: texts, set status and dates, which cards are in which set, and pack
 * types. All of it is mutable and outside the snapshot hash, so none of it goes through a draft and none of it touches
 * a replay, a deck or a collection. Taking a card out of a set only removes the link: the card stays in the snapshot,
 * in decks and in collections.
 */
@Service
public class CatalogueAdminService {

    private static final Pattern LOCALE = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");

    public record SetView(String code, String status, String releaseDate, String closeDate, int cards,
                          Map<String, String> names) {
    }

    public record PackTypeView(String code, String set, long price, Map<String, Integer> slots, int supportSlots,
                               boolean featured) {
    }

    private final CardRepository cards;
    private final CardSetRepository sets;
    private final CardSetCardRepository links;
    private final CardTextRepository cardTexts;
    private final CardSetTextRepository setTexts;
    private final StoredPackTypeRepository packTypes;

    public CatalogueAdminService(CardRepository cards, CardSetRepository sets, CardSetCardRepository links,
                                 CardTextRepository cardTexts, CardSetTextRepository setTexts,
                                 StoredPackTypeRepository packTypes) {
        this.cards = cards;
        this.sets = sets;
        this.links = links;
        this.cardTexts = cardTexts;
        this.setTexts = setTexts;
        this.packTypes = packTypes;
    }

    // ---- texts ----

    /**
     * Writes the text of a card or a set in one language, replacing the one that is there (unlike a publish, which only
     * fills what is missing). Body: {@code {name, flavour}} for a card, {@code {name, description}} for a set. A flavour
     * or description left out of the body is kept as it is; {@code null} clears it.
     *
     * @param kind CARD or SET
     */
    @Transactional
    public void putText(String kind, String code, String locale, JsonNode body) {
        if (!LOCALE.matcher(locale).matches()) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_TEXT_INVALID, "locale must be a language tag such as it or pt-BR");
        }
        String name = string(body, "name");
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_TEXT_INVALID, "name is required, up to 100 characters");
        }
        name = name.strip();
        String extraField = kind.equals("CARD") ? "flavour" : "description";
        boolean hasExtra = body.has(extraField);
        String extra = string(body, extraField);
        if (hasExtra && !body.get(extraField).isNull() && (extra == null || extra.isBlank())) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_TEXT_INVALID, "a flavour or description is not blank when it is given");
        }
        if (kind.equals("CARD")) {
            Card card = cards.findByCardCode(code)
                    .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_CARD_NOT_FOUND));
            boolean taken = cardTexts.findByLocaleAndNameIgnoreCase(locale, name).stream()
                    .anyMatch(t -> !t.getCardId().equals(card.getId()));
            if (taken) {
                throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_TEXT_NAME_TAKEN);
            }
            CardText row = cardTexts.findById(new CardText.Key(card.getId(), locale)).orElse(null);
            if (row == null) {
                cardTexts.save(new CardText(card.getId(), locale, name, extra));
            } else {
                row.setName(name);
                if (hasExtra) {
                    row.setFlavour(extra);
                }
                cardTexts.save(row);
            }
        } else if (kind.equals("SET")) {
            CardSet set = sets.findByCode(code)
                    .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_SET_NOT_FOUND));
            CardSetText row = setTexts.findById(new CardSetText.Key(set.getId(), locale)).orElse(null);
            if (row == null) {
                setTexts.save(new CardSetText(set.getId(), locale, name, extra));
            } else {
                row.setName(name);
                if (hasExtra) {
                    row.setDescription(extra);
                }
                setTexts.save(row);
            }
        } else {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_TEXT_INVALID, "kind is CARD or SET");
        }
    }

    // ---- sets ----

    @Transactional(readOnly = true)
    public List<SetView> listSets() {
        return sets.findAll().stream().sorted((a, b) -> a.getCode().compareTo(b.getCode())).map(this::view).toList();
    }

    /** Changes the status and dates of an existing set. Body: {@code {status, release_date, close_date}}, each optional. */
    @Transactional
    public SetView updateSet(String code, JsonNode body) {
        CardSet set = sets.findByCode(code)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_SET_NOT_FOUND));
        try {
            if (string(body, "status") != null) {
                set.setStatus(CardSetStatus.valueOf(string(body, "status")));
            }
            if (body.has("release_date")) {
                set.setReleaseDate(body.get("release_date").isNull() ? null : LocalDate.parse(string(body, "release_date")));
            }
            if (body.has("close_date")) {
                set.setCloseDate(body.get("close_date").isNull() ? null : LocalDate.parse(string(body, "close_date")));
            }
        } catch (IllegalArgumentException | DateTimeParseException | NullPointerException e) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_SET_INVALID,
                    "status is BOZZA, IN_VENDITA or CHIUSA; dates are yyyy-mm-dd or null");
        }
        if (set.getReleaseDate() != null && set.getCloseDate() != null && set.getCloseDate().isBefore(set.getReleaseDate())) {
            throw new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_SET_INVALID, "close_date is before release_date");
        }
        return view(sets.save(set));
    }

    /** Puts a card in a set (a reprint, or a card that was taken out). Nothing happens if it is already there. */
    @Transactional
    public void addCardToSet(String setCode, String cardCode) {
        CardSet set = sets.findByCode(setCode)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_SET_NOT_FOUND));
        Card card = cards.findByCardCode(cardCode)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_CARD_NOT_FOUND));
        CardSetCard.Key key = new CardSetCard.Key(card.getId(), set.getId());
        if (!links.existsById(key)) {
            links.save(new CardSetCard(card.getId(), set.getId()));
        }
    }

    /** Takes a card out of a set, so out of that set's packs. It stays in the snapshot, in decks and in collections. */
    @Transactional
    public void removeCardFromSet(String setCode, String cardCode) {
        CardSet set = sets.findByCode(setCode)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_SET_NOT_FOUND));
        Card card = cards.findByCardCode(cardCode)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_CARD_NOT_FOUND));
        CardSetCard.Key key = new CardSetCard.Key(card.getId(), set.getId());
        if (links.existsById(key)) {
            links.deleteById(key);
        }
    }

    // ---- pack types ----

    @Transactional(readOnly = true)
    public List<PackTypeView> listPackTypes() {
        Map<UUID, String> setCodes = new LinkedHashMap<>();
        sets.findAll().forEach(s -> setCodes.put(s.getId(), s.getCode()));
        return packTypes.findAll().stream().sorted((a, b) -> a.getCode().compareTo(b.getCode()))
                .map(t -> view(t, setCodes.get(t.getSetId()))).toList();
    }

    /**
     * Creates or updates a pack type: {@code {set, price, slots: {TIER_1..TIER_4: n}, support_slots, featured}}. The set
     * of a type that exists cannot change (create a new type instead). Whether the pool of the set can fill the slots
     * is checked by the engine at every publish, and the shop refuses to sell a pack it cannot fill.
     */
    @Transactional
    public PackTypeView putPackType(String code, JsonNode body) {
        PackTypeSpec spec = PackTypeSpec.parse(code, body);
        CardSet set = sets.findByCode(spec.setCode())
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_SET_NOT_FOUND));
        short t1 = (short) (int) spec.slots().get("TIER_1");
        short t2 = (short) (int) spec.slots().get("TIER_2");
        short t3 = (short) (int) spec.slots().get("TIER_3");
        short t4 = (short) (int) spec.slots().get("TIER_4");

        StoredPackType type = packTypes.findById(code).orElse(null);
        if (type == null) {
            type = new StoredPackType(code, set.getId(), spec.price(), t1, t2, t3, t4, (short) spec.support(), spec.featured());
        } else {
            if (!type.getSetId().equals(set.getId())) {
                throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_PACK_TYPE_CONFLICT,
                        "the set of a pack type cannot change: create a new type");
            }
            type.setPrice(spec.price());
            type.setSlotsTier1(t1);
            type.setSlotsTier2(t2);
            type.setSlotsTier3(t3);
            type.setSlotsTier4(t4);
            type.setSupportSlots((short) spec.support());
            type.setFeatured(spec.featured());
        }
        return view(packTypes.save(type), set.getCode());
    }

    /** Deletes a pack type that was never sold; 409 if packs of it were opened (they keep a reference to it). */
    @Transactional
    public void deletePackType(String code) {
        StoredPackType type = packTypes.findById(code)
                .orElseThrow(() -> new CatalogueException(HttpStatus.NOT_FOUND, ErrorCodes.CATALOGUE_PACK_TYPE_NOT_FOUND));
        try {
            packTypes.delete(type);
            packTypes.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CatalogueException(HttpStatus.CONFLICT, ErrorCodes.CATALOGUE_PACK_TYPE_CONFLICT,
                    "packs of this type were already opened");
        }
    }

    // ---- views ----

    private SetView view(CardSet set) {
        Map<String, String> names = new LinkedHashMap<>();
        setTexts.findAll().stream().filter(t -> t.getSetId().equals(set.getId()))
                .sorted((a, b) -> a.getLocale().compareTo(b.getLocale())).forEach(t -> names.put(t.getLocale(), t.getName()));
        return new SetView(set.getCode(), set.getStatus().name(),
                set.getReleaseDate() == null ? null : set.getReleaseDate().toString(),
                set.getCloseDate() == null ? null : set.getCloseDate().toString(),
                links.findBySetId(set.getId()).size(), names);
    }

    private static PackTypeView view(StoredPackType t, String setCode) {
        Map<String, Integer> slots = new LinkedHashMap<>();
        slots.put("TIER_1", (int) t.getSlotsTier1());
        slots.put("TIER_2", (int) t.getSlotsTier2());
        slots.put("TIER_3", (int) t.getSlotsTier3());
        slots.put("TIER_4", (int) t.getSlotsTier4());
        return new PackTypeView(t.getCode(), setCode, t.getPrice(), slots, t.getSupportSlots(), t.isFeatured());
    }

    private static String string(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v != null && v.isString() ? v.asString() : null;
    }
}
