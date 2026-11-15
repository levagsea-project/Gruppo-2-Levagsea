package quorma.gameengine.json;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * The canonical JSON form of the MatchLog and of the rule pack hash (decision {@code 2026-11-14-match-log-schema}):
 * UTF-8, LF, 2-space indent, keys in insertion order, a trailing newline. Byte for byte what Python's
 * {@code json.dumps(value, ensure_ascii=False, indent=2) + "\n"} writes, so either side can recompute a hash.
 *
 * <p>Values: {@link Map} with {@link String} keys (write it as a {@code LinkedHashMap} to fix the key order),
 * {@link List}, {@link String}, {@link Integer}, {@link Long}, {@link Boolean}, {@code null}, and {@link BigDecimal}
 * for the few decimals in the data files (the MatchLog itself has integers only).</p>
 */
public final class CanonicalJson {

    private CanonicalJson() {}

    /** The canonical text of {@code value}, trailing newline included. */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, 0, out);
        return out.append('\n').toString();
    }

    /** The canonical UTF-8 bytes of {@code value}. */
    public static byte[] bytes(Object value) {
        return write(value).getBytes(StandardCharsets.UTF_8);
    }

    /** SHA-256 of the canonical bytes of {@code value}, lower-case hex. */
    public static String sha256(Object value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes(value)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every Java runtime has SHA-256", e);
        }
    }

    private static void write(Object value, int depth, StringBuilder out) {
        switch (value) {
            case null -> out.append("null");
            case String s -> string(s, out);
            case Integer n -> out.append(n);
            case Long n -> out.append(n);
            case BigDecimal d -> out.append(d.toPlainString());
            case Boolean b -> out.append(b);
            case Map<?, ?> map -> object(map, depth, out);
            case List<?> list -> array(list, depth, out);
            default -> throw new IllegalArgumentException("not a JSON value: " + value.getClass().getName());
        }
    }

    private static void object(Map<?, ?> map, int depth, StringBuilder out) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key)) {
                throw new IllegalArgumentException("JSON object keys must be strings, got " + e.getKey());
            }
            out.append(first ? "" : ",");
            first = false;
            newline(depth + 1, out);
            string(key, out);
            out.append(": ");
            write(e.getValue(), depth + 1, out);
        }
        newline(depth, out);
        out.append('}');
    }

    private static void array(List<?> list, int depth, StringBuilder out) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
            out.append(i == 0 ? "" : ",");
            newline(depth + 1, out);
            write(list.get(i), depth + 1, out);
        }
        newline(depth, out);
        out.append(']');
    }

    private static void newline(int depth, StringBuilder out) {
        out.append('\n');
        out.append("  ".repeat(depth));
    }

    /** As Python with {@code ensure_ascii=False}: only the quote, the backslash and control characters are escaped. */
    private static void string(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
