package quorma.gameengine.effects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;

/**
 * What the engine can resolve. A card whose abilities use anything else is refused when it is bound
 * ({@link AbilityException}), so an ability is never silently ignored.
 *
 * <p>Every piece of a bound expansion is checked: its trigger (and trigger params) is one the engine fires, its
 * effect is implemented for its resolution class, its effect params are the ones of the effect's mode with integers
 * in range, its target (and target params) is one the engine selects, its duration is one the engine ends and its
 * conditions are ones the engine evaluates. The class must also fit the trigger: T0 is continuous (C), the "about to"
 * triggers are replacements (R), the others are triggered (T), and only a spell or equipment effect has no trigger
 * (I).</p>
 */
public final class EffectRegistry {

    private static final int MAX = Integer.MAX_VALUE;
    private static final int MIN = Integer.MIN_VALUE;
    private static final Set<String> SUITS = Set.of("Vincolo", "Frattura", "Spira", "Vestigio");
    private static final Set<String> RANKS = Set.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K");

    private static int[] range(int lo, int hi) {
        return ParamRule.range(lo, hi);
    }

    private static final int[] ANY = ParamRule.ANY;
    private static final int[] NON_NEGATIVE = ParamRule.NON_NEGATIVE;

    /** Per effect: the classes it is implemented for. */
    private static final Map<String, Set<ResClass>> CLASSES = new LinkedHashMap<>();

    /** Per effect, per {@code mode} ({@code ""}: the effect has no mode). */
    private static final Map<String, Map<String, ParamRule>> PARAMS = new LinkedHashMap<>();

    private static void effect(String id, Set<ResClass> classes, Map<String, ParamRule> modes) {
        CLASSES.put(id, classes);
        PARAMS.put(id, modes);
    }

    private static final Set<ResClass> C = Set.of(ResClass.C);
    private static final Set<ResClass> T = Set.of(ResClass.T);
    private static final Set<ResClass> R = Set.of(ResClass.R);
    private static final Set<ResClass> I = Set.of(ResClass.I);
    private static final Set<ResClass> TI = Set.of(ResClass.T, ResClass.I);
    private static final Set<ResClass> CT = Set.of(ResClass.C, ResClass.T);
    private static final Set<ResClass> CTI = Set.of(ResClass.C, ResClass.T, ResClass.I);

    static {
        for (Effect effect : Effects.all()) {
            effect(effect.id(), effect.classes(), effect.modes());
        }
        effect("E8", TI, Map.of("", ParamRule.ints(Map.of("amount", NON_NEGATIVE))));
        effect("E9", TI, Map.of("", ParamRule.ints(Map.of("amount", NON_NEGATIVE))));
        effect("E11", R, Map.of("", ParamRule.ints(Map.of("count", range(1, MAX)))
                .optInts(Map.of("set_def", range(1, MAX))).bools(Set.of("spill_on_prevent"))
                .defaults(Map.of("set_def", 1))));
        effect("E12", R, Map.of("", ParamRule.none()
                .optInts(Map.of("lockout.reactivate_below", range(1, 5)))
                .optStrings(Map.of("then", Set.of("none", "destroy_at_next_refill"), "lockout.key", Set.of(),
                        "lockout.suit", SUITS))));
        effect("E17", T, Map.of("", ParamRule.none()));
        effect("E19", C, Map.of("", ParamRule.ints(Map.of("factor", NON_NEGATIVE))));
        effect("E21", R, Map.of("", ParamRule.ints(Map.of("factor", NON_NEGATIVE))));
        effect("E22", R, Map.of("", ParamRule.ints(Map.of("permille", range(0, 1000)))));
        effect("E23", R, Map.of("", ParamRule.none()));
        effect("E24", T, Map.of("", ParamRule.none().strings(Map.of("new_target_id", Set.of("S1", "S7")))));
        effect("E25", R, Map.of("", ParamRule.none()));
        effect("E26", C, Map.of("", ParamRule.none()));
        effect("E29", R, Map.of("", ParamRule.none().optInts(Map.of("amount", NON_NEGATIVE)).bools(Set.of("once"))));
        effect("E30", C, Map.of("", ParamRule.none().strings(Map.of("rank", RANKS))));
        Set<String> ranksOrAlly = new java.util.HashSet<>(RANKS);
        ranksOrAlly.add("highest_ally");
        effect("E32", C, Map.of("", ParamRule.none().strings(Map.of("rank", Set.copyOf(ranksOrAlly)))));
        effect("E34", C, Map.of("", ParamRule.none().strings(Map.of("group",
                Set.of("Vincolo", "Frattura", "Spira", "Vestigio", "stesso_giocatore")))));
        effect("E35", I, Map.of("", ParamRule.none()));
        effect("E36", I, Map.of("", ParamRule.none().optInts(Map.of("lane", range(1, 5)))
                .optStrings(Map.of("source", Set.of("deck", "discard")))));
        effect("E47", R, Map.of("", ParamRule.none().optInts(Map.of("lane", range(1, 5)))
                .optStrings(Map.of("player", Set.of("controllore", "avversario")))));
        effect("E49", I, Map.of("", ParamRule.none().strings(Map.of("position", Set.of("top", "bottom")))));
        effect("E51", R, Map.of("", ParamRule.ints(Map.of("permille", NON_NEGATIVE))));
        effect("E52", C, Map.of("", ParamRule.none()));
        effect("E54", C, Map.of("", ParamRule.none().optInts(Map.of("step", range(1, 12)))));
        effect("E55", C, Map.of("", ParamRule.none()));
        effect("E56", C, Map.of("", ParamRule.ints(Map.of("pct", range(0, 100)))));
        effect("E57", I, Map.of("", ParamRule.ints(Map.of("count", NON_NEGATIVE))));
        effect("E60", C, Map.of("", ParamRule.none()));
        effect("E61", C, Map.of("", ParamRule.none()));
        effect("E63", R, Map.of("", ParamRule.none()));
        effect("E64", T, Map.of("", ParamRule.none().strings(Map.of("effect_id", Set.of("E1", "E2", "E6"),
                "duration", Set.of())).prefixes(Set.of("effect_params"))));
    }

    /** Per trigger: the class it allows. */
    private static final Map<String, ResClass> TRIGGERS = new LinkedHashMap<>();

    static {
        TRIGGERS.put("T0", ResClass.C);
        for (String t : List.of("T2", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12", "T13", "T15", "T18", "T19",
                "T26", "T29", "T35", "T36", "T37", "T38")) {
            TRIGGERS.put(t, ResClass.T);
        }
        for (String t : List.of("T4", "T20", "T21", "T22", "T23", "T25", "T34", "T39")) {
            TRIGGERS.put(t, ResClass.R);
        }
    }

    private static final Set<String> PHASES = Set.of("Valutazione", "Dichiarazione", "Inizio del combattimento",
            "Ciclo 1", "Ciclo 2", "Fine del combattimento", "Rifornimento", "Fine del turno");

    private static final Set<String> DURATIONS = Set.of("D_Permanente", "D_Istantanea", "D_FincheSorgenteInGioco",
            "D_FinoAUccisione", "D_FinoAlProssimoRifornimento", "D_FinoAllaFineDelCombattimento", "D_FinoAllaFineDelTurno");

    /** The filters of {@code target_params.filters}. */
    public static final Set<String> TARGET_FILTERS = Set.of("attacker", "non_attacker", "own", "ally", "enemy",
            "damaged", "not_damaged", "damaged_this_turn", "suit:Vincolo", "suit:Frattura", "suit:Spira", "suit:Vestigio");

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
    public static boolean isImplemented(ExpansionDef x) {
        return problem(x.triggerId(), x.triggerParams(), x.effectId(), x.effectParams(), x.targetId(),
                x.targetParams(), x.duration(), x.cls(), x.conditions(), false).isEmpty();
    }

    /**
     * Checks that the engine implements every piece of {@code expansion}.
     *
     * @param where card and ability, for the error message
     * @throws AbilityException naming the first problem
     */
    public static void check(BoundExpansion e, String where) {
        problem(e.triggerId(), e.triggerParams(), e.effectId(), e.effectParams(), e.targetId(), e.targetParams(),
                e.duration(), e.cls(), e.conditions(), true).ifPresent(p -> {
                    throw new AbilityException(where + ": " + p);
                });
    }

    private static Optional<String> problem(String trigger, Map<String, Value> triggerParams, String effect,
            Map<String, Value> effectParams, String target, Map<String, Value> targetParams, String duration,
            ResClass cls, List<ConditionDef> conditions, boolean strict) {
        Map<String, ParamRule> modes = PARAMS.get(effect);
        if (modes == null) {
            return Optional.of(notImplemented("effect " + effect));
        }
        if (trigger != null && !TRIGGERS.containsKey(trigger)) {
            return Optional.of(notImplemented("trigger " + trigger));
        }
        if (Targets.find(target).isEmpty()) {
            return Optional.of(notImplemented("target " + target));
        }
        if (!DURATIONS.contains(duration)) {
            return Optional.of(notImplemented("duration " + duration));
        }
        Optional<String> tp = triggerParamsProblem(trigger, triggerParams);
        if (tp.isPresent()) {
            return tp;
        }
        Optional<String> gp = targetParamsProblem(target, targetParams);
        if (gp.isPresent()) {
            return gp;
        }
        for (ConditionDef c : conditions) {
            Optional<String> cp = conditionProblem(c);
            if (cp.isPresent()) {
                return cp;
            }
        }
        ResClass expected = trigger == null ? ResClass.I : TRIGGERS.get(trigger);
        if (cls != expected) {
            return Optional.of("class " + cls + " does not fit trigger " + (trigger == null ? "(none)" : trigger)
                    + " (expected class " + expected + ")");
        }
        if (!CLASSES.get(effect).contains(cls)) {
            return Optional.of("effect " + effect + " with class " + cls + " is not implemented by the engine yet"
                    + " (supported: " + new TreeSet<>(CLASSES.get(effect).stream().map(Enum::name).toList()) + ")");
        }
        String mode = effectParams.get("mode") instanceof Value.StrV s ? s.v() : "";
        ParamRule rule = modes.get(mode);
        if (rule == null) {
            return Optional.of(notImplemented("effect " + effect + " with mode " + (mode.isEmpty() ? "(none)" : mode)));
        }
        Optional<String> own = Effects.find(effect).flatMap(x -> x.validate(mode, effectParams));
        if (own.isPresent()) {
            return own;
        }
        return paramsProblem(effect, mode, rule, effectParams, strict);
    }

    private static Optional<String> paramsProblem(String effect, String mode, ParamRule rule, Map<String, Value> params,
            boolean strict) {
        String what = "effect " + effect + (mode.isEmpty() ? "" : " mode " + mode);
        Map<String, Value> flat = new LinkedHashMap<>();
        flatten("", params, flat);
        for (String name : flat.keySet()) {
            if (!name.equals("mode") && !rule.takes(name)) {
                return Optional.of(what + " does not take param '" + name + "'");
            }
        }
        for (Map.Entry<String, Set<String>> e : rule.strings().entrySet()) {
            if (!(flat.get(e.getKey()) instanceof Value.StrV s)) {
                return Optional.of(what + " needs a text param '" + e.getKey() + "'");
            }
            if (!e.getValue().isEmpty() && !e.getValue().contains(s.v())) {
                return Optional.of(what + ": param '" + e.getKey() + "' must be one of " + new TreeSet<>(e.getValue())
                        + ", got " + s.v());
            }
        }
        for (Map.Entry<String, int[]> e : rule.ints().entrySet()) {
            Value value = flat.get(e.getKey());
            if (value == null) {
                return Optional.of(what + " needs param '" + e.getKey() + "'");
            }
            Optional<String> p = intProblem(what, e.getKey(), value, e.getValue(), strict);
            if (p.isPresent()) {
                return p;
            }
        }
        for (Map.Entry<String, Value> e : flat.entrySet()) {
            String name = e.getKey();
            Value value = e.getValue();
            boolean asString = rule.optionalStrings().containsKey(name);
            boolean asInt = rule.optionalInts().containsKey(name);
            if (asString && value instanceof Value.StrV s) {
                Set<String> allowed = rule.optionalStrings().get(name);
                if (!allowed.isEmpty() && !allowed.contains(s.v())) {
                    return Optional.of(what + ": param '" + name + "' must be one of " + new TreeSet<>(allowed));
                }
            } else if (asInt && !(value instanceof Value.StrV)) {
                Optional<String> p = intProblem(what, name, value, rule.optionalInts().get(name), strict);
                if (p.isPresent()) {
                    return p;
                }
            } else if (asString && !asInt && strict) {
                return Optional.of(what + ": param '" + name + "' must be one of " + new TreeSet<>(rule.optionalStrings().get(name)));
            } else if (rule.optionalBools().contains(name) && strict && !(value instanceof Value.BoolV)) {
                return Optional.of(what + ": param '" + name + "' must be true or false");
            }
        }
        return Optional.empty();
    }

    private static Optional<String> intProblem(String what, String name, Value value, int[] range, boolean strict) {
        if (!strict && value instanceof Value.Ref) {
            return Optional.empty();
        }
        if (!(value instanceof Value.IntV n)) {
            return Optional.of(what + ": param '" + name + "' must be an integer");
        }
        if (n.v() < range[0] || n.v() > range[1]) {
            return Optional.of(what + ": param '" + name + "' must be "
                    + (range[1] == MAX ? range[0] + " or more" : "between " + range[0] + " and " + range[1])
                    + ", got " + n.v());
        }
        return Optional.empty();
    }

    private static void flatten(String prefix, Map<String, Value> params, Map<String, Value> out) {
        for (Map.Entry<String, Value> e : params.entrySet()) {
            if (e.getValue() instanceof Value.MapV m) {
                flatten(prefix + e.getKey() + ".", m.fields(), out);
            } else {
                out.put(prefix + e.getKey(), e.getValue());
            }
        }
    }

    private static Optional<String> triggerParamsProblem(String trigger, Map<String, Value> params) {
        if (trigger == null) {
            return params.isEmpty() ? Optional.empty() : Optional.of("an expansion with no trigger has no trigger params");
        }
        String bad = notImplemented("trigger " + trigger + " with params " + params);
        return switch (trigger) {
            case "T2" -> params.size() == 1 && params.get("phase") instanceof Value.StrV p && PHASES.contains(p.v())
                    ? Optional.empty() : Optional.of(bad);
            case "T5" -> params.size() == 2 && params.get("suit") instanceof Value.StrV s && SUITS.contains(s.v())
                    && params.get("level") instanceof Value.IntV n && n.v() >= 3 && n.v() <= 5
                    ? Optional.empty() : Optional.of(bad);
            case "T8" -> onlyStrings(params, Map.of("attacker", Set.of("opponent", "own", "any"),
                    "target_scope", Set.of("any", "self", "self_or_adjacent"))) ? Optional.empty() : Optional.of(bad);
            case "T13" -> onlyStrings(params, Map.of("source", Set.of("combattimento", "effetto", "qualsiasi")))
                    ? Optional.empty() : Optional.of(bad);
            case "T15" -> onlyStrings(params, Map.of("source", Set.of("self", "ally"))) ? Optional.empty() : Optional.of(bad);
            case "T18" -> onlyStrings(params, Map.of("controller", Set.of("own", "opponent", "any"), "spell_id", Set.of()))
                    ? Optional.empty() : Optional.of(bad);
            default -> params.isEmpty() ? Optional.empty() : Optional.of(bad);
        };
    }

    private static boolean onlyStrings(Map<String, Value> params, Map<String, Set<String>> allowed) {
        for (Map.Entry<String, Value> e : params.entrySet()) {
            Set<String> values = allowed.get(e.getKey());
            if (values == null || !(e.getValue() instanceof Value.StrV s) || (!values.isEmpty() && !values.contains(s.v()))) {
                return false;
            }
        }
        return true;
    }

    private static Optional<String> targetParamsProblem(String target, Map<String, Value> params) {
        if (Targets.find(target).orElseThrow().acceptsParams(params)) {
            return Optional.empty();
        }
        return Optional.of(notImplemented("target " + target + " with params " + params.keySet()));
    }

    private static Optional<String> conditionProblem(ConditionDef c) {
        Optional<Condition> condition = Conditions.find(c.id());
        if (condition.isEmpty()) {
            return Optional.of(notImplemented("condition " + c.id()));
        }
        return condition.get().validate(c);
    }

    private static String notImplemented(String what) {
        return what + " is not implemented by the engine yet";
    }
}
