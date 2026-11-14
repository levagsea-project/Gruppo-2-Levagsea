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
            new Shape("T0", "E56", ResClass.C, "D_Permanente"),  // Perforante: ignores part of the target's reductions
            new Shape("T19", "E1", ResClass.T, "D_Permanente"),  // Insaziabile: + share of base ATK on a kill
            new Shape("T34", "E21", ResClass.R, "D_Istantanea")); // Traboccante: spill multiplier factor

    /** The params of one effect mode: integer params with their range, string params with their one value. */
    private record ParamRule(Map<String, int[]> ints, Map<String, Integer> defaults) {}

    private static final int MAX = Integer.MAX_VALUE;

    /** Per effect, per {@code mode} ({@code ""}: the effect has no mode). */
    private static final Map<String, Map<String, ParamRule>> PARAMS = Map.of(
            "E1", Map.of("pct_base_atk", new ParamRule(Map.of("permille", new int[] {0, MAX}), Map.of())),
            "E21", Map.of("", new ParamRule(Map.of("factor", new int[] {0, MAX}), Map.of())),
            "E28", Map.of(
                    "reduction_pct", new ParamRule(Map.of("pct", new int[] {0, 100}), Map.of()),
                    "flat", new ParamRule(
                            Map.of("amount", new int[] {0, MAX}, "min_damage", new int[] {0, MAX}),
                            Map.of("min_damage", 1))),
            "E56", Map.of("", new ParamRule(Map.of("pct", new int[] {0, 100}), Map.of())));

    private static final Set<String> TRIGGERS = Set.of("T0", "T19", "T34");
    private static final Set<String> TARGETS = Set.of("S1");
    private static final Set<String> DURATIONS = Set.of("D_Permanente", "D_Istantanea");

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
        if (!TRIGGERS.contains(expansion.triggerId())) {
            throw notImplemented(where, "trigger " + expansion.triggerId());
        }
        if (!TARGETS.contains(expansion.targetId())) {
            throw notImplemented(where, "target " + expansion.targetId());
        }
        if (!DURATIONS.contains(expansion.duration())) {
            throw notImplemented(where, "duration " + expansion.duration());
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

    private static void checkParams(BoundExpansion expansion, String where, ParamRule rule) {
        String what = where + ": effect " + expansion.effectId()
                + expansion.stringParam("mode").map(m -> " mode " + m).orElse("");
        for (String name : expansion.effectParams().keySet()) {
            if (!name.equals("mode") && !rule.ints().containsKey(name)) {
                throw new AbilityException(what + " does not take param '" + name + "'");
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
                .map(s -> s.triggerId() + "/" + s.cls() + "/" + s.duration())
                .sorted()
                .toList();
        return String.join(", ", out);
    }

    private static AbilityException notImplemented(String where, String what) {
        return new AbilityException(where + ": " + what + " is not implemented by the engine yet");
    }
}
