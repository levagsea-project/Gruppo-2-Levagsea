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
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.KeywordDef;
import quorma.gameengine.data.ParamSpec;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.InlineEffect;

/**
 * The expansion parser: turns a card's abilities into {@link BoundExpansion}s, in card order.
 * <br/>
 * Any problem throws {@link AbilityException}: a card is played with all its abilities or not at all.
 */
public final class AbilityBinder {

    private final Ruleset rules;

    public AbilityBinder(Ruleset rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /** The bound expansions of every ability of {@code card}, abilities in card order, expansions in file order. */
    public List<BoundExpansion> bind(CreatureCard card) {
        List<BoundExpansion> out = new ArrayList<>();
        List<Ability> abilities = card.abilities();
        for (int i = 0; i < abilities.size(); i++) {
            switch (abilities.get(i)) {
                case AbilityRef ref -> out.addAll(bindKeyword(card.id(), ref));
                case InlineEffect inline -> out.addAll(bindInline(card.id(), inlineId(i + 1), inline));
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
     * no {@code K_PARAM}; {@code $tune} still resolves against {@code balance.json}.
     */
    private List<BoundExpansion> bindInline(String cardId, String abilityId, InlineEffect inline) {
        String where = "card " + cardId + " " + abilityId;
        List<BoundExpansion> out = new ArrayList<>();
        for (ExpansionDef expansion : inline.expansions()) {
            if (!expansion.conditions().isEmpty()) {
                throw new AbilityException(
                        where + ": conditions on an ability written on the card are not implemented by the engine yet");
            }
            BoundExpansion bound = bindExpansion(where, abilityId, expansion, Map.of(), Map.of());
            EffectRegistry.check(bound, where);
            out.add(bound);
        }
        return out;
    }

    private List<BoundExpansion> bindKeyword(String cardId, AbilityRef ref) {
        String where = "card " + cardId + " " + ref.id();
        KeywordDef keyword = rules.keywords().get(ref.id());
        if (keyword == null) {
            throw new AbilityException(where + ": unknown ability (not a live keyword)");
        }
        Map<String, Value> params = cardParams(where, keyword, tierScaled(keyword), ref.params());

        List<BoundExpansion> out = new ArrayList<>();
        for (ExpansionDef expansion : keyword.expandsTo()) {
            if (conditionsHold(where, expansion, params)) {
                BoundExpansion bound = bindExpansion(where, keyword.id(), expansion, params, ref.params());
                EffectRegistry.check(bound, where);
                out.add(bound);
            }
        }
        return out;
    }

    /**
     * The names in {@code balance.json} {@code keywords[balance_ref].tier_scaled}: pool creation writes their
     * scaled value on the card, so a card may carry them even when the keyword does not declare them.
     */
    private Set<String> tierScaled(KeywordDef keyword) {
        Set<String> out = new LinkedHashSet<>();
        if (rules.balance().get("keywords") instanceof Value.MapV entries
                && entries.get(keyword.balanceRef()) instanceof Value.MapV entry
                && entry.get("tier_scaled") instanceof Value.ListV names) {
            for (Value name : names.items()) {
                if (name instanceof Value.StrV s) {
                    out.add(s.v());
                }
            }
        }
        return out;
    }

    /** The card's params checked against the keyword's declaration, with defaults filled in. */
    private static Map<String, Value> cardParams(
            String where, KeywordDef keyword, Set<String> tierScaled, Map<String, Value> given) {
        for (Map.Entry<String, Value> e : given.entrySet()) {
            String name = e.getKey();
            if (!keyword.params().containsKey(name) && !tierScaled.contains(name)) {
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

    /** {@code K_PARAM} conditions, evaluated on the card's params. Any other condition is not implemented yet. */
    private static boolean conditionsHold(String where, ExpansionDef expansion, Map<String, Value> params) {
        for (ConditionDef condition : expansion.conditions()) {
            if (!"K_PARAM".equals(condition.id())) {
                throw new AbilityException(where + ": condition " + condition.id() + " is not implemented by the engine yet");
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

    private BoundExpansion bindExpansion(String where, String abilityId, ExpansionDef expansion,
            Map<String, Value> params, Map<String, Value> cardParams) {
        if (!expansion.triggerParams().isEmpty() || !expansion.targetParams().isEmpty()) {
            throw new AbilityException(where + ": trigger and target params are not implemented by the engine yet");
        }
        Map<String, Value> effectParams = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : expansion.effectParams().entrySet()) {
            effectParams.put(e.getKey(), resolve(where, e.getValue(), params, cardParams));
        }
        effectParams = EffectRegistry.withDefaults(expansion.effectId(), effectParams);

        OptionalInt maxStacks = OptionalInt.empty();
        Optional<String> stackKey = Optional.empty();
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
                default -> throw new AbilityException(where + ": " + e.getKey() + " is not implemented by the engine yet");
            }
        }
        if (maxStacks.isPresent() && stackKey.isEmpty()) {
            stackKey = Optional.of(abilityId); // vocab.common_params.stack_key: defaults to the source's id
        }
        return new BoundExpansion(abilityId, expansion.triggerId(), expansion.effectId(), expansion.targetId(),
                expansion.duration(), expansion.cls(), effectParams, maxStacks, stackKey);
    }

    /** Resolves a reference to a plain integer, string or boolean. */
    private Value resolve(String where, Value value, Map<String, Value> params, Map<String, Value> cardParams) {
        Value resolved = value;
        if (value instanceof Value.Ref ref) {
            resolved = switch (ref.kind()) {
                case PARAM -> params.get(ref.path());
                case TUNE -> tune(ref.path(), cardParams);
            };
            if (resolved == null) {
                throw new AbilityException(where + ": cannot resolve $" + ref.kind().name().toLowerCase() + "." + ref.path());
            }
        }
        if (!(resolved instanceof Value.IntV || resolved instanceof Value.StrV || resolved instanceof Value.BoolV)) {
            throw new AbilityException(where + ": a param must be an integer, a string or a boolean, got " + resolved);
        }
        return resolved;
    }

    private Value tune(String path, Map<String, Value> cardParams) {
        int dot = path.indexOf('.');
        if (dot <= 0 || dot == path.length() - 1 || path.startsWith("synergies.")) {
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
