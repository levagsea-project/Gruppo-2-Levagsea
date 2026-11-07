package quorma.gameengine.data;

import java.util.Map;
import java.util.Optional;

/**
 * Everything loaded from {@code data/}: vocabulary, keywords and the raw balance tree.
 * Immutable. Rebalancing {@code balance.json} changes {@link #tune} results with no code change.
 */
public record Ruleset(Vocab vocab, Map<String, KeywordDef> keywords, Value.MapV balance) {

    /**
     * Resolves {@code $tune.<key>.<name>}: card instance params first, then {@code balance.keywords[key].params},
     * then the top level of that balance entry (data/AGENTS.md section 3). Looks in keywords, then evolutions.
     */
    public Optional<Value> tune(String key, String name, Map<String, Value> cardParams) {
        if (cardParams != null && cardParams.containsKey(name)) {
            return Optional.of(cardParams.get(name));
        }
        for (String section : new String[] {"keywords", "evolutions"}) {
            if (balance.get(section) instanceof Value.MapV entries && entries.get(key) instanceof Value.MapV entry) {
                if (entry.get("params") instanceof Value.MapV p && p.get(name) != null) {
                    return Optional.of(p.get(name));
                }
                if (entry.get(name) != null) {
                    return Optional.of(entry.get(name));
                }
            }
        }
        return Optional.empty();
    }

    /** Resolves {@code $tune.synergies.<TIER>.<name>}, for example ("VINCOLO_3", "x"). */
    public Optional<Value> synergyTune(String tier, String name) {
        if (balance.get("synergies") instanceof Value.MapV s && s.get(tier) instanceof Value.MapV t && t.get(name) != null) {
            return Optional.of(t.get(name));
        }
        return Optional.empty();
    }

    /**
     * Spill multiplier of a poker hand in per mille, from {@code balance.hand_multipliers_permille}. The key is
     * the {@code HandType} name (for example {@code "PAIR"}); the map is keyed by name so this package does
     * not depend on the hand package.
     *
     * @throws RulesetException if the entry is missing or is not an integer
     */
    public int handMultiplierPermille(String handTypeName) {
        if (balance.get("hand_multipliers_permille") instanceof Value.MapV m
                && m.get(handTypeName) instanceof Value.IntV n) {
            return Math.toIntExact(n.v());
        }
        throw new RulesetException(
                "balance.json#hand_multipliers_permille." + handTypeName + ": missing or not an integer");
    }

    /** A global number from {@code balance.globals}, for example {@code mitigation_floor_permille}. */
    public Optional<Value> global(String name) {
        if (balance.get("globals") instanceof Value.MapV g && g.get(name) != null) {
            return Optional.of(g.get(name));
        }
        return Optional.empty();
    }
}
