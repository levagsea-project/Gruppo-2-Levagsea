package quorma.backend.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import quorma.backend.catalogue.DraftItemType;
import quorma.backend.catalogue.publish.DraftService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The base set as the tests use it: the set file {@code data/sets/BASE.json}, which holds the set definition, the 150
 * cards (engine fields and {@code sets}) and their Italian texts. The backend does not bundle it: tests read it from the
 * repository, as an admin would import it.
 */
public final class BaseSet {

    public static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** The folder of set files, as a bootstrap is pointed at it. */
    public static final Path SETS_DIR = Path.of("..", "data", "sets");

    private static final ObjectNode FILE;

    static {
        try {
            FILE = (ObjectNode) MAPPER.readTree(Files.readString(SETS_DIR.resolve("BASE.json"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read data/sets/BASE.json", e);
        }
    }

    private BaseSet() {
    }

    /** The whole base set as a batch (cards, set definition, Italian texts), a fresh copy. */
    public static ObjectNode batch() {
        return FILE.deepCopy();
    }

    /** A copy of every card of the base set. */
    public static List<ObjectNode> cards() {
        return java.util.stream.StreamSupport.stream(FILE.get("cards").spliterator(), false)
                .map(c -> ((ObjectNode) c).deepCopy()).toList();
    }

    public static ObjectNode card(String id) {
        return cards().stream().filter(c -> c.get("id").asString().equals(id)).findFirst().orElseThrow();
    }

    private static JsonNode italian() {
        return FILE.get("texts").get("it");
    }

    /** The Italian text item of a card, as the draft holds it. */
    public static ObjectNode cardText(String code) {
        ObjectNode t = MAPPER.createObjectNode();
        t.put("kind", "CARD");
        t.put("code", code);
        t.put("locale", "it");
        t.put("name", italian().get("card." + code + ".name").asString());
        t.put("flavour", italian().get("card." + code + ".flavour").asString());
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
        setText.put("name", italian().get("set.BASE.name").asString());
        setText.put("description", italian().get("set.BASE.description").asString());
        drafts.putItem(draftId, DraftItemType.TEXT, setText);
        for (ObjectNode card : cards()) {
            drafts.putItem(draftId, DraftItemType.CARD, card);
            drafts.putItem(draftId, DraftItemType.TEXT, cardText(card.get("id").asString()));
        }
    }
}
