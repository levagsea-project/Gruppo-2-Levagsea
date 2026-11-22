package quorma.gameengine.effects;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** C9: the player's hand is one of the types in {@code hand_type} (one name or a list). */
public final class HandTypeCondition implements Condition {

    /** Hand type names of C9, in Italian as in the vocabulary, with the engine's {@link HandType} names. */
    public static final Map<String, String> HAND_TYPES = Map.ofEntries(
            Map.entry("Five of a kind", "FIVE_OF_A_KIND"), Map.entry("Scala reale", "ROYAL_FLUSH"),
            Map.entry("Scala colore", "STRAIGHT_FLUSH"), Map.entry("Poker", "FOUR_OF_A_KIND"),
            Map.entry("Full", "FULL_HOUSE"), Map.entry("Colore", "FLUSH"), Map.entry("Scala", "STRAIGHT"),
            Map.entry("Tris", "THREE_OF_A_KIND"), Map.entry("Quattro dello stesso seme", "FOUR_OF_A_SUIT"),
            Map.entry("Doppia coppia", "TWO_PAIR"), Map.entry("Coppia", "PAIR"), Map.entry("Carta alta", "HIGH_CARD"));

    @Override
    public String id() {
        return "C9";
    }

    @Override
    public Optional<String> validate(ConditionDef def) {
        Map<String, Value> p = def.params();
        List<Value> types = p.get("hand_type") instanceof Value.ListV l ? l.items()
                : p.get("hand_type") != null ? List.of(p.get("hand_type")) : List.of();
        if (types.isEmpty() || !types.stream().allMatch(v -> v instanceof Value.StrV s && HAND_TYPES.containsKey(s.v()))) {
            return Optional.of("condition C9 needs a param 'hand_type' (one of " + new TreeSet<>(HAND_TYPES.keySet())
                    + ", or a list of them)");
        }
        return Optional.empty();
    }

    @Override
    public boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source) {
        return ctx.handType(side).map(type -> handTypes(def).contains(type)).orElse(false);
    }

    /** The hand types {@code def} accepts, already checked by {@link #validate}. */
    public static Set<HandType> handTypes(ConditionDef def) {
        List<Value> raw = def.params().get("hand_type") instanceof Value.ListV l ? l.items()
                : List.of(def.params().get("hand_type"));
        EnumSet<HandType> out = EnumSet.noneOf(HandType.class);
        for (Value v : raw) {
            out.add(HandType.valueOf(HAND_TYPES.get(((Value.StrV) v).v())));
        }
        return out;
    }
}
