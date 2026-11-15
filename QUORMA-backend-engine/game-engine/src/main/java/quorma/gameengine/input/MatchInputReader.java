package quorma.gameengine.input;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.JsonReader;
import quorma.gameengine.data.RulesetException;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;

/**
 * Reads the match input JSON ({@code schemas/match-input.schema.json}) and enforces what the schema says: no
 * unknown key, the types, the codes and the ranges. What a schema cannot check (cards of the player's faction,
 * unique ids, playable abilities) is checked by {@code GameEngine}. Every error is a {@link MatchInputException}
 * that starts with the JSON path.
 */
public final class MatchInputReader {

    private static final Pattern ABILITY_ID = Pattern.compile("^(K|EVO)_[A-Z0-9_]+$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final int MAX_DECK = 64;

    private static final Set<String> ROOT_KEYS = Set.of("schema_version", "seed", "ruleset_version", "ruleset_hash", "players");
    private static final Set<String> PLAYER_KEYS = Set.of("faction", "pool_snapshot_id", "deck");
    private static final Set<String> CREATURE_KEYS = Set.of("id", "kind", "rank", "suit", "subfaction", "atk", "def", "abilities");
    private static final Set<String> OTHER_CARD_KEYS = Set.of("id", "kind", "faction", "abilities");
    private static final Set<String> REF_KEYS = Set.of("id", "params");
    private static final Set<String> EXPANSION_KEYS = union(
            Set.of("trigger_id", "trigger_params", "effect_id", "effect_params", "target_id", "target_params",
                    "duration", "conditions", "class"),
            ExpansionDef.COMMON_KEYS);

    private MatchInputReader() {}

    public static MatchInput read(String json) {
        return read(json.getBytes(StandardCharsets.UTF_8));
    }

    public static MatchInput read(byte[] utf8) {
        Value root;
        try {
            root = JsonReader.parse(utf8, "input");
        } catch (RulesetException e) {
            throw new MatchInputException(e.getMessage(), e);
        }
        Value.MapV m = object(root, "input");
        keys(m, "input", ROOT_KEYS, Set.of("schema_version", "seed", "ruleset_version", "players"));

        long schemaVersion = integer(m, "schema_version", "input");
        if (schemaVersion != MatchInput.SCHEMA_VERSION) {
            throw new MatchInputException("input.schema_version: this engine reads version "
                    + MatchInput.SCHEMA_VERSION + ", got " + schemaVersion);
        }
        long seed = integer(m, "seed", "input");
        String rulesetVersion = string(m, "ruleset_version", "input", 1, 32);
        Optional<String> rulesetHash = Optional.empty();
        if (m.get("ruleset_hash") != null) {
            String hash = string(m, "ruleset_hash", "input", 64, 64);
            if (!SHA256.matcher(hash).matches()) {
                throw new MatchInputException("input.ruleset_hash: must be 64 lower-case hex digits");
            }
            rulesetHash = Optional.of(hash);
        }

        Value.MapV players = object(m.get("players"), "input.players");
        keys(players, "input.players", Set.of("A", "B"), Set.of("A", "B"));
        Map<Side, MatchInput.Player> out = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            out.put(side, player(object(players.get(side.name()), "players." + side), "players." + side));
        }
        return new MatchInput((int) schemaVersion, seed, rulesetVersion, rulesetHash, out);
    }

    private static MatchInput.Player player(Value.MapV m, String path) {
        keys(m, path, PLAYER_KEYS, Set.of("faction", "deck"));
        Faction faction = code(Faction.class, m, "faction", path);
        Optional<String> snapshot = m.get("pool_snapshot_id") != null
                ? Optional.of(string(m, "pool_snapshot_id", path, 1, 64))
                : Optional.empty();
        List<Value> deck = list(m.get("deck"), path + ".deck");
        if (deck.isEmpty() || deck.size() > MAX_DECK) {
            throw new MatchInputException(path + ".deck: must have 1 to " + MAX_DECK + " cards, got " + deck.size());
        }
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < deck.size(); i++) {
            String cardPath = path + ".deck[" + i + "]";
            cards.add(card(object(deck.get(i), cardPath), cardPath));
        }
        return new MatchInput.Player(faction, snapshot, cards);
    }

    private static Card card(Value.MapV m, String path) {
        String kind = string(m, "kind", path, 1, 32);
        switch (kind) {
            case "CREATURE" -> {
                keys(m, path, CREATURE_KEYS, CREATURE_KEYS);
                return new CreatureCard(
                        string(m, "id", path, 1, 64),
                        (int) integer(m, "rank", path, 1, 13),
                        code(Suit.class, m, "suit", path),
                        code(Subfaction.class, m, "subfaction", path),
                        (int) integer(m, "atk", path, 0, Integer.MAX_VALUE),
                        (int) integer(m, "def", path, 1, Integer.MAX_VALUE),
                        abilities(m, path));
            }
            case "SPELL", "EQUIPMENT" -> {
                keys(m, path, OTHER_CARD_KEYS, OTHER_CARD_KEYS);
                String id = string(m, "id", path, 1, 64);
                Faction faction = code(Faction.class, m, "faction", path);
                List<Ability> abilities = abilities(m, path);
                return kind.equals("SPELL") ? new SpellCard(id, faction, abilities) : new EquipmentCard(id, faction, abilities);
            }
            default -> throw new MatchInputException(path + ".kind: must be CREATURE, SPELL or EQUIPMENT, got " + kind);
        }
    }

    private static List<Ability> abilities(Value.MapV card, String path) {
        List<Value> raw = list(card.get("abilities"), path + ".abilities");
        List<Ability> out = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            String p = path + ".abilities[" + i + "]";
            Value.MapV m = object(raw.get(i), p);
            out.add(m.get("expansions") != null ? inline(m, p) : ref(m, p));
        }
        return out;
    }

    private static AbilityRef ref(Value.MapV m, String path) {
        keys(m, path, REF_KEYS, Set.of("id"));
        String id = string(m, "id", path, 1, 64);
        if (!ABILITY_ID.matcher(id).matches()) {
            throw new MatchInputException(path + ".id: must look like K_NAME or EVO_NAME, got " + id);
        }
        Map<String, Value> params = new LinkedHashMap<>();
        if (m.get("params") != null) {
            for (Map.Entry<String, Value> e : object(m.get("params"), path + ".params").fields().entrySet()) {
                Value v = e.getValue();
                if (!(v instanceof Value.IntV || v instanceof Value.StrV || v instanceof Value.BoolV)) {
                    throw new MatchInputException(path + ".params." + e.getKey()
                            + ": must be an integer, a string or a boolean (no $ references on a card)");
                }
                params.put(e.getKey(), v);
            }
        }
        return new AbilityRef(id, params);
    }

    private static InlineEffect inline(Value.MapV m, String path) {
        keys(m, path, Set.of("expansions"), Set.of("expansions"));
        List<Value> raw = list(m.get("expansions"), path + ".expansions");
        if (raw.isEmpty()) {
            throw new MatchInputException(path + ".expansions: needs at least one expansion");
        }
        List<ExpansionDef> out = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            String p = path + ".expansions[" + i + "]";
            Value.MapV x = object(raw.get(i), p);
            keys(x, p, EXPANSION_KEYS, Set.of("trigger_id", "effect_id", "target_id", "duration", "class"));
            try {
                out.add(RulesetLoader.expansion(x, p));
            } catch (RulesetException e) {
                throw new MatchInputException(e.getMessage(), e);
            }
        }
        return new InlineEffect(out);
    }

    // ---- helpers: each names the path of the problem ----

    private static void keys(Value.MapV m, String path, Set<String> allowed, Set<String> required) {
        for (String key : m.fields().keySet()) {
            if (!allowed.contains(key)) {
                throw new MatchInputException(path + ": unknown key '" + key + "'");
            }
        }
        for (String key : required) {
            if (m.get(key) == null) {
                throw new MatchInputException(path + ": missing '" + key + "'");
            }
        }
    }

    private static Value.MapV object(Value v, String path) {
        if (v instanceof Value.MapV m) {
            return m;
        }
        throw new MatchInputException(path + ": must be an object");
    }

    private static List<Value> list(Value v, String path) {
        if (v instanceof Value.ListV l) {
            return l.items();
        }
        throw new MatchInputException(path + ": must be an array");
    }

    private static long integer(Value.MapV m, String key, String path) {
        if (m.get(key) instanceof Value.IntV n) {
            return n.v();
        }
        throw new MatchInputException(path + "." + key + ": must be an integer");
    }

    private static long integer(Value.MapV m, String key, String path, long min, long max) {
        long n = integer(m, key, path);
        if (n < min || n > max) {
            throw new MatchInputException(path + "." + key + ": must be " + min + ".." + max + ", got " + n);
        }
        return n;
    }

    private static String string(Value.MapV m, String key, String path, int minLength, int maxLength) {
        if (!(m.get(key) instanceof Value.StrV s)) {
            throw new MatchInputException(path + "." + key + ": must be a string");
        }
        if (s.v().length() < minLength || s.v().length() > maxLength) {
            throw new MatchInputException(path + "." + key + ": must be " + minLength + " to " + maxLength + " characters");
        }
        return s.v();
    }

    private static <E extends Enum<E>> E code(Class<E> type, Value.MapV m, String key, String path) {
        String s = string(m, key, path, 1, 64);
        for (E e : type.getEnumConstants()) {
            if (e.name().equals(s)) {
                return e;
            }
        }
        List<String> codes = new ArrayList<>();
        for (E e : type.getEnumConstants()) {
            codes.add(e.name());
        }
        throw new MatchInputException(path + "." + key + ": must be one of " + codes + ", got " + s);
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> out = new LinkedHashSet<>(a);
        out.addAll(b);
        return Set.copyOf(out);
    }
}
