package quorma.gameengine.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The only class that imports Jackson. Strict parse (duplicate keys and trailing content fail) into {@link Value}.
 * Used for the data files and for the match input.
 */
public final class JsonReader {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private JsonReader() {}

    /**
     * @param fileName names the document in error messages
     * @throws RulesetException naming the path of the problem
     */
    public static Value parse(byte[] utf8, String fileName) {
        JsonNode root;
        try {
            root = MAPPER.readTree(utf8);
        } catch (RuntimeException e) {
            throw new RulesetException(fileName + ": invalid JSON: " + e.getMessage(), e);
        }
        return convert(root, fileName);
    }

    private static Value convert(JsonNode n, String path) {
        if (n.isObject()) {
            Map<String, Value> fields = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> e : n.properties()) {
                fields.put(e.getKey(), convert(e.getValue(), path + "." + e.getKey()));
            }
            return new Value.MapV(fields);
        }
        if (n.isArray()) {
            List<Value> items = new ArrayList<>();
            int i = 0;
            for (JsonNode child : n) {
                items.add(convert(child, path + "[" + i++ + "]"));
            }
            return new Value.ListV(items);
        }
        if (n.isString()) {
            return parseString(n.asString(), path);
        }
        if (n.isIntegralNumber()) {
            if (!n.canConvertToLong()) {
                throw new RulesetException(path + ": integer out of range");
            }
            return new Value.IntV(n.longValue());
        }
        if (n.isNumber()) {
            return new Value.DecV(n.decimalValue().toPlainString());
        }
        if (n.isBoolean()) {
            return new Value.BoolV(n.booleanValue());
        }
        return new Value.NullV();
    }

    private static Value parseString(String s, String path) {
        if (!s.startsWith("$")) {
            return new Value.StrV(s);
        }
        int dot = s.indexOf('.');
        String head = dot < 0 ? s : s.substring(0, dot);
        String rest = dot < 0 ? "" : s.substring(dot + 1);
        return switch (head) {
            case "$param" -> new Value.Ref(Value.RefKind.PARAM, rest);
            case "$tune" -> new Value.Ref(Value.RefKind.TUNE, rest);
            default -> throw new RulesetException(path + ": unknown reference '" + s + "'");
        };
    }
}
