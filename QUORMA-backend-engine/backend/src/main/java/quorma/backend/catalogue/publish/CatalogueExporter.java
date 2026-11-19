package quorma.backend.catalogue.publish;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.CardSet;
import quorma.backend.catalogue.CardSetCard;
import quorma.backend.catalogue.CardSetCardRepository;
import quorma.backend.catalogue.CardSetRepository;
import quorma.backend.catalogue.CardSetText;
import quorma.backend.catalogue.CardSetTextRepository;
import quorma.backend.catalogue.CardText;
import quorma.backend.catalogue.CardTextRepository;
import quorma.backend.catalogue.Faction;
import quorma.backend.pack.StoredPackTypeRepository;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The active catalogue in the format of {@link Changeset}: export it, edit it outside, import it again (upsert). The
 * cards are the definitions of the active snapshot of each faction, with the sets each card belongs to; the texts are
 * every language there is; the sets carry their status and dates, and the pack types come with them.
 */
@Service
public class CatalogueExporter {

    private final DraftPlanner planner;
    private final CardRepository cards;
    private final CardSetRepository sets;
    private final CardSetCardRepository links;
    private final CardTextRepository cardTexts;
    private final CardSetTextRepository setTexts;
    private final StoredPackTypeRepository packTypes;

    CatalogueExporter(DraftPlanner planner, CardRepository cards, CardSetRepository sets, CardSetCardRepository links,
                      CardTextRepository cardTexts, CardSetTextRepository setTexts, StoredPackTypeRepository packTypes) {
        this.planner = planner;
        this.cards = cards;
        this.sets = sets;
        this.links = links;
        this.cardTexts = cardTexts;
        this.setTexts = setTexts;
        this.packTypes = packTypes;
    }

    @Transactional(readOnly = true)
    public ObjectNode export() {
        Map<UUID, Card> cardById = new HashMap<>();
        Map<String, Card> cardByCode = new HashMap<>();
        cards.findAll().forEach(c -> {
            cardById.put(c.getId(), c);
            cardByCode.put(c.getCardCode(), c);
        });
        Map<UUID, CardSet> setById = new HashMap<>();
        sets.findAll().forEach(s -> setById.put(s.getId(), s));
        Map<UUID, TreeSet<String>> setsOfCard = new HashMap<>();
        for (CardSetCard link : links.findAll()) {
            setsOfCard.computeIfAbsent(link.getCardId(), k -> new TreeSet<>()).add(setById.get(link.getSetId()).getCode());
        }

        ObjectNode root = DraftPlanner.MAPPER.createObjectNode();
        root.put("_readme", "Export of the active catalogue. Edit and import it again with mode upsert.");
        ArrayNode setArray = root.putArray("set_definitions");
        sets.findAll().stream().sorted((a, b) -> a.getCode().compareTo(b.getCode())).forEach(s -> {
            ObjectNode n = setArray.addObject();
            n.put("code", s.getCode());
            n.put("status", s.getStatus().name());
            if (s.getReleaseDate() != null) {
                n.put("release_date", s.getReleaseDate().toString());
            }
            if (s.getCloseDate() != null) {
                n.put("close_date", s.getCloseDate().toString());
            }
        });

        ArrayNode packArray = root.putArray("pack_types");
        packTypes.findAll().stream().sorted((a, b) -> a.getCode().compareTo(b.getCode())).forEach(t -> {
            ObjectNode n = packArray.addObject();
            n.put("code", t.getCode());
            n.put("set", setById.get(t.getSetId()).getCode());
            n.put("price", t.getPrice());
            ObjectNode slots = n.putObject("slots");
            if (t.getSlotsTier1() > 0) {
                slots.put("TIER_1", (int) t.getSlotsTier1());
            }
            if (t.getSlotsTier2() > 0) {
                slots.put("TIER_2", (int) t.getSlotsTier2());
            }
            if (t.getSlotsTier3() > 0) {
                slots.put("TIER_3", (int) t.getSlotsTier3());
            }
            if (t.getSlotsTier4() > 0) {
                slots.put("TIER_4", (int) t.getSlotsTier4());
            }
            n.put("support_slots", (int) t.getSupportSlots());
            n.put("featured", t.isFeatured());
        });

        Map<String, ObjectNode> active = new TreeMap<>();
        for (Faction f : Faction.values()) {
            active.putAll(planner.activeEngineCards(f.name()));
        }
        ArrayNode cardArray = root.putArray("cards");
        for (Map.Entry<String, ObjectNode> e : active.entrySet()) {
            ObjectNode card = e.getValue().deepCopy();
            ArrayNode cardSets = card.putArray("sets");
            TreeSet<String> codes = setsOfCard.get(cardByCode.get(e.getKey()).getId());
            if (codes != null) {
                codes.forEach(cardSets::add);
            }
            cardArray.add(card);
        }

        Map<String, Map<String, String>> texts = new TreeMap<>();
        for (CardText t : cardTexts.findAll()) {
            Map<String, String> table = texts.computeIfAbsent(t.getLocale(), k -> new TreeMap<>());
            String code = cardById.get(t.getCardId()).getCardCode();
            table.put("card." + code + ".name", t.getName());
            if (t.getFlavour() != null) {
                table.put("card." + code + ".flavour", t.getFlavour());
            }
        }
        for (CardSetText t : setTexts.findAll()) {
            Map<String, String> table = texts.computeIfAbsent(t.getLocale(), k -> new TreeMap<>());
            String code = setById.get(t.getSetId()).getCode();
            table.put("set." + code + ".name", t.getName());
            if (t.getDescription() != null) {
                table.put("set." + code + ".description", t.getDescription());
            }
        }
        ObjectNode textNode = root.putObject("texts");
        List<String> locales = new ArrayList<>(texts.keySet());
        for (String locale : locales) {
            ObjectNode table = textNode.putObject(locale);
            texts.get(locale).forEach(table::put);
        }
        return root;
    }
}
