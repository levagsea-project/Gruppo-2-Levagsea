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
    record Ref(RefKind kind, String path) implements Value {

        /** The reference as written in JSON, for example {@code $tune.K_SCUDO.x}. */
        public String text() {
            return "$" + kind.name().toLowerCase(java.util.Locale.ROOT) + (path.isEmpty() ? "" : "." + path);
        }
    }

    /**
     * {@code value} as plain Java values for {@code CanonicalJson}: maps keep their order, a {@link DecV} becomes a
     * {@link java.math.BigDecimal} with its exact text, a {@link Ref} becomes its JSON text again.
     */
    static Object toJson(Value value) {
        return switch (value) {
            case IntV n -> n.v();
            case DecV d -> new java.math.BigDecimal(d.text());
            case StrV s -> s.v();
            case BoolV b -> b.v();
            case NullV n -> null;
            case Ref r -> r.text();
            case ListV l -> {
                List<Object> out = new java.util.ArrayList<>();
                for (Value item : l.items()) {
                    out.add(toJson(item));
                }
                yield out;
            }
            case MapV m -> {
                Map<String, Object> out = new LinkedHashMap<>();
                m.fields().forEach((k, v) -> out.put(k, toJson(v)));
                yield out;
            }
        };
    }
}
