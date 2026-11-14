package quorma.gameengine.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import quorma.gameengine.json.CanonicalJson;

/**
 * Builds a {@link Ruleset} from {@code vocab.json}, {@code keyword.json}, {@code subfactions.json} and
 * {@code balance.json}.
 *
 * <ul>
 *   <li>{@link #fromDirectory(Path)}: reads the live files, so a rebalance shows up on the next load (use {@code data/}).</li>
 *   <li>{@link #fromClasspath()}: reads the copy bundled in the jar under {@code ruleset/} (refreshed on every Maven build).</li>
 * </ul>
 */
public final class RulesetLoader {

    private static final List<String> FILES = List.of("vocab.json", "keyword.json", "subfactions.json", "balance.json");

    /** The files of the rule pack hash, in hash order (decision {@code 2026-11-14-match-log-schema}). */
    public static final List<String> HASHED_FILES = List.of(
            "vocab.json", "keyword.json", "evolutions.json", "synergies.json", "balance.json", "invalid-combinations.json");

    private RulesetLoader() {}

    public static Ruleset fromDirectory(Path dir) {
        return build(directory(dir));
    }

    public static Ruleset fromClasspath() {
        return build(classpath());
    }

    private static Source directory(Path dir) {
        return name -> {
            try {
                return Files.readAllBytes(dir.resolve(name));
            } catch (IOException e) {
                throw new RulesetException(name + ": cannot read from " + dir + ": " + e.getMessage(), e);
            }
        };
    }

    private static Source classpath() {
        return name -> {
            try (InputStream in = RulesetLoader.class.getResourceAsStream("/ruleset/" + name)) {
                if (in == null) {
                    throw new RulesetException(name + ": not found on the classpath under /ruleset/");
                }
                return in.readAllBytes();
            } catch (IOException e) {
                throw new RulesetException(name + ": cannot read: " + e.getMessage(), e);
            }
        };
    }

    /** {@link #rulesetHash} of the rule pack bundled in the jar. */
    public static String rulesetHashFromClasspath() {
        return rulesetHash(classpath());
    }

    /** {@link #rulesetHash} of the rule pack in {@code dir}, for example {@code data/}. */
    public static String rulesetHashFromDirectory(Path dir) {
        return rulesetHash(directory(dir));
    }

    /**
     * SHA-256 of the canonical JSON of an object whose keys are the {@link #HASHED_FILES} names, in that order, and
     * whose values are the parsed files. The MatchLog records it; a replay must match it (ruleset 12.6).
     */
    private static String rulesetHash(Source source) {
        Map<String, Object> pack = new LinkedHashMap<>();
        for (String f : HASHED_FILES) {
            pack.put(f, Value.toJson(JsonReader.parse(source.read(f), f)));
        }
        return CanonicalJson.sha256(pack);
    }

    private interface Source {
        byte[] read(String fileName);
    }

    private static Ruleset build(Source source) {
        Map<String, Value.MapV> roots = new LinkedHashMap<>();
        for (String f : FILES) {
            roots.put(f, asMap(JsonReader.parse(source.read(f), f), f));
        }
        Value.MapV vocab = roots.get("vocab.json");
        return new Ruleset(
                new Vocab(
                        section(vocab, "effects"), section(vocab, "triggers"), section(vocab, "targets"),
                        section(vocab, "conditions"), section(vocab, "durations")),
                keywords(roots.get("keyword.json")),
                subfactions(roots.get("subfactions.json")),
                roots.get("balance.json"));
    }

    // ---- vocab ----

    private static Map<String, VocabEntry> section(Value.MapV vocab, String name) {
        String path = "vocab.json." + name;
        Value.MapV entries = asMap(require(vocab, name, "vocab.json"), path);
        Map<String, VocabEntry> out = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : entries.fields().entrySet()) {
            String p = path + "." + e.getKey();
            Value.MapV m = asMap(e.getValue(), p);
            out.put(e.getKey(), new VocabEntry(
                    e.getKey(), optString(m, "name"), optString(m, "description"), classes(m.get("class")), m));
        }
        return out;
    }

    private static List<String> classes(Value v) {
        List<String> out = new ArrayList<>();
        if (v instanceof Value.StrV s) {
            out.add(s.v());
        } else if (v instanceof Value.ListV l) {
            for (Value item : l.items()) {
                if (item instanceof Value.StrV s) {
                    out.add(s.v());
                }
            }
        }
        return out;
    }

    // ---- keywords ----

    private static Map<String, KeywordDef> keywords(Value.MapV root) {
        Map<String, KeywordDef> out = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : root.fields().entrySet()) {
            if (e.getKey().startsWith("_")) {
                continue;
            }
            String id = e.getKey();
            String path = "keyword.json#" + id;
            Value.MapV k = asMap(e.getValue(), path);

            Map<String, ParamSpec> params = new LinkedHashMap<>();
            if (k.get("params") != null) {
                for (Map.Entry<String, Value> p : asMap(k.get("params"), path + ".params").fields().entrySet()) {
                    params.put(p.getKey(), paramSpec(asMap(p.getValue(), path + ".params." + p.getKey())));
                }
            }

            List<ExpansionDef> expansions = new ArrayList<>();
            List<Value> raw = asList(require(k, "expands_to", path), path + ".expands_to");
            for (int i = 0; i < raw.size(); i++) {
                expansions.add(expansion(asMap(raw.get(i), path + ".expands_to[" + i + "]"),
                        path + ".expands_to[" + i + "]"));
            }

            out.put(id, new KeywordDef(
                    id, optString(k, "name"), resClass(requireString(k, "class", path), path + ".class"),
                    optString(k, "balance_ref"), Map.copyOf(params), optString(k, "description"),
                    List.copyOf(expansions)));
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    private static ParamSpec paramSpec(Value.MapV m) {
        boolean required = m.get("required") instanceof Value.BoolV b && b.v();
        List<Value> values = m.get("values") instanceof Value.ListV l ? l.items() : List.of();
        return new ParamSpec(optString(m, "type"), required, m.get("default"), values);
    }

    /**
     * Reads one expansion as written in the data files and in a card's inline abilities. Keys other than the
     * expansion's own and the common params are ignored: the caller checks them if it needs to.
     *
     * @param path names the expansion in error messages
     * @throws RulesetException naming the path of the problem
     */
    public static ExpansionDef expansion(Value.MapV m, String path) {
        List<ConditionDef> conditions = new ArrayList<>();
        if (m.get("conditions") != null) {
            List<Value> raw = asList(m.get("conditions"), path + ".conditions");
            for (int i = 0; i < raw.size(); i++) {
                Value.MapV c = asMap(raw.get(i), path + ".conditions[" + i + "]");
                conditions.add(new ConditionDef(
                        requireString(c, "id", path + ".conditions[" + i + "]"), params(c.get("params"))));
            }
        }
        Map<String, Value> common = new LinkedHashMap<>();
        for (Map.Entry<String, Value> field : m.fields().entrySet()) {
            if (ExpansionDef.COMMON_KEYS.contains(field.getKey())) {
                common.put(field.getKey(), field.getValue());
            }
        }
        return new ExpansionDef(
                requireString(m, "trigger_id", path), params(m.get("trigger_params")),
                requireString(m, "effect_id", path), params(m.get("effect_params")),
                requireString(m, "target_id", path), params(m.get("target_params")),
                requireString(m, "duration", path),
                List.copyOf(conditions),
                resClass(requireString(m, "class", path), path + ".class"),
                common);
    }

    // ---- subfactions ----

    /** Reads {@code subfactions.json}: the {@code subfactions} section, keyed by name, in file order. */
    private static Map<String, SubfactionDef> subfactions(Value.MapV root) {
        String file = "subfactions.json";
        Value.MapV entries = asMap(require(root, "subfactions", file), file + "#subfactions");
        Map<String, SubfactionDef> out = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : entries.fields().entrySet()) {
            if (e.getKey().startsWith("_")) {
                continue;
            }
            String path = file + "#subfactions." + e.getKey();
            Value.MapV m = asMap(e.getValue(), path);
            out.put(e.getKey(), new SubfactionDef(
                    e.getKey(), requireString(m, "faction", path), requireString(m, "cycle", path),
                    requireString(m, "strong_against", path), requireString(m, "weak_against", path)));
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    // ---- helpers ----

    private static Map<String, Value> params(Value v) {
        if (v == null) {
            return Map.of();
        }
        return asMap(v, "params").fields();
    }

    private static ResClass resClass(String s, String path) {
        try {
            return ResClass.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new RulesetException(path + ": unknown resolution class '" + s + "'");
        }
    }

    private static Value require(Value.MapV m, String key, String path) {
        Value v = m.get(key);
        if (v == null) {
            throw new RulesetException(path + ": missing required field '" + key + "'");
        }
        return v;
    }

    private static String requireString(Value.MapV m, String key, String path) {
        if (require(m, key, path) instanceof Value.StrV s) {
            return s.v();
        }
        throw new RulesetException(path + "." + key + ": expected a string");
    }

    private static String optString(Value.MapV m, String key) {
        return m.get(key) instanceof Value.StrV s ? s.v() : null;
    }

    private static Value.MapV asMap(Value v, String path) {
        if (v instanceof Value.MapV m) {
            return m;
        }
        throw new RulesetException(path + ": expected an object");
    }

    private static List<Value> asList(Value v, String path) {
        if (v instanceof Value.ListV l) {
            return l.items();
        }
        throw new RulesetException(path + ": expected a list");
    }
}
