package quorma.backend.catalogue.publish;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The canonical text of a JSON value, the same on every machine: object keys sorted, no whitespace, UTF-8, integers
 * only (the game has no floating point). Snapshot content hashes are SHA-256 of this text, so it must never depend on
 * the order a database or a client happened to give.
 */
final class CanonicalJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private CanonicalJson() {
    }

    static String write(JsonNode node) {
        StringBuilder out = new StringBuilder();
        append(node, out);
        return out.toString();
    }

    /** SHA-256 of the canonical UTF-8 bytes of {@code node}, lower-case hex. */
    static String sha256(JsonNode node) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(write(node).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every Java runtime has SHA-256", e);
        }
    }

    private static void append(JsonNode node, StringBuilder out) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>(node.propertyNames());
            Collections.sort(names);
            out.append('{');
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(MAPPER.writeValueAsString(names.get(i))).append(':');
                append(node.get(names.get(i)), out);
            }
            out.append('}');
        } else if (node.isArray()) {
            out.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                append(node.get(i), out);
            }
            out.append(']');
        } else {
            if (node.isNumber() && !node.isIntegralNumber()) {
                throw new IllegalArgumentException("canonical JSON has integers only, got " + node);
            }
            out.append(MAPPER.writeValueAsString(node));
        }
    }
}
