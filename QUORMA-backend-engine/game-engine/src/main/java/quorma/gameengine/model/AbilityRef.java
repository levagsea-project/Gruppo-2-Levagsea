package quorma.gameengine.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import quorma.gameengine.data.Value;

/**
 * A reference to a keyword or an evolution by id, with the card's own parameters. The same shape as the card
 * instance in {@code data/AGENTS.md} section 4: {@code {"id": "K_SCUDO", "params": {"x": 15}}}.
 *
 * <p>The id is looked up in {@code keyword.json} or {@code evolutions.json} (ids do not overlap: {@code K_}
 * and {@code EVO_}). The params fill the {@code $param.x} references of the definition.
 *
 * @param id     keyword or evolution id
 * @param params the card's values for the definition's params, in file order. May be empty
 */
public record AbilityRef(String id, Map<String, Value> params) implements Ability {

    public AbilityRef {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("ability id must not be blank");
        }
        params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /** A reference with no parameters. */
    public AbilityRef(String id) {
        this(id, Map.of());
    }
}
