package quorma.backend.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import quorma.backend.catalogue.DraftItemType;
import quorma.backend.catalogue.publish.DraftService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The 150 cards of the base set as a draft would hold them: the cards of {@code data/cards.json} (engine fields and
 * {@code sets}), their Italian texts from {@code data/labels/cards.it.json}, and the set BASE with its text.
 */
public final class BaseSet {

    public static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final List<JsonNode> CARDS = new ArrayList<>();
    private static final JsonNode LABELS;

    static {
        try {
            for (JsonNode c : MAPPER.readTree(Files.readString(Path.of("..", "data", "cards.json"), StandardCharsets.UTF_8)).get("cards")) {
                CARDS.add(c);
            }
            LABELS = MAPPER.readTree(Files.readString(Path.of("..", "data", "labels", "cards.it.json"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the base set from ../data", e);
        }
    }

    private BaseSet() {
    }

    /** A copy of every card of the base set. */
    public static List<ObjectNode> cards() {
        return CARDS.stream().map(c -> ((ObjectNode) c).deepCopy()).toList();
    }

    public static ObjectNode card(String id) {
        return cards().stream().filter(c -> c.get("id").asString().equals(id)).findFirst().orElseThrow();
    }

    /** The Italian text item of a card, as the draft holds it. */
    public static ObjectNode cardText(String code) {
        ObjectNode t = MAPPER.createObjectNode();
        t.put("kind", "CARD");
        t.put("code", code);
        t.put("locale", "it");
        t.put("name", LABELS.get("card." + code + ".name").asString());
        t.put("flavour", LABELS.get("card." + code + ".flavour").asString());
        return t;
    }

    /** Puts the whole base set into the draft: the set BASE and its text, every card and its Italian text. */
    public static void fill(DraftService drafts, UUID draftId) {
        ObjectNode set = MAPPER.createObjectNode();
        set.put("code", "BASE");
        drafts.putItem(draftId, DraftItemType.SET, set);
        ObjectNode setText = MAPPER.createObjectNode();
        setText.put("kind", "SET");
        setText.put("code", "BASE");
        setText.put("locale", "it");
        setText.put("name", LABELS.get("set.BASE.name").asString());
        setText.put("description", LABELS.get("set.BASE.description").asString());
        drafts.putItem(draftId, DraftItemType.TEXT, setText);
        for (ObjectNode card : cards()) {
            drafts.putItem(draftId, DraftItemType.CARD, card);
            drafts.putItem(draftId, DraftItemType.TEXT, cardText(card.get("id").asString()));
        }
    }
}
