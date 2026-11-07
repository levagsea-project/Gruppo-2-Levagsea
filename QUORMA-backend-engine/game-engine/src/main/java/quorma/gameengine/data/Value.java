package quorma.gameengine.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A JSON value as the engine sees it. Integers only: a fraction is kept as exact text ({@link DecV})
 * and is never turned into a double. Strings starting with {@code $} become {@link Ref}.
 */
public sealed interface Value {

    record IntV(long v) implements Value {}

    /** A non-integer number, kept as text on purpose (no floating point in the engine). */
    record DecV(String text) implements Value {}

    record StrV(String v) implements Value {}

    record BoolV(boolean v) implements Value {}

    record NullV() implements Value {}

    record ListV(List<Value> items) implements Value {
        public ListV {
            items = List.copyOf(items);
        }
    }

    /** Insertion-ordered, immutable object. */
    record MapV(Map<String, Value> fields) implements Value {
        public MapV {
            fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }

        public Value get(String key) {
            return fields.get(key);
        }
    }

    enum RefKind { PARAM, TUNE }

    /** {@code $param.x} or {@code $tune.K_SCUDO.x}; {@code path} is what follows the first dot. */
    record Ref(RefKind kind, String path) implements Value {}
}
