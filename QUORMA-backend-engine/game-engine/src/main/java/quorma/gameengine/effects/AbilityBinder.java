package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.EvolutionDef;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.KeywordDef;
import quorma.gameengine.data.ParamSpec;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.SynergyDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.SpellCard;

/**
 * The expansion parser: turns a card's abilities into {@link BoundExpansion}s, in card order.
 * <br/>
 * Any problem throws {@link AbilityException}: a card is played with all its abilities or not at all.
 */
public final class AbilityBinder {

    /** Targets a spell's immediate effects may aim at: groups, selections, the players, lanes and the deck. */
    private static final Set<String> SPELL_TARGETS = Set.of("S9", "S10", "S11", "S30", "S31", "S32", "S33", "S35",
            "S37", "S38", "S39", "S40", "S44", "S47");

    /** Targets an equipment's immediate effects may aim at: the creature it is applied to and the players. */
    private static final Set<String> EQUIPMENT_TARGETS = Set.of("S27", "S30", "S31");

    private final Ruleset rules;

    public AbilityBinder(Ruleset rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public Ruleset rules() {
        return rules;
    }

    /** The bound expansions of every ability of {@code card}, abilities in card order, expansions in file order. */
    public List<BoundExpansion> bind(CreatureCard card) {
        List<BoundExpansion> out = new ArrayList<>();
        List<Ability> abilities = card.abilities();
        for (int i = 0; i < abilities.size(); i++) {
            switch (abilities.get(i)) {
                case AbilityRef ref -> out.addAll(ref.id().startsWith("EVO_")
                        ? bindEvolution("card " + card.id() + " " + ref.id(), ref.id(), ref.params())
                        : bindKeyword("card " + card.id() + " " + ref.id(), ref.id(), ref.params()));
                case InlineEffect inline -> out.addAll(bindInline(card.id(), inlineId(i + 1), inline));
            }
        }
        return List.copyOf(out);
    }

    /**
     * The immediate expansions of a spell or an equipment (ruleset 3.3, 3.4): class I with no trigger, in card order.
     * A keyword or an Evolution is for creatures, so a reference is refused. A spell aims at groups of creatures,
     * selections ({@code S33}-{@code S40}), the players, an empty lane or its deck; an equipment at the creature it
     * is applied to ({@code S27}) or the players.
     *
     * @throws IllegalArgumentException for a creature
     */
    public List<BoundExpansion> bindImmediate(Card card) {
        return bindCard(card).stream().filter(e -> e.cls() == ResClass.I).toList();
    }

    /**
     * The lasting expansions of a spell or an equipment: the ones with a trigger. An equipment gives them to the
     * creature it is applied to (its {@code S27} becomes {@code S1}, except with T29, where S27 is the replacement); a
     * spell leaves them on its player's side of the table for their duration.
     */
    public List<BoundExpansion> bindLasting(Card card) {
        return bindCard(card).stream().filter(e -> e.cls() != ResClass.I).toList();
    }

    private List<BoundExpansion> bindCard(Card card) {
        if (card instanceof CreatureCard) {
            throw new IllegalArgumentException("not a spell or an equipment: " + card.id());
        }
        boolean spell = card instanceof SpellCard;
        String thing = spell ? "a spell" : "an equipment";
        Set<String> targets = spell ? SPELL_TARGETS : EQUIPMENT_TARGETS;
        List<BoundExpansion> out = new ArrayList<>();
        List<Ability> abilities = card.abilities();
        for (int i = 0; i < abilities.size(); i++) {
            String id = inlineId(i + 1);
            String where = "card " + card.id() + " " + id;
            if (!(abilities.get(i) instanceof InlineEffect inline)) {
                throw new AbilityException("card " + card.id() + " " + ((AbilityRef) abilities.get(i)).id()
                        + ": " + thing + " has its effects written as expansions (keywords and evolutions are for creatures)");
            }
            List<ExpansionDef> expansions = inline.expansions();
            for (int j = 0; j < expansions.size(); j++) {
                ExpansionDef expansion = expansions.get(j);
                if (expansion.triggerId() == null && !targets.contains(expansion.targetId())) {
                    throw new AbilityException(where + ": " + thing + " aims at " + new java.util.TreeSet<>(targets)
                            + ", not " + expansion.targetId());
                }
                if (expansion.conditions().stream().anyMatch(c -> c.id().equals("K_PARAM"))) {
                    throw new AbilityException(where + ": K_PARAM needs a keyword");
                }
                if ("T_EVO".equals(expansion.triggerId())) {
                    throw new AbilityException(where + ": " + thing + " has no Evolution (T_EVO is for creatures)");
                }
                BoundExpansion bound = bindExpansion(where, id, j, expansion, Map.of(), Map.of(), expansion.conditions());
                EffectRegistry.check(rules.vocab(), bound, where, spell ? Origin.SPELL : Origin.EQUIPMENT);
                out.add(bound);
            }
        }
        return List.copyOf(out);
    }

    /**
     * The id of an ability written on the card (data/AGENTS.md section 12): {@code INLINE_<n>}, n being its
     * position among all the card's abilities, from 1, keywords included. It names the ability in errors and in
     * the MatchLog, and is its default stack key.
     */
    public static String inlineId(int position) {
        return "INLINE_" + position;
    }

    /**
     * An ability written on the card as expansions. It has no keyword, so no card params, no {@code $param} and
     * no {@code K_PARAM}; {@code $tune} still resolves against {@code balance.json}. An expansion with trigger
     * {@code T_EVO} is an Evolution written on the card: it fires once and counts for Spira like any other.
     */
    private List<BoundExpansion> bindInline(String cardId, String abilityId, InlineEffect inline) {
        String where = "card " + cardId + " " + abilityId;
        List<BoundExpansion> out = new ArrayList<>();
        List<ExpansionDef> expansions = inline.expansions();
        for (int j = 0; j < expansions.size(); j++) {
            ExpansionDef expansion = expansions.get(j);
            if (expansion.triggerId() == null) {
                throw new AbilityException(where + ": an expansion with no trigger is for a spell or an equipment");
            }
            if (expansion.conditions().stream().anyMatch(c -> c.id().equals("K_PARAM"))) {
                throw new AbilityException(
                        where + ": conditions on an ability written on the card are not implemented by the engine yet"
                                + " (K_PARAM needs a keyword)");
            }
            BoundExpansion bound;
            if ("T_EVO".equals(expansion.triggerId())) {
                bound = unwrapEvolution(where, abilityId, j, expansion, Map.of(), Map.of());
            } else {
                bound = bindExpansion(where, abilityId, j, expansion, Map.of(), Map.of(), expansion.conditions());
            }
            EffectRegistry.check(rules.vocab(), bound, where, Origin.CREATURE);
            out.add(bound);
        }
        return out;
    }

    /**
     * An Evolution by id, with the card's params (or the params an E16 grants it with). Its expansions use the
     * wrapper trigger T_EVO: the bound expansion has the real trigger ({@code trigger_params.trigger_id}) and is
     * marked as an Evolution, which fires at most {@code max_triggers} (1) times per creature. Numbers resolve like a
     * keyword's ({@code $tune.EVO_X.name}, the card's params first).
     */
    public List<BoundExpansion> bindEvolution(String where, String evolutionId, Map<String, Value> given) {
        EvolutionDef evolution = rules.evolutions().get(evolutionId);
        if (evolution == null) {
            throw new AbilityException(where + ": unknown ability (not a live keyword or evolution)");
        }
        KeywordDef asKeyword = new KeywordDef(evolution.id(), evolution.name(), ResClass.T, evolution.balanceRef(),
                Map.of(), evolution.description(), evolution.expandsTo());
        Map<String, Value> params = cardParams(where, asKeyword, allowedCardParams(asKeyword), given);

        List<BoundExpansion> out = new ArrayList<>();
        List<ExpansionDef> expansions = evolution.expandsTo();
        for (int j = 0; j < expansions.size(); j++) {
            ExpansionDef expansion = expansions.get(j);
            if (!"T_EVO".equals(expansion.triggerId())) {
                throw new AbilityException(where + ": an Evolution must use T_EVO with a trigger_id");
            }
            BoundExpansion bound = unwrapEvolution(where, evolution.id(), j, expansion, params, given);
            EffectRegistry.check(rules.vocab(), bound, where, Origin.CREATURE);
            out.add(bound);
        }
        return List.copyOf(out);
    }

    private BoundExpansion unwrapEvolution(String where, String abilityId, int index, ExpansionDef expansion,
            Map<String, Value> params, Map<String, Value> cardParams) {
        if (!(expansion.triggerParams().get("trigger_id") instanceof Value.StrV inner)) {
            throw new AbilityException(where + ": an Evolution must use T_EVO with a trigger_id");
        }
        Map<String, Value> innerParams = expansion.triggerParams().get("trigger_params") instanceof Value.MapV m
                ? m.fields() : Map.of();
        int maxTriggers = expansion.triggerParams().get("max_triggers") instanceof Value.IntV n
                ? Math.toIntExact(n.v()) : 1;
        ExpansionDef unwrapped = new ExpansionDef(inner.v(), innerParams, expansion.effectId(),
                expansion.effectParams(), expansion.targetId(), expansion.targetParams(), expansion.duration(),
                expansion.conditions(), expansion.cls(), expansion.common());
        return bindExpansion(where, abilityId, index, unwrapped, params, cardParams, inPlayConditions(unwrapped))
                .asEvolution(maxTriggers);
    }

    /** A keyword by id, with the card's params (or the params an E14 grants it with). */
    public List<BoundExpansion> bindKeyword(String where, String keywordId, Map<String, Value> given) {
        KeywordDef keyword = rules.keywords().get(keywordId);
        if (keyword == null) {
            throw new AbilityException(where + ": unknown ability (not a live keyword)");
        }
        Map<String, Value> params = cardParams(where, keyword, allowedCardParams(keyword), given);

        List<BoundExpansion> out = new ArrayList<>();
        List<ExpansionDef> expansions = keyword.expandsTo();
        for (int j = 0; j < expansions.size(); j++) {
            ExpansionDef expansion = expansions.get(j);
            if (cardConditionsHold(where, expansion, params)) {
                BoundExpansion bound = bindExpansion(where, keyword.id(), j, expansion, params, given,
                        inPlayConditions(expansion));
                EffectRegistry.check(rules.vocab(), bound, where, Origin.CREATURE);
                out.add(bound);
            }
        }
        return out;
    }

    /**
     * The expansions of a suit synergy tier ({@code synergies.json}) that the engine implements, bound for the
     * creature that is their source. A tier expansion the engine does not implement yet is left out: it changes
     * nothing, as before. Numbers come from {@code balance.json} ({@code $tune.synergies.<TIER>.<name>}).
     */
    public List<BoundExpansion> bindSynergy(SynergyDef tier) {
        String where = "synergy " + tier.id();
        List<BoundExpansion> out = new ArrayList<>();
        List<ExpansionDef> expansions = tier.expandsTo();
        for (int j = 0; j < expansions.size(); j++) {
            ExpansionDef expansion = expansions.get(j);
            if (EffectRegistry.isImplemented(expansion)) {
                BoundExpansion bound = bindExpansion(where, tier.id(), j, expansion, Map.of(), Map.of(), expansion.conditions());
                EffectRegistry.check(rules.vocab(), bound, where, Origin.CREATURE);
                out.add(bound);
            }
        }
        return List.copyOf(out);
    }

    /**
     * The params a card may write for {@code keyword}: the ones it declares, the tier-scaled ones and the numbers of
     * its {@code balance.json} entry, which {@code $tune} reads from the card first (data/AGENTS.md section 3).
     */
    private Set<String> allowedCardParams(KeywordDef keyword) {
        Set<String> out = new LinkedHashSet<>();
        for (String section : new String[] {"keywords", "evolutions"}) {
            if (rules.balance().get(section) instanceof Value.MapV entries
                    && entries.get(keyword.balanceRef()) instanceof Value.MapV entry) {
                if (entry.get("tier_scaled") instanceof Value.ListV names) {
                    for (Value name : names.items()) {
                        if (name instanceof Value.StrV s) {
                            out.add(s.v());
                        }
                    }
                }
                if (entry.get("params") instanceof Value.MapV p) {
                    out.addAll(p.fields().keySet());
                }
            }
        }
        return out;
    }

    /** The card's params checked against the keyword's declaration, with defaults filled in. */
    private static Map<String, Value> cardParams(
            String where, KeywordDef keyword, Set<String> tuned, Map<String, Value> given) {
        for (Map.Entry<String, Value> e : given.entrySet()) {
            String name = e.getKey();
            if (!keyword.params().containsKey(name) && !tuned.contains(name)) {
                throw new AbilityException(where + ": unknown param '" + name + "'");
            }
            if (!keyword.params().containsKey(name) && !(e.getValue() instanceof Value.IntV)) {
                throw new AbilityException(where + ": tier-scaled param '" + name + "' must be an integer");
            }
        }
        Map<String, Value> out = new LinkedHashMap<>(given);
        for (Map.Entry<String, ParamSpec> e : keyword.params().entrySet()) {
            String name = e.getKey();
            ParamSpec spec = e.getValue();
            Value value = given.get(name);
            if (value == null) {
                if (spec.required()) {
                    throw new AbilityException(where + ": missing required param '" + name + "'");
                }
                if (spec.defaultValue() != null) {
                    out.put(name, spec.defaultValue());
                }
                continue;
            }
            if ("int".equals(spec.type()) && !(value instanceof Value.IntV)) {
                throw new AbilityException(where + ": param '" + name + "' must be an integer");
            }
            if (!spec.allowedValues().isEmpty() && !spec.allowedValues().contains(value)) {
                throw new AbilityException(where + ": param '" + name + "' must be one of " + display(spec.allowedValues()));
            }
        }
        return out;
    }

    /** The conditions that are checked while the creature is in play: everything but {@code K_PARAM}. */
    private static List<ConditionDef> inPlayConditions(ExpansionDef expansion) {
        return expansion.conditions().stream().filter(c -> !"K_PARAM".equals(c.id())).toList();
    }

    /** {@code K_PARAM} conditions, evaluated on the card's params when the creature is bound. */
    private static boolean cardConditionsHold(String where, ExpansionDef expansion, Map<String, Value> params) {
        for (ConditionDef condition : expansion.conditions()) {
            if (!"K_PARAM".equals(condition.id())) {
                continue;
            }
            if (!(condition.params().get("name") instanceof Value.StrV name)
                    || !(condition.params().get("equals") instanceof Value.StrV equals)) {
                throw new AbilityException(where + ": K_PARAM needs string params 'name' and 'equals'");
            }
            if (!(params.get(name.v()) instanceof Value.StrV actual) || !actual.v().equals(equals.v())) {
                return false;
            }
        }
        return true;
    }

    private BoundExpansion bindExpansion(String where, String abilityId, int index, ExpansionDef expansion,
            Map<String, Value> params, Map<String, Value> cardParams, List<ConditionDef> conditions) {
        Map<String, Value> effectParams = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : expansion.effectParams().entrySet()) {
            flatten(where, e.getKey(), e.getValue(), params, cardParams, effectParams);
        }
        effectParams = EffectRegistry.withDefaults(expansion.effectId(), effectParams);

        OptionalInt maxStacks = OptionalInt.empty();
        Optional<String> stackKey = Optional.empty();
        int maxTriggers = 0;
        boolean oncePerTurn = false;
        for (Map.Entry<String, Value> e : expansion.common().entrySet()) {
            Value value = resolve(where, e.getValue(), params, cardParams);
            switch (e.getKey()) {
                case "max_stacks" -> {
                    if (!(value instanceof Value.IntV n) || n.v() < 1) {
                        throw new AbilityException(where + ": max_stacks must be a positive integer");
                    }
                    maxStacks = OptionalInt.of(Math.toIntExact(n.v()));
                }
                case "stack_key" -> {
                    if (!(value instanceof Value.StrV s)) {
                        throw new AbilityException(where + ": stack_key must be a string");
                    }
                    stackKey = Optional.of(s.v());
                }
                case "max_triggers" -> {
                    if (!(value instanceof Value.IntV n) || n.v() < 1) {
                        throw new AbilityException(where + ": max_triggers must be a positive integer");
                    }
                    maxTriggers = Math.toIntExact(n.v());
                }
                case "once_per_turn" -> {
                    if (!(value instanceof Value.BoolV b)) {
                        throw new AbilityException(where + ": once_per_turn must be true or false");
                    }
                    oncePerTurn = b.v();
                }
                default -> throw new AbilityException(where + ": " + e.getKey() + " is not implemented by the engine yet");
            }
        }
        if (maxStacks.isPresent() && stackKey.isEmpty()) {
            stackKey = Optional.of(abilityId); // vocab.common_params.stack_key: defaults to the source's id
        }
        return new BoundExpansion(abilityId, expansion.triggerId(), expansion.triggerParams(), expansion.effectId(),
                expansion.targetId(), expansion.targetParams(), conditions, expansion.duration(), expansion.cls(),
                effectParams, maxStacks, stackKey, false, maxTriggers, oncePerTurn, index);
    }

    /** Resolves a param; an object becomes one param per field, named {@code parent.field} (E12 {@code lockout}). */
    private void flatten(String where, String name, Value value, Map<String, Value> params, Map<String, Value> cardParams,
            Map<String, Value> out) {
        if (value instanceof Value.MapV map) {
            for (Map.Entry<String, Value> field : map.fields().entrySet()) {
                flatten(where, name + "." + field.getKey(), field.getValue(), params, cardParams, out);
            }
        } else {
            out.put(name, resolve(where, value, params, cardParams));
        }
    }

    /** Resolves a reference to a plain integer, string or boolean; {@code -$...} negates the integer. */
    private Value resolve(String where, Value value, Map<String, Value> params, Map<String, Value> cardParams) {
        Value resolved = value;
        if (value instanceof Value.Ref ref) {
            resolved = switch (ref.kind()) {
                case PARAM -> params.get(ref.path());
                case TUNE -> tune(ref.path(), cardParams);
            };
            if (resolved == null) {
                throw new AbilityException(where + ": cannot resolve " + ref.text());
            }
            if (ref.negated()) {
                if (!(resolved instanceof Value.IntV n)) {
                    throw new AbilityException(where + ": " + ref.text() + " negates a value that is not an integer");
                }
                resolved = new Value.IntV(Math.negateExact(n.v()));
            }
        }
        if (!(resolved instanceof Value.IntV || resolved instanceof Value.StrV || resolved instanceof Value.BoolV)) {
            throw new AbilityException(where + ": a param must be an integer, a string or a boolean, got " + resolved);
        }
        return resolved;
    }

    private Value tune(String path, Map<String, Value> cardParams) {
        int dot = path.indexOf('.');
        if (path.startsWith("synergies.")) {
            String[] parts = path.split("\\.");
            return parts.length == 3 ? rules.synergyTune(parts[1], parts[2]).orElse(null) : null;
        }
        if (dot <= 0 || dot == path.length() - 1) {
            return null;
        }
        return rules.tune(path.substring(0, dot), path.substring(dot + 1), cardParams).orElse(null);
    }

    private static String display(List<Value> values) {
        List<String> out = new ArrayList<>();
        for (Value v : values) {
            out.add(v instanceof Value.StrV s ? s.v() : String.valueOf(v));
        }
        return out.toString();
    }
}
