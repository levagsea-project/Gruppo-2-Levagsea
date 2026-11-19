package quorma.backend.catalogue.publish;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import quorma.backend.common.ErrorCodes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A batch of catalogue changes, in the format of {@code data/cards.json}: the base set file, a file an admin edits and
 * re-imports, and the export of the active catalogue all look like this.
 *
 * <pre>
 * { "set_definitions": [ {"code": "BASE", "status": "IN_VENDITA"} ],       sets created if they do not exist yet
 *   "cards": [ {"id": "...", "sets": ["BASE"], "kind": "CREATURE", ...} ],  engine cards plus their sets
 *   "texts": { "it": { "card.&lt;id&gt;.name": "...", "card.&lt;id&gt;.flavour": "...",
 *                      "set.&lt;CODE&gt;.name": "...", "set.&lt;CODE&gt;.description": "..." } } }
 * </pre>
 *
 * Keys that start with an underscore ({@code _readme}) are ignored. {@code texts} is optional and may be partial for
 * every locale but Italian, which every new card and set needs. The {@code cards.<locale>.json} label files are the
 * {@code texts} of one locale.
 *
 * @param cards the cards as written, in file order
 * @param sets  the sets the batch defines
 * @param texts locale to label key to text
 */
public record Changeset(List<ObjectNode> cards, List<SetDefinition> sets, Map<String, Map<String, String>> texts) {

    /** Most cards one batch may carry (the engine's own limit). */
    public static final int MAX_CARDS = 5000;

    private static final Pattern LOCALE = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");
    private static final Pattern TEXT_KEY = Pattern.compile("^(card|set)\\.([A-Za-z0-9_-]{1,64})\\.(name|flavour|description)$");
    private static final Set<String> SET_KEYS = Set.of("code", "status", "release_date", "close_date");

    public record SetDefinition(String code, String status, String releaseDate, String closeDate) {
    }

    /**
     * @throws CatalogueException 400 {@code catalogue.import_invalid} if the batch is not in the format
     */
    public static Changeset parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw invalid("the batch must be a JSON object");
        }
        for (String key : root.propertyNames()) {
            if (!key.startsWith("_") && !Set.of("cards", "set_definitions", "texts").contains(key)) {
                throw invalid("unknown key '" + key + "' (a batch has cards, set_definitions and texts)");
            }
        }
        if (!root.has("cards") || !root.get("cards").isArray()) {
            throw invalid("cards must be an array");
        }
        List<ObjectNode> cards = new ArrayList<>();
        for (JsonNode c : root.get("cards")) {
            if (!c.isObject() || !c.has("id") || !c.get("id").isString() || c.get("id").asString().isBlank()) {
                throw invalid("every card is an object with an id");
            }
            cards.add((ObjectNode) c);
        }
        if (cards.size() > MAX_CARDS) {
            throw invalid("at most " + MAX_CARDS + " cards, got " + cards.size());
        }
        List<SetDefinition> sets = new ArrayList<>();
        if (root.has("set_definitions")) {
            if (!root.get("set_definitions").isArray()) {
                throw invalid("set_definitions must be an array");
            }
            for (JsonNode s : root.get("set_definitions")) {
                if (!s.isObject() || !s.has("code") || !s.get("code").isString()) {
                    throw invalid("every set definition is an object with a code");
                }
                for (String key : s.propertyNames()) {
                    if (!SET_KEYS.contains(key)) {
                        throw invalid("set " + s.get("code").asString() + ": unknown key '" + key + "'");
                    }
                }
                sets.add(new SetDefinition(s.get("code").asString(), text(s, "status"), text(s, "release_date"), text(s, "close_date")));
            }
        }
        Map<String, Map<String, String>> texts = new TreeMap<>();
        if (root.has("texts")) {
            if (!root.get("texts").isObject()) {
                throw invalid("texts must be an object of locales");
            }
            for (String locale : root.get("texts").propertyNames()) {
                JsonNode table = root.get("texts").get(locale);
                if (!LOCALE.matcher(locale).matches() || !table.isObject()) {
                    throw invalid("texts." + locale + " must be an object, and the locale a language tag such as it or pt-BR");
                }
                Map<String, String> entries = new LinkedHashMap<>();
                for (String key : table.propertyNames()) {
                    if (!TEXT_KEY.matcher(key).matches() || !table.get(key).isString() || table.get(key).asString().isBlank()) {
                        throw invalid("texts." + locale + "." + key + ": a key is card.<id>.name|flavour or set.<CODE>.name|description, "
                                + "and its text a non-empty string");
                    }
                    entries.put(key, table.get(key).asString());
                }
                texts.put(locale, entries);
            }
        }
        return new Changeset(cards, sets, texts);
    }

    /** One label file ({@code cards.<locale>.json}: a flat object of keys to texts) as the texts of {@code locale}. */
    public static Map<String, String> labelTable(JsonNode table) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : table.propertyNames()) {
            out.put(key, table.get(key).asString());
        }
        return out;
    }

    /** What the text key means: kind CARD or SET, the code, and the field (name, flavour or description). */
    record TextKey(String kind, String code, String field) {

        static TextKey of(String key) {
            var m = TEXT_KEY.matcher(key);
            if (!m.matches()) {
                throw invalid("text key '" + key + "' is not card.<id>.name|flavour or set.<CODE>.name|description");
            }
            String kind = m.group(1).equals("card") ? "CARD" : "SET";
            String field = m.group(3);
            if (kind.equals("CARD") ? field.equals("description") : field.equals("flavour")) {
                throw invalid("text key '" + key + "': a card has a name and a flavour, a set a name and a description");
            }
            return new TextKey(kind, m.group(2), field);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && v.isString() ? v.asString() : null;
    }

    private static CatalogueException invalid(String reason) {
        return new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_IMPORT_INVALID, reason);
    }
}
