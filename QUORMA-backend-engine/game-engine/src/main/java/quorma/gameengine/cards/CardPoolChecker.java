package quorma.gameengine.cards;

import static quorma.gameengine.cards.CardCheckCodes.CARD_BIND_FAILED;
import static quorma.gameengine.cards.CardCheckCodes.CARD_DUPLICATE_ABILITY;
import static quorma.gameengine.cards.CardCheckCodes.CARD_INVALID;
import static quorma.gameengine.cards.CardCheckCodes.CARD_RANK_NOT_ALLOWED;
import static quorma.gameengine.cards.CardCheckCodes.CARD_TOO_MANY_EVOLUTIONS;
import static quorma.gameengine.cards.CardCheckCodes.CARD_TOO_MANY_KEYWORDS;
import static quorma.gameengine.cards.CardCheckCodes.CARD_WRONG_FACTION;
import static quorma.gameengine.cards.CardCheckCodes.PACK_NO_CREATURE_FOR_TIER;
import static quorma.gameengine.cards.CardCheckCodes.PACK_NO_SUPPORT_CARD;
import static quorma.gameengine.cards.CardCheckCodes.PACK_UNKNOWN_CARD;
import static quorma.gameengine.cards.CardCheckCodes.POOL_DUPLICATE_CARD_ID;
import static quorma.gameengine.cards.CardCheckCodes.POOL_EMPTY;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import quorma.gameengine.cards.CardCheckReport.CardError;
import quorma.gameengine.cards.CardCheckReport.PoolError;
import quorma.gameengine.data.JsonReader;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.RulesetException;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.AbilityException;
import quorma.gameengine.input.MatchInputException;
import quorma.gameengine.input.MatchInputReader;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.Faction;

/**
 * Checks a candidate card pool of one faction against the rules this engine plays: the single authority on what a
 * card may be (decision of {@code docs/plans/cards-into-game.md}). {@code data/validate.py} stays as the CI check of
 * the data files and must agree with it.
 *
 * <p><b>Request</b> (JSON): {@code schema_version} 1, {@code faction}, {@code cards} (cards as a deck lists them,
 * without any catalogue field) and optionally {@code pools}: pack pools, each {@code {name, card_ids, tier_slots,
 * support_slots}}, where {@code tier_slots} maps {@code TIER_1}..{@code TIER_4} to a number of creatures. A broken
 * request (not JSON, an unknown key, a bad faction, too many cards) is a {@link MatchInputException}, as for a match.
 *
 * <p><b>Per card</b>, every problem is reported, not only the first: the card schema; the faction; no ability listed
 * twice; at most {@code balance.globals.card_ability_slots} keywords and evolutions; a creature's rank within the
 * {@code allowed_ranks} of each keyword and evolution it carries (this is what bounds {@code max_built_in}: a deck
 * has at most one creature per rank and suit, so the copies of an ability are at most 4 per allowed rank); and that
 * the engine can bind every ability (known id, required and allowed params, an effect that fits its target, trigger,
 * duration and class).
 *
 * <p><b>Per pool</b>: not empty; no id twice; and for each pack pool, every card it names is in the pool, every Tier
 * with slots has a creature, and support slots have a spell or an equipment (a pack draws with repetition, so one
 * card is enough). Balance is not checked.
 */
public final class CardPoolChecker {

    /** Most cards one request may carry. */
    static final int MAX_CARDS = 5000;

    /** Most pack pools one request may carry. */
    static final int MAX_PACK_POOLS = 64;

    private static final Set<String> ROOT_KEYS = Set.of("schema_version", "faction", "cards", "pools");
    private static final Set<String> POOL_KEYS = Set.of("name", "card_ids", "tier_slots", "support_slots");
    private static final Set<String> TIERS = Set.of("TIER_1", "TIER_2", "TIER_3", "TIER_4");

    private final Ruleset rules;
    private final String rulesetHash;
    private final String engineVersion;

    /**
     * @param rulesetHash   the hash of the rule pack {@code rules} was loaded from
     * @param engineVersion the engine version to name in the report
     */
    public CardPoolChecker(Ruleset rules, String rulesetHash, String engineVersion) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.rulesetHash = Objects.requireNonNull(rulesetHash, "rulesetHash");
        this.engineVersion = Objects.requireNonNull(engineVersion, "engineVersion");
    }

    /** One pack pool of the request. */
    private record PackPool(String name, List<String> cardIds, Map<String, Integer> tierSlots, int supportSlots) {
    }

    /**
     * @throws MatchInputException if the request itself is broken (a problem of a card is a report entry, not this)
     */
    public CardCheckReport check(String json) {
        Value root;
        try {
            root = JsonReader.parse(json.getBytes(StandardCharsets.UTF_8), "input");
        } catch (RulesetException e) {
            throw new MatchInputException(e.getMessage(), e);
        }
        Value.MapV m = object(root, "input");
        keys(m, "input", ROOT_KEYS, Set.of("schema_version", "faction", "cards"));
        long version = integer(m, "schema_version", "input");
        if (version != CardCheckReport.SCHEMA_VERSION) {
            throw new MatchInputException("input.schema_version: this engine reads version "
                    + CardCheckReport.SCHEMA_VERSION + ", got " + version);
        }
        Faction faction = faction(m);
        List<Value> rawCards = list(m.get("cards"), "input.cards");
        if (rawCards.size() > MAX_CARDS) {
            throw new MatchInputException("input.cards: at most " + MAX_CARDS + " cards, got " + rawCards.size());
        }
        List<PackPool> packPools = packPools(m.get("pools"));

        List<CardError> cardErrors = new ArrayList<>();
        List<PoolError> poolErrors = new ArrayList<>();
        AbilityBinder binder = new AbilityBinder(rules);
        Map<String, Card> pool = new LinkedHashMap<>();
        for (int i = 0; i < rawCards.size(); i++) {
            String path = "cards[" + i + "]";
            Value raw = rawCards.get(i);
            Card card;
            try {
                card = MatchInputReader.readCard(raw, path);
            } catch (MatchInputException e) {
                String rawId = raw instanceof Value.MapV rm && rm.get("id") instanceof Value.StrV s ? s.v() : null;
                cardErrors.add(new CardError(rawId, CARD_INVALID, path, e.getMessage()));
                continue;
            }
            if (pool.containsKey(card.id())) {
                poolErrors.add(new PoolError(null, POOL_DUPLICATE_CARD_ID, card.id(),
                        path + ".id: " + card.id() + " is already in the pool"));
                continue;
            }
            pool.put(card.id(), card);
            checkCard(card, path, faction, binder, cardErrors);
        }
        if (rawCards.isEmpty()) {
            poolErrors.add(new PoolError(null, POOL_EMPTY, null, "input.cards: the pool has no card"));
        }
        for (PackPool pack : packPools) {
            checkPack(pack, pool, poolErrors);
        }
        return new CardCheckReport(engineVersion, rules.rulesetVersion(), rulesetHash, cardErrors, poolErrors);
    }

    // ---- one card ----

    private void checkCard(Card card, String path, Faction faction, AbilityBinder binder, List<CardError> errors) {
        if (card.faction() != faction) {
            errors.add(new CardError(card.id(), CARD_WRONG_FACTION, path, path + ": card " + card.id() + " is "
                    + card.faction() + ", the pool is " + faction));
        }
        if (card instanceof CreatureCard creature) {
            checkAbilityRefs(creature, path, errors);
        }
        try {
            if (card instanceof CreatureCard creature) {
                binder.bind(creature);
            } else {
                binder.bindImmediate(card);
            }
        } catch (AbilityException e) {
            errors.add(new CardError(card.id(), CARD_BIND_FAILED, path, path + ".abilities: " + e.getMessage()));
        }
    }

    /** Duplicates, slots and rank eligibility of the keywords and evolutions a creature carries. */
    private void checkAbilityRefs(CreatureCard card, String path, List<CardError> errors) {
        Set<String> seen = new LinkedHashSet<>();
        int keywords = 0;
        int evolutions = 0;
        for (Ability ability : card.abilities()) {
            if (!(ability instanceof AbilityRef ref)) {
                continue;
            }
            if (!seen.add(ref.id())) {
                errors.add(new CardError(card.id(), CARD_DUPLICATE_ABILITY, path,
                        path + ".abilities: " + ref.id() + " is listed twice"));
            }
            if (ref.id().startsWith("EVO_")) {
                evolutions++;
            } else {
                keywords++;
            }
            Set<String> allowed = allowedRanks(ref.id());
            String rank = rankName(card.rank());
            if (allowed != null && !allowed.contains(rank)) {
                errors.add(new CardError(card.id(), CARD_RANK_NOT_ALLOWED, path, path + ".abilities: " + ref.id()
                        + " is not allowed on rank " + rank + " (allowed: " + String.join(", ", allowed) + ")"));
            }
        }
        int maxKeywords = slot("keyword");
        int maxEvolutions = slot("evolution");
        if (keywords > maxKeywords) {
            errors.add(new CardError(card.id(), CARD_TOO_MANY_KEYWORDS, path,
                    path + ".abilities: " + keywords + " keywords, at most " + maxKeywords));
        }
        if (evolutions > maxEvolutions) {
            errors.add(new CardError(card.id(), CARD_TOO_MANY_EVOLUTIONS, path,
                    path + ".abilities: " + evolutions + " evolutions, at most " + maxEvolutions));
        }
    }

    /** {@code balance.globals.card_ability_slots.<kind>}, or no limit if the ruleset has none. */
    private int slot(String kind) {
        if (rules.global("card_ability_slots").orElse(null) instanceof Value.MapV slots
                && slots.get(kind) instanceof Value.IntV n) {
            return (int) n.v();
        }
        return Integer.MAX_VALUE;
    }

    /** The {@code allowed_ranks} of a keyword or evolution, in file order; null if it has none or is unknown. */
    private Set<String> allowedRanks(String abilityId) {
        boolean evolution = abilityId.startsWith("EVO_");
        String ref = evolution
                ? (rules.evolutions().get(abilityId) == null ? null : rules.evolutions().get(abilityId).balanceRef())
                : (rules.keywords().get(abilityId) == null ? null : rules.keywords().get(abilityId).balanceRef());
        if (ref == null) {
            return null; // unknown ability: the bind reports it
        }
        if (rules.balance().get(evolution ? "evolutions" : "keywords") instanceof Value.MapV table
                && table.get(ref) instanceof Value.MapV entry
                && entry.get("allowed_ranks") instanceof Value.ListV ranks) {
            Set<String> out = new LinkedHashSet<>();
            for (Value v : ranks.items()) {
                if (v instanceof Value.StrV s) {
                    out.add(s.v());
                }
            }
            return out;
        }
        return null;
    }

    /** Rank 1..13 as the data files write it: A, 2..10, J, Q, K. */
    static String rankName(int rank) {
        return switch (rank) {
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> Integer.toString(rank);
        };
    }

    /** The Tier of a creature rank (ruleset 3.2.2): 2-6, 7-10, J/Q/K, Ace. */
    static String tierOf(int rank) {
        if (rank == 1) {
            return "TIER_4";
        }
        if (rank >= 11) {
            return "TIER_3";
        }
        return rank >= 7 ? "TIER_2" : "TIER_1";
    }

    // ---- one pack pool ----

    private static void checkPack(PackPool pack, Map<String, Card> pool, List<PoolError> errors) {
        Set<String> tiers = new TreeSet<>();
        boolean support = false;
        for (String id : pack.cardIds()) {
            Card card = pool.get(id);
            if (card == null) {
                errors.add(new PoolError(pack.name(), PACK_UNKNOWN_CARD, id,
                        "pool " + pack.name() + ": card " + id + " is not in the pool"));
            } else if (card instanceof CreatureCard creature) {
                tiers.add(tierOf(creature.rank()));
            } else {
                support = true;
            }
        }
        for (Map.Entry<String, Integer> slot : pack.tierSlots().entrySet()) {
            if (slot.getValue() > 0 && !tiers.contains(slot.getKey())) {
                errors.add(new PoolError(pack.name(), PACK_NO_CREATURE_FOR_TIER, slot.getKey(), "pool " + pack.name()
                        + ": " + slot.getValue() + " " + slot.getKey() + " slots and no creature of that Tier"));
            }
        }
        if (pack.supportSlots() > 0 && !support) {
            errors.add(new PoolError(pack.name(), PACK_NO_SUPPORT_CARD, null, "pool " + pack.name() + ": "
                    + pack.supportSlots() + " support slots and no spell or equipment"));
        }
    }

    // ---- the request: each problem names its path ----

    private static List<PackPool> packPools(Value raw) {
        if (raw == null) {
            return List.of();
        }
        List<Value> list = list(raw, "input.pools");
        if (list.size() > MAX_PACK_POOLS) {
            throw new MatchInputException("input.pools: at most " + MAX_PACK_POOLS + " pools, got " + list.size());
        }
        List<PackPool> out = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String path = "input.pools[" + i + "]";
            Value.MapV p = object(list.get(i), path);
            keys(p, path, POOL_KEYS, Set.of("name", "card_ids"));
            String name = string(p, "name", path, 1, 64);
            if (!names.add(name)) {
                throw new MatchInputException(path + ".name: " + name + " is used by another pool");
            }
            List<String> ids = new ArrayList<>();
            List<Value> rawIds = list(p.get("card_ids"), path + ".card_ids");
            for (int j = 0; j < rawIds.size(); j++) {
                if (!(rawIds.get(j) instanceof Value.StrV s)) {
                    throw new MatchInputException(path + ".card_ids[" + j + "]: must be a string");
                }
                ids.add(s.v());
            }
            Map<String, Integer> tierSlots = new TreeMap<>();
            int total = 0;
            if (p.get("tier_slots") != null) {
                Value.MapV slots = object(p.get("tier_slots"), path + ".tier_slots");
                for (Map.Entry<String, Value> e : slots.fields().entrySet()) {
                    if (!TIERS.contains(e.getKey())) {
                        throw new MatchInputException(path + ".tier_slots: unknown Tier '" + e.getKey()
                                + "', must be one of " + new TreeSet<>(TIERS));
                    }
                    int n = (int) integer(slots, e.getKey(), path + ".tier_slots", 0, 1000);
                    tierSlots.put(e.getKey(), n);
                    total += n;
                }
            }
            int support = p.get("support_slots") == null ? 0 : (int) integer(p, "support_slots", path, 0, 1000);
            if (total + support < 1) {
                throw new MatchInputException(path + ": a pack pool needs at least one slot");
            }
            out.add(new PackPool(name, List.copyOf(ids), tierSlots, support));
        }
        return out;
    }

    private static Faction faction(Value.MapV m) {
        String s = string(m, "faction", "input", 1, 64);
        for (Faction f : Faction.values()) {
            if (f.name().equals(s)) {
                return f;
            }
        }
        List<String> codes = new ArrayList<>();
        for (Faction f : Faction.values()) {
            codes.add(f.name());
        }
        throw new MatchInputException("input.faction: must be one of " + codes + ", got " + s);
    }

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
}
