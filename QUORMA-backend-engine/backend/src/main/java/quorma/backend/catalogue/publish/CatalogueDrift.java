package quorma.backend.catalogue.publish;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
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
import quorma.backend.catalogue.publish.Changeset.TextKey;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Compares a batch (in practice the file bundled with the backend) with what the database holds, and says what differs.
 * The database is the source of truth: a startup never changes a card that exists, so when an admin edited a card or
 * the bundled file changed after the card was created, the two differ until an explicit upsert import. Read-only.
 */
@Service
public class CatalogueDrift {

    /**
     * @param inSync        nothing of the batch is missing from the database and no card definition differs
     * @param missingCards  cards of the batch that are not in the active catalogue yet
     * @param changedCards  cards whose active definition differs from the batch's (an upsert import would change them)
     * @param extraCards    cards in the active catalogue that the batch does not have (added through the API)
     * @param missingLinks  {@code card:SET} pairs of the batch that are not linked yet
     * @param missingTexts  texts of the batch that the database does not have yet
     * @param differingTexts text keys that exist in both and read differently (an admin edited them; informational)
     */
    public record DriftReport(
            boolean inSync, List<String> missingCards, List<String> changedCards, List<String> extraCards,
            List<String> missingLinks, List<String> missingTexts, List<String> differingTexts) {
    }

    private final DraftPlanner planner;
    private final CardRepository cards;
    private final CardSetRepository sets;
    private final CardSetCardRepository links;
    private final CardTextRepository cardTexts;
    private final CardSetTextRepository setTexts;

    CatalogueDrift(DraftPlanner planner, CardRepository cards, CardSetRepository sets, CardSetCardRepository links,
                   CardTextRepository cardTexts, CardSetTextRepository setTexts) {
        this.planner = planner;
        this.cards = cards;
        this.sets = sets;
        this.links = links;
        this.cardTexts = cardTexts;
        this.setTexts = setTexts;
    }

    @Transactional(readOnly = true)
    public DriftReport compare(Changeset batch) {
        Map<String, ObjectNode> active = new TreeMap<>();
        for (Faction f : Faction.values()) {
            active.putAll(planner.activeEngineCards(f.name()));
        }
        Map<String, Card> cardByCode = new HashMap<>();
        cards.findAll().forEach(c -> cardByCode.put(c.getCardCode(), c));
        Map<String, CardSet> setByCode = new HashMap<>();
        sets.findAll().forEach(s -> setByCode.put(s.getCode(), s));

        TreeSet<String> missing = new TreeSet<>();
        TreeSet<String> changed = new TreeSet<>();
        TreeSet<String> missingLinks = new TreeSet<>();
        TreeSet<String> inBatch = new TreeSet<>();
        for (ObjectNode card : batch.cards()) {
            String code = card.get("id").asString();
            inBatch.add(code);
            ObjectNode engineCard = card.deepCopy();
            engineCard.remove("sets");
            ObjectNode current = active.get(code);
            if (current == null) {
                missing.add(code);
            } else if (!CanonicalJson.write(current).equals(CanonicalJson.write(engineCard))) {
                changed.add(code);
            }
            if (card.has("sets") && card.get("sets").isArray()) {
                for (JsonNode s : card.get("sets")) {
                    Card existing = cardByCode.get(code);
                    CardSet set = setByCode.get(s.asString());
                    if (existing == null || set == null || !links.existsById(new CardSetCard.Key(existing.getId(), set.getId()))) {
                        missingLinks.add(code + ":" + s.asString());
                    }
                }
            }
        }
        TreeSet<String> extra = new TreeSet<>(active.keySet());
        extra.removeAll(inBatch);

        TreeSet<String> missingTexts = new TreeSet<>();
        TreeSet<String> differing = new TreeSet<>();
        for (Map.Entry<String, Map<String, String>> locale : batch.texts().entrySet()) {
            for (Map.Entry<String, String> e : locale.getValue().entrySet()) {
                TextKey k = TextKey.of(e.getKey());
                String key = locale.getKey() + ":" + e.getKey();
                String stored = k.kind().equals("CARD") ? cardText(cardByCode.get(k.code()), locale.getKey(), k.field())
                        : setText(setByCode.get(k.code()), locale.getKey(), k.field());
                if (stored == null) {
                    missingTexts.add(key);
                } else if (!stored.equals(e.getValue())) {
                    differing.add(key);
                }
            }
        }
        boolean inSync = missing.isEmpty() && changed.isEmpty() && missingLinks.isEmpty() && missingTexts.isEmpty();
        return new DriftReport(inSync, new ArrayList<>(missing), new ArrayList<>(changed), new ArrayList<>(extra),
                new ArrayList<>(missingLinks), new ArrayList<>(missingTexts), new ArrayList<>(differing));
    }

    private String cardText(Card card, String locale, String field) {
        if (card == null) {
            return null;
        }
        CardText t = cardTexts.findById(new CardText.Key(card.getId(), locale)).orElse(null);
        return t == null ? null : field.equals("name") ? t.getName() : t.getFlavour();
    }

    private String setText(CardSet set, String locale, String field) {
        if (set == null) {
            return null;
        }
        CardSetText t = setTexts.findById(new CardSetText.Key(set.getId(), locale)).orElse(null);
        return t == null ? null : field.equals("name") ? t.getName() : t.getDescription();
    }
}
