package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;

/**
 * The params of one effect mode, as {@link EffectRegistry} checks them when a card is bound. Each {@link Effect}
 * says its own rules ({@link Effect#modes()}); the registry applies them with the same messages for every effect.
 *
 * @param ints            required integer params and their allowed range ({@code [low, high]})
 * @param optionalInts    integer params that may be left out
 * @param strings         required text params; an empty set means any text
 * @param optionalStrings text params that may be left out; an empty set means any text
 * @param optionalBools   true/false params that may be left out
 * @param prefixes        params that may be written as a map under these names (flattened as {@code name.key})
 * @param defaults        integer values added when the card leaves the param out
 */
public record ParamRule(
        Map<String, int[]> ints,
        Map<String, int[]> optionalInts,
        Map<String, Set<String>> strings,
        Map<String, Set<String>> optionalStrings,
        Set<String> optionalBools,
        Set<String> prefixes,
        Map<String, Integer> defaults) {

    /** Any integer. */
    public static final int[] ANY = range(Integer.MIN_VALUE, Integer.MAX_VALUE);

    /** 0 or more. */
    public static final int[] NON_NEGATIVE = range(0, Integer.MAX_VALUE);

    /** An integer range for {@link #ints} and {@link #optionalInts}, both ends included. */
    public static int[] range(int low, int high) {
        return new int[] {low, high};
    }

    /** No params at all. */
    public static ParamRule none() {
        return new ParamRule(Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of(), Map.of());
    }

    public static ParamRule ints(Map<String, int[]> ints) {
        return new ParamRule(ints, Map.of(), Map.of(), Map.of(), Set.of(), Set.of(), Map.of());
    }

    public ParamRule optInts(Map<String, int[]> more) {
        return new ParamRule(ints, more, strings, optionalStrings, optionalBools, prefixes, defaults);
    }

    public ParamRule strings(Map<String, Set<String>> more) {
        return new ParamRule(ints, optionalInts, more, optionalStrings, optionalBools, prefixes, defaults);
    }

    public ParamRule optStrings(Map<String, Set<String>> more) {
        return new ParamRule(ints, optionalInts, strings, more, optionalBools, prefixes, defaults);
    }

    public ParamRule bools(Set<String> more) {
        return new ParamRule(ints, optionalInts, strings, optionalStrings, more, prefixes, defaults);
    }

    public ParamRule prefixes(Set<String> more) {
        return new ParamRule(ints, optionalInts, strings, optionalStrings, optionalBools, more, defaults);
    }

    public ParamRule defaults(Map<String, Integer> more) {
        return new ParamRule(ints, optionalInts, strings, optionalStrings, optionalBools, prefixes, more);
    }

    boolean takes(String name) {
        if (ints.containsKey(name) || optionalInts.containsKey(name) || strings.containsKey(name)
                || optionalStrings.containsKey(name) || optionalBools.contains(name)) {
            return true;
        }
        int dot = name.indexOf('.');
        return dot > 0 && prefixes.contains(name.substring(0, dot));
    }
}
