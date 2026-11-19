package quorma.backend.catalogue.publish;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The base set the backend ships with: {@code data/cards.json} and the {@code data/labels/cards.<locale>.json} files,
 * copied into the jar by Maven on every build (one copy in git, in {@code data/}), read here as a {@link Changeset}.
 */
@Component
public class BundledCatalogue {

    private static final String CARDS = "catalogue/cards.json";
    private static final String LABELS = "classpath*:catalogue/labels/cards.*.json";

    /** The bundled batch: cards and sets from cards.json, texts from every cards.&lt;locale&gt;.json. */
    public Changeset load() {
        try {
            ObjectNode root = (ObjectNode) read(new PathMatchingResourcePatternResolver().getResource("classpath:" + CARDS));
            ObjectNode texts = root.putObject("texts");
            Map<String, Resource> labelFiles = new TreeMap<>();
            for (Resource r : new PathMatchingResourcePatternResolver().getResources(LABELS)) {
                String name = r.getFilename();
                labelFiles.put(name.substring("cards.".length(), name.length() - ".json".length()), r);
            }
            for (Map.Entry<String, Resource> e : labelFiles.entrySet()) {
                texts.set(e.getKey(), read(e.getValue()));
            }
            return Changeset.parse(root);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the bundled catalogue (" + CARDS + " and cards.<locale>.json)", e);
        }
    }

    private static JsonNode read(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return DraftPlanner.MAPPER.readTree(in);
        }
    }
}
