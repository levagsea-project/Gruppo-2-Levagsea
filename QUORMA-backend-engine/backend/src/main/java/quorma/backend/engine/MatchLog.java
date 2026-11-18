package quorma.backend.engine;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.JsonNode;

/**
 * The engine's canonical MatchLog document. The engine owns its schema and canonical serialization;
 * the backend preserves the complete JSON tree without interpreting or reshaping it.
 */
public record MatchLog(JsonNode document) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public MatchLog {
        Objects.requireNonNull(document, "document");
        if (!document.isObject()) {
            throw new IllegalArgumentException("MatchLog must be a JSON object");
        }
    }

    /** Serialize this value as the MatchLog root object, without an extra `document` wrapper. */
    @Override
    @JsonValue
    public JsonNode document() {
        return document;
    }
}
