package quorma.gameengine.effects;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;

/** The {@code suit} param shared by the conditions that count or look for a suit (C4, C11, C25). */
final class ConditionParams {

    /** The suit names the data uses. */
    static final Set<String> SUITS = Set.of("Vincolo", "Frattura", "Spira", "Vestigio");

    private ConditionParams() {}

    /** The {@code suit} param, already checked by {@link #suitProblem}. */
    static Suit suit(ConditionDef c) {
        return Suit.valueOf(((Value.StrV) c.params().get("suit")).v().toUpperCase(Locale.ROOT));
    }

    /** A message if {@code suit} is missing or not one of the four suits. */
    static Optional<String> suitProblem(ConditionDef c) {
        if (!(c.params().get("suit") instanceof Value.StrV s && SUITS.contains(s.v()))) {
            return Optional.of("condition " + c.id() + " needs a param 'suit' (one of " + new TreeSet<>(SUITS) + ")");
        }
        return Optional.empty();
    }
}
