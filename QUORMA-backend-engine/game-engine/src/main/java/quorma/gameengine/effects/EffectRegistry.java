package quorma.gameengine.effects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;

/**
 * What the engine can resolve. A card whose abilities use anything else is refused when it is bound
 * ({@link AbilityException}), so an ability is never silently ignored.
 *
 * <p>Keywords are checked by {@code data/validate.py}, abilities written on the card are not, so every bound
 * expansion is checked here the same way: its (trigger, effect, class, duration) must be one of the
 * {@link #SHAPES} the engine runs, and its effect params must be the ones that shape uses, integers in range.
 * An ability written on the card can change the numbers of a shape and add {@code max_stacks}, nothing else.</p>
 */
public final class EffectRegistry {

    /** One way the engine runs an effect. Target is always S1 (the creature itself) for now. */
    private record Shape(String triggerId, String effectId, ResClass cls, String duration) {}

    private static final Set<Shape> SHAPES = Set.of(
            new Shape("T0", "E28", ResClass.C, "D_Permanente"),  // Scudo: less damage taken
            new Shape(null, "E1", ResClass.I, "D_Permanente"),    // spell or equipment: permanent + ATK
            new Shape(null, "E2", ResClass.I, "D_Permanente"),    // spell or equipment: permanent + DEF
            new Shape(null, "E6", ResClass.I, "D_Istantanea"),    // spell or equipment: heal
            new Shape("T0", "E55", ResClass.C, "D_Permanente"),  // Camaleonte: counts as any suit for the hand
            new Shape("T0", "E1", ResClass.C, "D_FincheSorgenteInGioco"), // static ATK bonus (suit synergies, cards)
            new Shape("T0", "E34", ResClass.C, "D_FincheSorgenteInGioco"), // the group's creatures count as adjacent
            new Shape("T0", "E52", ResClass.C, "D_FincheSorgenteInGioco"), // ATK cannot be lowered by effects
            new Shape("T0", "E56", ResClass.C, "D_Permanente"),  // Perforante: ignores part of the target's reductions
            new Shape("T19", "E1", ResClass.T, "D_Permanente"),  // Insaziabile: + share of base ATK on a kill
            new Shape("T34", "E21", ResClass.R, "D_Istantanea"),  // Traboccante: spill multiplier factor
            new Shape("T0", "E19", ResClass.C, "D_FincheSorgenteInGioco"), // Evolution values multiplied (Spira 4)
            new Shape("T12", "E1", ResClass.T, "D_Permanente"),   // + share of base ATK after surviving damage (Crescita)
            new Shape("T9", "E2", ResClass.T, "D_Permanente"),    // + share of base DEF when targeted (Trincea)
            new Shape("T5", "E17", ResClass.T, "D_Istantanea"),   // force the Evolutions of a suit (Spira 5)
            new Shape("T9", "E24", ResClass.T, "D_Istantanea"),   // redirect an attack declared against this creature
            new Shape("T34", "E22", ResClass.R, "D_Istantanea"),  // spill treats the target's DEF as a share of itself
            new Shape("T2", "E6", ResClass.T, "D_Istantanea"),    // heal at the start of a phase (Refill)
            new Shape("T20", "E12", ResClass.R, "D_Istantanea"),  // saved from destruction (Vestigio 5)
            new Shape("T29", "E1", ResClass.T, "D_FinoAUccisione")); // + ATK for a replacement until it kills (Vestigio 3)

    /** The params of one effect mode: integer params with their range, string params with their one value. */
    private record ParamRule(Map<String, int[]> ints, Map<String, Integer> defaults, Map<String, Set<String>> strings,
            Map<String, Set<String>> optional) {

        ParamRule(Map<String, int[]> ints, Map<String, Integer> defaults) {
            this(ints, defaults, Map.of(), Map.of());
        }

        ParamRule(Map<String, int[]> ints, Map<String, Integer> defaults, Map<String, Set<String>> strings) {
            this(ints, defaults, strings, Map.of());
        }

        boolean takes(String name) {
            return ints.containsKey(name) || strings.containsKey(name) || optional.containsKey(name);
        }
    }

    private static final int MAX = Integer.MAX_VALUE;

    /** Per effect, per {@code mode} ({@code ""}: the effect has no mode). */
    private static final Map<String, Map<String, ParamRule>> PARAMS_1 = Map.of(
            "E1", Map.of(
                    "pct_base_atk", new ParamRule(Map.of("permille", new int[] {0, MAX}), Map.of()),
                    "flat", new ParamRule(Map.of("amount", new int[] {0, MAX}), Map.of(), Map.of(),
                            Map.of("scale_by", Set.of("own_evolutions_triggered")))),
            "E21", Map.of("", new ParamRule(Map.of("factor", new int[] {0, MAX}), Map.of())),
            "E28", Map.of(
                    "reduction_pct", new ParamRule(Map.of("pct", new int[] {0, 100}), Map.of()),
                    "flat", new ParamRule(
                            Map.of("amount", new int[] {0, MAX}, "min_damage", new int[] {0, MAX}),
                            Map.of("min_damage", 1))),
            "E55", Map.of("", new ParamRule(Map.of(), Map.of())),
            "E22", Map.of("", new ParamRule(Map.of("permille", new int[] {0, 1000}), Map.of())),
            "E52", Map.of("", new ParamRule(Map.of(), Map.of())),
            "E34", Map.of("", new ParamRule(Map.of(), Map.of(), Map.of("group", Set.of("Vincolo", "Frattura", "Spira", "Vestigio")))),
            "E56", Map.of("", new ParamRule(Map.of("pct", new int[] {0, 100}), Map.of())),
            "E6", Map.of("flat", new ParamRule(Map.of("amount", new int[] {0, MAX}), Map.of())),
            "E12", Map.of("", new ParamRule(Map.of("lockout.reactivate_below", new int[] {1, 5}), Map.of(), Map.of(
                    "then", Set.of("destroy_at_next_refill"),
                    "lockout.key", Set.of(),
                    "lockout.suit", Set.of("Vincolo", "Frattura", "Spira", "Vestigio")))));

    private static final Map<String, Map<String, ParamRule>> PARAMS_2 = Map.of(
            "E2", Map.of(
                    "pct_base_def", new ParamRule(Map.of("permille", new int[] {0, MAX}), Map.of()),
                    "flat", new ParamRule(Map.of("amount", new int[] {0, MAX}), Map.of())),
            "E17", Map.of("", new ParamRule(Map.of(), Map.of())),
            "E24", Map.of("", new ParamRule(Map.of(), Map.of(), Map.of("new_target_id", Set.of("S7")))),
            "E19", Map.of("", new ParamRule(Map.of("factor", new int[] {0, MAX}), Map.of())));

    private static final Map<String, Map<String, ParamRule>> PARAMS;

    static {
        Map<String, Map<String, ParamRule>> all = new LinkedHashMap<>(PARAMS_1);
        all.putAll(PARAMS_2);
        PARAMS = Map.copyOf(all);
    }

    private static final Set<String> TRIGGERS = Set.of("T0", "T2", "T5", "T9", "T12", "T19", "T20", "T29", "T34");
    private static final Set<String> TARGETS = Set.of("S1", "S9", "S10", "S12", "S27", "S41");
    private static final Set<String> DURATIONS = Set.of("D_Permanente", "D_Istantanea", "D_FincheSorgenteInGioco", "D_FinoAUccisione");

    /** Conditions the engine evaluates while the creature is in play ({@code StaticEffects}). */
    private static final Set<String> CONDITIONS = Set.of("C1", "C4", "C11", "C22", "C24", "C25");

    /** The filters of target S41 ({@code params.filters}). */
    private static final Set<String> TARGET_FILTERS = Set.of("attacker");

    private static final Set<String> SUITS = Set.of("Vincolo", "Frattura", "Spira", "Vestigio");

    private EffectRegistry() {}

    /**
     * {@code params} with the defaults of the effect's mode added (flat Scudo: {@code min_damage} 1, as in
     * {@code vocab.json}). Unknown effects or modes are returned unchanged and refused by {@link #check}.
     */
    static Map<String, Value> withDefaults(String effectId, Map<String, Value> params) {
        String mode = params.get("mode") instanceof Value.StrV s ? s.v() : "";
        ParamRule rule = PARAMS.getOrDefault(effectId, Map.of()).get(mode);
        Map<String, Value> out = new LinkedHashMap<>(params);
        if (rule != null) {
            rule.defaults().forEach((name, value) -> out.putIfAbsent(name, new Value.IntV(value)));
        }
        return out;
    }

    /**
     * True if the engine runs {@code expansion} as it is written in the data (before references are resolved): the
     * same test as {@link #check}, without the ranges of the numbers. For data files that may hold more than the
     * engine implements, such as {@code synergies.json}; a tier expansion that is not implemented changes nothing.
     */
    public static boolean isImplemented(quorma.gameengine.data.ExpansionDef x) {
        Map<String, ParamRule> modes = PARAMS.get(x.effectId());
        if (modes == null || (x.triggerId() != null && !TRIGGERS.contains(x.triggerId()))
                || !triggerParamsOk(x.triggerId(), x.triggerParams()) || !TARGETS.contains(x.targetId())
                || (everyoneTarget(x.targetId()) && x.cls() != ResClass.I)
                || !DURATIONS.contains(x.duration())
                || !SHAPES.contains(new Shape(x.triggerId(), x.effectId(), x.cls(), x.duration()))
                || !x.common().isEmpty()) {
            return false;
        }
        ParamRule rule = modes.get(x.effectParams().get("mode") instanceof Value.StrV s ? s.v() : "");
        if (rule == null) {
            return false;
        }
        Map<String, Value> flat = new LinkedHashMap<>();
        flattenParams("", x.effectParams(), flat);
        for (String name : flat.keySet()) {
            if (!name.equals("mode") && !rule.takes(name)) {
                return false;
            }
        }
        for (String name : rule.strings().keySet()) {
            Set<String> allowed = rule.strings().get(name);
            if (!(flat.get(name) instanceof Value.StrV s && (allowed.isEmpty() || allowed.contains(s.v())))) {
                return false;
            }
        }
        for (Map.Entry<String, Set<String>> o : rule.optional().entrySet()) {
            if (flat.containsKey(o.getKey())
                    && !(flat.get(o.getKey()) instanceof Value.StrV s && o.getValue().contains(s.v()))) {
                return false;
            }
        }
        for (quorma.gameengine.data.ConditionDef c : x.conditions()) {
            if (!CONDITIONS.contains(c.id())) {
                return false;
            }
        }
        return targetParamsOk(x.targetId(), x.targetParams());
    }

    private static void flattenParams(String prefix, Map<String, Value> params, Map<String, Value> out) {
        for (Map.Entry<String, Value> e : params.entrySet()) {
            if (e.getValue() instanceof Value.MapV m) {
                flattenParams(prefix + e.getKey() + ".", m.fields(), out);
            } else {
                out.put(prefix + e.getKey(), e.getValue());
            }
        }
    }

    /** T2 takes the phase (only Refill is implemented), T5 the suit and level; no other trigger takes params. */
    private static boolean triggerParamsOk(String triggerId, Map<String, Value> params) {
        if (triggerId == null) {
            return params.isEmpty();
        }
        if (triggerId.equals("T5")) {
            return params.size() == 2 && params.get("suit") instanceof Value.StrV s && SUITS.contains(s.v())
                    && params.get("level") instanceof Value.IntV n && n.v() >= 3 && n.v() <= 5;
        }
        if (triggerId.equals("T2")) {
            return params.size() == 1 && params.get("phase") instanceof Value.StrV p && p.v().equals("Rifornimento");
        }
        return params.isEmpty();
    }

    /** S9 and S10 name the creatures of a whole side: only the immediate effects of spells use them. */
    private static boolean everyoneTarget(String targetId) {
        return targetId.equals("S9") || targetId.equals("S10");
    }

    private static boolean targetParamsOk(String targetId, Map<String, Value> params) {
        if (targetId.equals("S12")) {
            return params.size() == 1 && params.get("suit") instanceof Value.StrV s && SUITS.contains(s.v());
        }
        if (targetId.equals("S41")) {
            return params.keySet().stream().allMatch(k -> k.equals("filters"))
                    && (params.get("filters") == null || params.get("filters") instanceof Value.ListV l
                            && l.items().stream().allMatch(v -> v instanceof Value.StrV f && TARGET_FILTERS.contains(f.v())));
        }
        return params.isEmpty();
    }

    /**
     * Checks that the engine implements every piece of {@code expansion}.
     *
     * @param where card and ability, for the error message
     * @throws AbilityException naming the first problem
     */
    public static void check(BoundExpansion expansion, String where) {
        String effect = expansion.effectId();
        Map<String, ParamRule> modes = PARAMS.get(effect);
        if (modes == null) {
            throw notImplemented(where, "effect " + effect);
        }
        if (expansion.triggerId() != null && !TRIGGERS.contains(expansion.triggerId())) {
            throw notImplemented(where, "trigger " + expansion.triggerId());
        }
        if (everyoneTarget(expansion.targetId()) && expansion.cls() != ResClass.I) {
            throw notImplemented(where, "target " + expansion.targetId() + " outside a spell");
        }
        if (!TARGETS.contains(expansion.targetId())) {
            throw notImplemented(where, "target " + expansion.targetId());
        }
        if (!DURATIONS.contains(expansion.duration())) {
            throw notImplemented(where, "duration " + expansion.duration());
        }
        if (!triggerParamsOk(expansion.triggerId(), expansion.triggerParams())) {
            throw notImplemented(where, "trigger " + expansion.triggerId() + " with params " + expansion.triggerParams());
        }
        if (!targetParamsOk(expansion.targetId(), expansion.targetParams())) {
            throw notImplemented(where, "target " + expansion.targetId() + " with params " + expansion.targetParams().keySet());
        }
        for (quorma.gameengine.data.ConditionDef c : expansion.conditions()) {
            checkCondition(c, where);
        }
        Optional<String> mode = expansion.stringParam("mode");
        ParamRule rule = modes.get(mode.orElse(""));
        if (rule == null) {
            throw notImplemented(where, "effect " + effect + " with mode " + mode.orElse("(none)"));
        }
        Shape shape = new Shape(expansion.triggerId(), effect, expansion.cls(), expansion.duration());
        if (!SHAPES.contains(shape)) {
            throw new AbilityException(where + ": effect " + effect + " with trigger " + shape.triggerId() + ", class "
                    + shape.cls() + " and duration " + shape.duration() + " is not implemented by the engine yet"
                    + " (supported: " + supported(effect) + ")");
        }
        checkParams(expansion, where, rule);
    }

    private static void checkCondition(quorma.gameengine.data.ConditionDef c, String where) {
        if (!CONDITIONS.contains(c.id())) {
            throw notImplemented(where, "condition " + c.id());
        }
        if (c.id().equals("C24") && !(c.params().get("key") instanceof Value.StrV k && !k.v().isEmpty())) {
            throw new AbilityException(where + ": condition C24 needs a text param 'key'");
        }
        boolean needsSuit = c.id().equals("C4") || c.id().equals("C11") || c.id().equals("C25");
        if (needsSuit && !(c.params().get("suit") instanceof Value.StrV s && SUITS.contains(s.v()))) {
            throw new AbilityException(where + ": condition " + c.id() + " needs a param 'suit' (one of " + new java.util.TreeSet<>(SUITS) + ")");
        }
        if (c.id().equals("C4") && !(c.params().get("level") instanceof Value.IntV n && n.v() >= 1 && n.v() <= 5)) {
            throw new AbilityException(where + ": condition C4 needs an integer param 'level' between 1 and 5");
        }
    }

    private static void checkParams(BoundExpansion expansion, String where, ParamRule rule) {
        String what = where + ": effect " + expansion.effectId()
                + expansion.stringParam("mode").map(m -> " mode " + m).orElse("");
        for (String name : expansion.effectParams().keySet()) {
            if (!name.equals("mode") && !rule.takes(name)) {
                throw new AbilityException(what + " does not take param '" + name + "'");
            }
        }
        for (Map.Entry<String, Set<String>> o : rule.optional().entrySet()) {
            Value given = expansion.effectParams().get(o.getKey());
            if (given != null && !(given instanceof Value.StrV s && o.getValue().contains(s.v()))) {
                throw new AbilityException(what + ": param '" + o.getKey() + "' must be one of " + new java.util.TreeSet<>(o.getValue()));
            }
        }
        for (Map.Entry<String, Set<String>> e : rule.strings().entrySet()) {
            if (!(expansion.effectParams().get(e.getKey()) instanceof Value.StrV s)) {
                throw new AbilityException(what + " needs a text param '" + e.getKey() + "'");
            }
            if (!e.getValue().isEmpty() && !e.getValue().contains(s.v())) {
                throw new AbilityException(what + ": param '" + e.getKey() + "' must be one of " + new java.util.TreeSet<>(e.getValue()) + ", got " + s.v());
            }
        }
        for (Map.Entry<String, int[]> e : rule.ints().entrySet()) {
            String name = e.getKey();
            Value value = expansion.effectParams().get(name);
            if (value == null) {
                throw new AbilityException(what + " needs param '" + name + "'");
            }
            if (!(value instanceof Value.IntV n)) {
                throw new AbilityException(what + ": param '" + name + "' must be an integer");
            }
            int[] range = e.getValue();
            if (n.v() < range[0] || n.v() > range[1]) {
                throw new AbilityException(what + ": param '" + name + "' must be "
                        + (range[1] == MAX ? range[0] + " or more" : "between " + range[0] + " and " + range[1])
                        + ", got " + n.v());
            }
        }
    }

    private static String supported(String effectId) {
        List<String> out = SHAPES.stream()
                .filter(s -> s.effectId().equals(effectId))
                .map(s -> (s.triggerId() == null ? "no trigger" : s.triggerId()) + "/" + s.cls() + "/" + s.duration())
                .sorted()
                .toList();
        return String.join(", ", out);
    }

    private static AbilityException notImplemented(String where, String what) {
        return new AbilityException(where + ": " + what + " is not implemented by the engine yet");
    }
}
